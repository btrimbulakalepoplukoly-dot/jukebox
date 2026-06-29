package net.chaosjukebox.client;

import net.chaosjukebox.ChaosJukeboxMod;
import net.chaosjukebox.audio.StreamAudioPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Живе тільки на клієнті. Один запис на блок, що зараз "грає".
 * Кожен клієнтський тік перераховує гучність кожного активного блока
 * залежно від відстані гравця до нього: 0 блоків = 100% голосність,
 * 30+ блоків = повна тиша. Це і є "динамічність" з ТЗ.
 */
public final class ActiveAudioManager {

    private static final Map<BlockPos, StreamAudioPlayer> ACTIVE = new ConcurrentHashMap<>();

    private ActiveAudioManager() {}

    public static void play(BlockPos pos, String url, long startTimeMillis) {
        stop(pos); // якщо там щось вже грало - зупиняємо перед новим запуском

        double startOffsetSec = Math.max(0, (System.currentTimeMillis() - startTimeMillis) / 1000.0);

        StreamAudioPlayer player = new StreamAudioPlayer();
        ACTIVE.put(pos, player);

        player.start(url, startOffsetSec, errorMessage -> {
            ACTIVE.remove(pos);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("[Chaos Jukebox] Не вдалось відтворити: " + errorMessage), false);
            }
            ChaosJukeboxMod.LOGGER.warn("Audio error at {}: {}", pos, errorMessage);
        });
    }

    public static void stop(BlockPos pos) {
        StreamAudioPlayer existing = ACTIVE.remove(pos);
        if (existing != null) {
            existing.stop();
        }
    }

    public static void stopAll() {
        ACTIVE.values().forEach(StreamAudioPlayer::stop);
        ACTIVE.clear();
    }

    /** Викликати кожен клієнтський тік (зареєстровано в ChaosJukeboxClient). */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        var playerPos = client.player.getPos();

        ACTIVE.entrySet().removeIf(entry -> entry.getValue().isStopped());

        for (Map.Entry<BlockPos, StreamAudioPlayer> entry : ACTIVE.entrySet()) {
            BlockPos pos = entry.getKey();
            double distance = Math.sqrt(pos.toCenterPos().squaredDistanceTo(playerPos));

            float volume;
            if (distance >= ChaosJukeboxMod.HEARING_RANGE) {
                volume = 0f;
            } else {
                // лінійне затухання: 0 блоків -> 1.0, 30 блоків -> 0.0
                volume = (float) (1.0 - (distance / ChaosJukeboxMod.HEARING_RANGE));
            }
            entry.getValue().setVolume(volume);
        }
    }
}

package net.chaosjukebox.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.chaosjukebox.client.gui.JukeboxScreen;
import net.chaosjukebox.network.JukeboxPackets.PlayAudioPayload;
import net.chaosjukebox.network.JukeboxPackets.StopAudioPayload;
import net.minecraft.util.math.BlockPos;

public class ChaosJukeboxClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Сервер сказав "грай це" - запускаємо локально, з власною дистанційною гучністю.
        ClientPlayNetworking.registerGlobalReceiver(PlayAudioPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        ActiveAudioManager.play(payload.pos(), payload.url(), payload.startTimeMillis())));

        // Сервер сказав "стоп" (хтось натиснув Стоп або блок зламали).
        ClientPlayNetworking.registerGlobalReceiver(StopAudioPayload.ID, (payload, context) ->
                context.client().execute(() -> ActiveAudioManager.stop(payload.pos())));

        // Кожен клієнтський тік перераховуємо гучність активних відтворень за дистанцією.
        ClientTickEvents.END_CLIENT_TICK.register(client -> ActiveAudioManager.tick());
    }

    /** Викликається з StreamJukeboxBlock.onUse() на клієнті. */
    public static void openJukeboxScreen(BlockPos pos) {
        MinecraftClient.getInstance().setScreen(new JukeboxScreen(pos));
    }
}

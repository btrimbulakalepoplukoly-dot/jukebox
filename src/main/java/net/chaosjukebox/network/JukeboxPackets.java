package net.chaosjukebox.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Усі кастомні пакети мода зібрані в одному місці.
 * Реєструються один раз у ChaosJukeboxMod.onInitialize().
 */
public final class JukeboxPackets {

    private JukeboxPackets() {}

    // ---- Клієнт -> Сервер: гравець натиснув "Грати" у вікні блока ----
    public record SetUrlPayload(BlockPos pos, String url) implements CustomPayload {
        public static final CustomPayload.Id<SetUrlPayload> ID =
                new CustomPayload.Id<>(Identifier.of("chaosjukebox", "set_url"));
        public static final PacketCodec<RegistryByteBuf, SetUrlPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, SetUrlPayload::pos,
                PacketCodecs.STRING, SetUrlPayload::url,
                SetUrlPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ---- Клієнт -> Сервер: гравець натиснув "Стоп" ----
    public record StopRequestPayload(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<StopRequestPayload> ID =
                new CustomPayload.Id<>(Identifier.of("chaosjukebox", "stop_request"));
        public static final PacketCodec<RegistryByteBuf, StopRequestPayload> CODEC =
                BlockPos.PACKET_CODEC.xmap(StopRequestPayload::new, StopRequestPayload::pos);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ---- Сервер -> Усі клієнти: щось має почати грати ----
    public record PlayAudioPayload(BlockPos pos, String url, long startTimeMillis) implements CustomPayload {
        public static final CustomPayload.Id<PlayAudioPayload> ID =
                new CustomPayload.Id<>(Identifier.of("chaosjukebox", "play_audio"));
        public static final PacketCodec<RegistryByteBuf, PlayAudioPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, PlayAudioPayload::pos,
                PacketCodecs.STRING, PlayAudioPayload::url,
                PacketCodecs.VAR_LONG, PlayAudioPayload::startTimeMillis,
                PlayAudioPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ---- Сервер -> Усі клієнти: зупинити відтворення для блока ----
    public record StopAudioPayload(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<StopAudioPayload> ID =
                new CustomPayload.Id<>(Identifier.of("chaosjukebox", "stop_audio"));
        public static final PacketCodec<RegistryByteBuf, StopAudioPayload> CODEC =
                BlockPos.PACKET_CODEC.xmap(StopAudioPayload::new, StopAudioPayload::pos);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Реєструє типи пакетів у глобальному реєстрі (треба викликати і на сервері, і на клієнті). */
    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(SetUrlPayload.ID, SetUrlPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StopRequestPayload.ID, StopRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayAudioPayload.ID, PlayAudioPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StopAudioPayload.ID, StopAudioPayload.CODEC);
    }
}

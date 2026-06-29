package net.chaosjukebox;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.chaosjukebox.block.StreamJukeboxBlock;
import net.chaosjukebox.block.StreamJukeboxBlockEntity;
import net.chaosjukebox.network.JukeboxPackets;
import net.chaosjukebox.network.JukeboxPackets.PlayAudioPayload;
import net.chaosjukebox.network.JukeboxPackets.SetUrlPayload;
import net.chaosjukebox.network.JukeboxPackets.StopAudioPayload;
import net.chaosjukebox.network.JukeboxPackets.StopRequestPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaosJukeboxMod implements ModInitializer {

    public static final String MOD_ID = "chaosjukebox";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Радіус чутності в блоках - ТЗ просило 30. */
    public static final double HEARING_RANGE = 30.0;

    public static final StreamJukeboxBlock JUKEBOX_BLOCK = new StreamJukeboxBlock(
            AbstractBlock.Settings.create()
                    .strength(2.0f)
                    .sounds(BlockSoundGroup.WOOD)
                    .mapColor(MapColor.BROWN)
                    .nonOpaque()
    );

    public static final Item JUKEBOX_ITEM = new BlockItem(JUKEBOX_BLOCK, new Item.Settings());

    public static BlockEntityType<StreamJukeboxBlockEntity> JUKEBOX_BLOCK_ENTITY;

    @Override
    public void onInitialize() {
        LOGGER.info("[ChaosJukebox] Ініціалізація...");

        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "stream_jukebox"), JUKEBOX_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "stream_jukebox"), JUKEBOX_ITEM);

        JUKEBOX_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(MOD_ID, "stream_jukebox"),
                FabricBlockEntityTypeBuilder.create(StreamJukeboxBlockEntity::new, JUKEBOX_BLOCK).build()
        );

        JukeboxPackets.registerPayloadTypes();
        registerServerReceivers();
    }

    private void registerServerReceivers() {
        // Гравець у GUI натиснув "Грати"
        ServerPlayNetworking.registerGlobalReceiver(SetUrlPayload.ID, (payload, context) -> {
            World world = context.player().getWorld();
            context.server().execute(() -> {
                BlockPos pos = payload.pos();
                if (world.getBlockEntity(pos) instanceof StreamJukeboxBlockEntity be) {
                    long startTime = System.currentTimeMillis();
                    be.setPlaying(payload.url(), startTime);
                    broadcastNear(world, pos, new PlayAudioPayload(pos, payload.url(), startTime));
                }
            });
        });

        // Гравець натиснув "Стоп"
        ServerPlayNetworking.registerGlobalReceiver(StopRequestPayload.ID, (payload, context) -> {
            World world = context.player().getWorld();
            context.server().execute(() -> {
                BlockPos pos = payload.pos();
                if (world.getBlockEntity(pos) instanceof StreamJukeboxBlockEntity be) {
                    be.stop();
                    broadcastNear(world, pos, new StopAudioPayload(pos));
                }
            });
        });
    }

    /** Розсилаємо всім гравцям у тому ж вимірі (клієнт сам відріже звук після 30 блоків). */
    private static void broadcastNear(World world, BlockPos pos, net.minecraft.network.packet.CustomPayload payload) {
        if (!(world instanceof net.minecraft.server.world.ServerWorld serverWorld)) return;
        for (var player : PlayerLookup.world(serverWorld)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /** Викликається ззовні (наприклад при ламанні блока), щоб гарантовано зупинити звук у всіх. */
    public static void stopAudioEverywhere(World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof StreamJukeboxBlockEntity be) {
            be.stop();
        }
        broadcastNear(world, pos, new StopAudioPayload(pos));
    }
}

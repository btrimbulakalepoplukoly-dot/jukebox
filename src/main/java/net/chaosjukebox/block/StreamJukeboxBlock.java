package net.chaosjukebox.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.chaosjukebox.client.ChaosJukeboxClient;

/**
 * Блок "Хаос-бумбокс". По ЛКМ (правий клік) на клієнті відкриває вікно для вставки
 * посилання на YouTube. Саме відтворення звуку керується ChaosJukeboxClient/ActiveAudioManager.
 */
public class StreamJukeboxBlock extends BlockWithEntity implements BlockEntityProvider {

    public static final MapCodec<StreamJukeboxBlock> CODEC = createCodec(StreamJukeboxBlock::new);

    public StreamJukeboxBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StreamJukeboxBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            // Відкриваємо вікно лише локально на клієнті - воно само надішле пакет
            // на сервер, коли гравець натисне "Грати".
            ChaosJukeboxClient.openJukeboxScreen(pos);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // Якщо блок зламали - на сервері зупиняємо трансляцію для всіх.
        if (!world.isClient) {
            net.chaosjukebox.ChaosJukeboxMod.stopAudioEverywhere(world, pos);
        }
        super.onBreak(world, pos, state, player);
    }
}

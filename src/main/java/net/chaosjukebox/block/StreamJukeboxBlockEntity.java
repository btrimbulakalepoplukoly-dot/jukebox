package net.chaosjukebox.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.chaosjukebox.ChaosJukeboxMod;

/**
 * Зберігає, яке відео/посилання зараз "грає" на цьому блоці, і коли воно почалось,
 * щоб гравці, які доєднались пізніше (зайшли в чанк), могли підхопити відтворення
 * приблизно з потрібного місця.
 */
public class StreamJukeboxBlockEntity extends BlockEntity {

    private String currentUrl = "";
    private boolean playing = false;
    private long startTimeMillis = 0L;

    public StreamJukeboxBlockEntity(BlockPos pos, BlockState state) {
        super(ChaosJukeboxMod.JUKEBOX_BLOCK_ENTITY, pos, state);
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public boolean isPlaying() {
        return playing;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setPlaying(String url, long startTimeMillis) {
        this.currentUrl = url;
        this.playing = true;
        this.startTimeMillis = startTimeMillis;
        markDirty();
    }

    public void stop() {
        this.playing = false;
        markDirty();
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        this.currentUrl = nbt.getString("Url", "");
        this.playing = nbt.getBoolean("Playing", false);
        this.startTimeMillis = nbt.getLong("StartTime", 0L);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putString("Url", currentUrl);
        nbt.putBoolean("Playing", playing);
        nbt.putLong("StartTime", startTimeMillis);
    }
}

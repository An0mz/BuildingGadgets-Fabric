package com.direwolf20.buildinggadgets.common.tileentities;

import com.direwolf20.buildinggadgets.common.blocks.EffectBlock.Mode;
import com.direwolf20.buildinggadgets.common.tainted.Tainted;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.TileSupport;
import com.direwolf20.buildinggadgets.common.util.ref.NBTKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Tainted(reason = "Used blockData and a stupid non-centralised callback system")
public class EffectBlockTileEntity extends BlockEntity {

    private BlockData renderedBlock;
    /**
     * A copy of the target block, used for inheriting data for {@link Mode#REPLACE}
     */
    private BlockData sourceBlock;

    private Mode mode = null;
    private int ticks;
    private boolean completed = false;

    public EffectBlockTileEntity(BlockPos pos, BlockState state) {
        super(OurTileEntities.EFFECT_BLOCK_TILE_ENTITY, pos, state);
    }

    public void initializeData(BlockState curState, @Nullable BlockEntity be, BlockData replacementBlock, Mode mode) {
        // Minecraft will reuse a tile entity object at a location where the block got removed, but the modification is still buffered, and the block got restored again
        // If we don't reset this here, the 2nd phase of REPLACE will simply finish immediately because the tile entity object is reused
        this.ticks = 0;
        this.completed = false;
        // Again we don't check if the data has been set or not because there is a chance that this tile object gets reused
        this.sourceBlock = replacementBlock;

        this.mode = mode;

        if (mode == Mode.REPLACE) {
            this.setRenderedBlock(TileSupport.createBlockData(curState, be));
        } else {
            this.setRenderedBlock(replacementBlock);
        }
    }

    public static void tick(Level level, BlockPos blockPos, BlockState state, EffectBlockTileEntity blockEntity) {
        if (++blockEntity.ticks >= blockEntity.getLifespan()) {
            blockEntity.complete();
        }
    }

    private void complete() {
        if (level == null || level.isClientSide || mode == null || getRenderedBlock() == null) {
            return;
        }
        if (completed) return;
        completed = true;
        mode.onBuilderRemoved(this);
    }

    public void forceComplete() {
        complete();
    }

    @Override
    public void setRemoved() {
        forceComplete();
        super.setRemoved();
    }

    public BlockData getRenderedBlock() {
        return renderedBlock;
    }

    public void setRenderedBlock(BlockData renderedBlock) {
        this.renderedBlock = renderedBlock;
    }

    public BlockData getSourceBlock() {
        return sourceBlock;
    }

    public Mode getReplacementMode() {
        return mode;
    }

    public int getTicksExisted() {
        return ticks;
    }

    public int getLifespan() {
        return 20;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag compound, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(compound, registries);
        if (mode != null && getRenderedBlock() != null && sourceBlock != null) {
            compound.putInt(NBTKeys.GADGET_TICKS, ticks);
            compound.putInt(NBTKeys.GADGET_MODE, mode.ordinal());
            compound.put(NBTKeys.GADGET_REPLACEMENT_BLOCK, getRenderedBlock().serialize(true));
            compound.put(NBTKeys.GADGET_SOURCE_BLOCK, sourceBlock.serialize(true));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(nbt, registries);

        if (nbt.contains(NBTKeys.GADGET_TICKS) &&
                nbt.contains(NBTKeys.GADGET_MODE) &&
                nbt.contains(NBTKeys.GADGET_SOURCE_BLOCK) &&
                nbt.contains(NBTKeys.GADGET_REPLACEMENT_BLOCK)) {

            ticks = nbt.getIntOr(NBTKeys.GADGET_TICKS, 0);
            mode = Mode.values()[nbt.getIntOr(NBTKeys.GADGET_MODE, 0)];
            setRenderedBlock(BlockData.tryDeserialize(nbt.getCompoundOrEmpty(NBTKeys.GADGET_REPLACEMENT_BLOCK), true));
            sourceBlock = BlockData.tryDeserialize(nbt.getCompoundOrEmpty(NBTKeys.GADGET_SOURCE_BLOCK), true);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel) {
            ((ServerChunkCache) level.getChunkSource()).blockChanged(getBlockPos());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
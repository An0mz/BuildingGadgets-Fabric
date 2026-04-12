package com.direwolf20.buildinggadgets.common.tileentities;

import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.containers.TemplateManagerContainer;
import com.direwolf20.buildinggadgets.common.inventory.ImplContainer;
import com.direwolf20.buildinggadgets.common.util.TemplateKeyHelper;
import com.direwolf20.buildinggadgets.common.util.ref.Reference.ItemReference;
import com.google.common.base.Preconditions;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TemplateManagerTileEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos>, ImplContainer {
    public static final TagKey<Item> TEMPLATE_CONVERTIBLES = TagKey.create(Registries.ITEM, ItemReference.TAG_TEMPLATE_CONVERTIBLE);

    public static final int SIZE = 2;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public TemplateManagerTileEntity(BlockPos pos, BlockState state) {
        super(OurTileEntities.TEMPLATE_MANAGER_TILE_ENTITY, pos, state);
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return Component.literal("Template Manager GUI");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
        Preconditions.checkArgument(getLevel() != null);
        return new TemplateManagerContainer(windowId, playerInventory, this);
    }

    public boolean isTemplateStack(ItemStack stack) {
        return TemplateKeyHelper.hasTemplateKey(stack);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, inventory);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, inventory);
    }

    public boolean canInteractWith(Player playerIn) {
        // If we are too far away (>4 blocks) from this tile entity you cannot use it
        return !isRemoved() && playerIn.distanceToSqr(Vec3.atLowerCornerOf(worldPosition).add(0.5D, 0.5D, 0.5D)) <= 64D;
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
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        saveAdditional(output);
        return output.buildResult();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return getBlockPos();
    }
}
package com.direwolf20.buildinggadgets.common.items;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.commands.ForceUnloadedCommand;
import com.direwolf20.buildinggadgets.common.compat.FLANCompat;
import com.direwolf20.buildinggadgets.common.compat.FTBChunksCompat;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.component.BGDataComponents;
import com.direwolf20.buildinggadgets.common.items.modes.*;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.concurrent.UndoScheduler;
import com.direwolf20.buildinggadgets.common.tainted.inventory.IItemIndex;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.tainted.save.Undo;
import com.direwolf20.buildinggadgets.common.util.helpers.VectorHelper;
import com.direwolf20.buildinggadgets.common.util.lang.MessageTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.Styles;
import com.direwolf20.buildinggadgets.common.util.lang.TooltipTranslation;
import com.google.common.collect.ImmutableSortedSet;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.direwolf20.buildinggadgets.common.util.GadgetUtils.withSuffix;

public abstract class AbstractGadget extends Item implements SimpleEnergyItem {

    private final TagKey<Block> whiteList;
    private final TagKey<Block> blackList;

    public AbstractGadget(Properties builder, Identifier whiteListTag, Identifier blackListTag) {
        super(builder);

        this.whiteList = TagKey.create(BuiltInRegistries.BLOCK.key(), whiteListTag);
        this.blackList = TagKey.create(BuiltInRegistries.BLOCK.key(), blackListTag);
    }

    public abstract long getEnergyCapacity();

    public abstract long getEnergyCost(ItemStack tool);

    @Override
    public int getBarColor(ItemStack itemStack) {
        float f = getBarWidth(itemStack) / 13.0F;
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean isBarVisible(ItemStack itemStack) {
        if(getEnergyCapacity() <= 0)
            return false;
        return (getEnergyCapacity() != getStoredEnergy(itemStack));
    }

    @Override
    public int getBarWidth(ItemStack itemStack) {
        return (int) (13.0F - (getEnergyCapacity() - (float) getStoredEnergy(itemStack)) * 13.0F / (float) getEnergyCapacity());
    }

    public long getEnergyMaxInput() {
        return 10000;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    // Keep no-param version for internal use
    public long getEnergyMaxOutput() {
        return 0;
    }

    public TagKey<Block> getWhiteList() {
        return whiteList;
    }

    public TagKey<Block> getBlackList() {
        return blackList;
    }

    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
        ItemStack charged = new ItemStack(this);
        charged.set(BGDataComponents.ENERGY, this.getEnergyCapacity());
        items.add(charged);
    }

    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() instanceof AbstractGadget && repair.getItem() == Items.DIAMOND;
    }

    public boolean isAllowedBlock(Block block) {
        if(block.defaultBlockState().is(ConventionalBlockTags.RELOCATION_NOT_SUPPORTED))
            return false;
        if(!BuiltInRegistries.BLOCK.getTagOrEmpty(getWhiteList()).iterator().hasNext()) {
            return !block.defaultBlockState().is(getBlackList());
        }
        return block.defaultBlockState().is(getWhiteList());
    }

    public static ItemStack getGadget(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof AbstractGadget)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof AbstractGadget)) {
                return ItemStack.EMPTY;
            }
        }
        return heldItem;
    }

    public boolean canUse(ItemStack tool, Player player) {
        if (player.isCreative() || this.getEnergyCapacity() == 0) {
            return true;
        }

        return getEnergyCost(tool) <= getStoredEnergy(tool);
    }

    public boolean useEnergy(ItemStack tool, ServerPlayer player) {
        if (player.isCreative() || this.getEnergyCapacity() == 0) {
            return true;
        }

        return ((AbstractGadget) tool.getItem()).tryUseEnergy(tool, getEnergyCost(tool));
    }

    protected void addEnergyInformation(java.util.function.Consumer<Component> tooltipAdder, ItemStack stack) {
        if (this.getEnergyCapacity() == 0) {
            return;
        }

        if (stack.getItem() instanceof SimpleEnergyItem) {
            tooltipAdder.accept(TooltipTranslation.GADGET_ENERGY
                    .componentTranslation(withSuffix((int) getStoredEnergy(stack)), withSuffix((int) getEnergyCapacity()))
                    .setStyle(Styles.GRAY));
        }
    }

    public final void onRotate(ItemStack stack, Player player) {
        if (performRotate(stack, player)) {
            player.sendOverlayMessage(MessageTranslation.ROTATED.componentTranslation().setStyle(Styles.AQUA));
        }
    }

    protected boolean performRotate(ItemStack stack, Player player) {
        return false;
    }

    public final void onMirror(ItemStack stack, Player player) {
        if (performMirror(stack, player)) {
            player.sendOverlayMessage(MessageTranslation.MIRRORED.componentTranslation().setStyle(Styles.AQUA));
        }
    }

    protected boolean performMirror(ItemStack stack, Player player) {
        return false;
    }

    public final void onAnchor(ItemStack stack, Player player) {
        if (getAnchor(stack) == null) {
            BlockHitResult lookingAt = VectorHelper.getLookingAt(player, stack);
            if ((player.level().isEmptyBlock(lookingAt.getBlockPos()))) {
                return;
            }
            onAnchorSet(stack, player, lookingAt);
            player.sendOverlayMessage(MessageTranslation.ANCHOR_SET.componentTranslation().setStyle(Styles.AQUA));
        } else {
            onAnchorRemoved(stack, player);
            player.sendOverlayMessage(MessageTranslation.ANCHOR_REMOVED.componentTranslation().setStyle(Styles.AQUA));
        }
    }

    protected void onAnchorSet(ItemStack stack, Player player, BlockHitResult lookingAt) {
        stack.set(BGDataComponents.ANCHOR, lookingAt.getBlockPos());
    }

    protected void onAnchorRemoved(ItemStack stack, Player player) {
        stack.remove(BGDataComponents.ANCHOR);
    }

    @Nullable
    public BlockPos getAnchor(ItemStack stack) {
        return stack.get(BGDataComponents.ANCHOR);
    }

    public static boolean getFuzzy(ItemStack stack) {
        return stack.getOrDefault(BGDataComponents.FUZZY, false);
    }

    public static void toggleFuzzy(Player player, ItemStack stack) {
        boolean current = getFuzzy(stack);
        stack.set(BGDataComponents.FUZZY, !current);
        player.sendOverlayMessage(MessageTranslation.FUZZY_MODE.componentTranslation(!current).setStyle(Styles.AQUA));
    }

    public static boolean getConnectedArea(ItemStack stack) {
        return !stack.getOrDefault(BGDataComponents.UNCONNECTED_AREA, false);
    }

    public static void toggleConnectedArea(Player player, ItemStack stack) {
        boolean current = getConnectedArea(stack);
        stack.set(BGDataComponents.UNCONNECTED_AREA, current);
        player.sendOverlayMessage((stack.getItem() instanceof GadgetDestruction ? MessageTranslation.CONNECTED_AREA : MessageTranslation.CONNECTED_SURFACE)
                .componentTranslation(!current).setStyle(Styles.AQUA));
    }

    public static boolean shouldRayTraceFluid(ItemStack stack) {
        return stack.getOrDefault(BGDataComponents.RAYTRACE_FLUID, false);
    }

    public static void toggleRayTraceFluid(ServerPlayer player, ItemStack stack) {
        boolean current = shouldRayTraceFluid(stack);
        stack.set(BGDataComponents.RAYTRACE_FLUID, !current);
        player.sendOverlayMessage(MessageTranslation.RAYTRACE_FLUID.componentTranslation(!current).setStyle(Styles.AQUA));
    }

    public static void addInformationRayTraceFluid(java.util.function.Consumer<Component> tooltipAdder, ItemStack stack) {
        tooltipAdder.accept(TooltipTranslation.GADGET_RAYTRACE_FLUID
                .componentTranslation(String.valueOf(shouldRayTraceFluid(stack)))
                .setStyle(Styles.BLUE));
    }

    //this should only be called Server-Side!!!
    public UUID getUUID(ItemStack stack) {
        UUID existing = stack.get(BGDataComponents.GADGET_UUID);
        if (existing != null) {
            return existing;
        } else {
            UUID newId = UUID.randomUUID();
            stack.set(BGDataComponents.GADGET_UUID, newId);
            return newId;
        }
    }

    // Todo: tweak and fix.
    public static int getRangeInBlocks(int range, AbstractMode mode) {
        if (mode instanceof StairMode ||
                mode instanceof VerticalColumnMode ||
                mode instanceof HorizontalColumnMode) {
            return range;
        }

        if (mode instanceof GridMode) {
            return range < 7 ? 9 : range < 13 ? 11 * 11 : 19 * 19;
        }

        return range == 1 ? 1 : (range + 1) * (range + 1);
    }

    protected void pushUndo(ItemStack stack, Undo undo, Level world) {
        // Don't save if there is nothing to undo...
        if (undo.getUndoData().isEmpty()) {
            return;
        }

        BGComponent.UNDO_COMPONENT.get(world).insertUndo(getUUID(stack), undo);
    }

    public void undo(Level world, Player player, ItemStack stack) {
        Optional<Undo> undoOptional = BGComponent.UNDO_COMPONENT.get(world).getUndo(getUUID(stack));

        if (undoOptional.isPresent()) {
            Undo undo = undoOptional.get();
            IItemIndex index = InventoryHelper.index(stack, player);
            if (!ForceUnloadedCommand.mayForceUnloadedChunks(player)) {
                ImmutableSortedSet<ChunkPos> unloadedChunks = undo.getBoundingBox().getUnloadedChunks(world);
                if (!unloadedChunks.isEmpty()) {
                    pushUndo(stack, undo, world);
                    player.sendOverlayMessage(MessageTranslation.UNDO_UNLOADED.componentTranslation().setStyle(Styles.RED));
                    BuildingGadgets.LOG.error("Player attempted to undo a Region missing {} unloaded chunks. Denied undo!", unloadedChunks.size());
                    BuildingGadgets.LOG.trace("The following chunks were detected as unloaded {}.", unloadedChunks);
                    return;
                }
            }
            BuildContext buildContext = BuildContext.builder()
                    .player(player)
                    .stack(stack)
                    .build(world);

            UndoScheduler.scheduleUndo(undo, index, buildContext, BuildingGadgets.getConfig().gadgets.placeSteps);
        } else {
            player.sendOverlayMessage(MessageTranslation.NOTHING_TO_UNDO.componentTranslation().setStyle(Styles.RED));
        }
    }

    protected static boolean mayInteract(ServerPlayer player, BlockPos pos) {
        return player.mayInteract((net.minecraft.server.level.ServerLevel) player.level(), pos) && FLANCompat.canUse(player, pos) && FTBChunksCompat.canUse(player, pos);
    }

    // Energy methods for SimpleEnergyItem
    public long getStoredEnergy(ItemStack itemStack) {
        return itemStack.getOrDefault(BGDataComponents.ENERGY, 0L);
    }

    public boolean tryUseEnergy(ItemStack itemStack, long amount) {
        long current = getStoredEnergy(itemStack);
        if (current >= amount) {
            itemStack.set(BGDataComponents.ENERGY, current - amount);
            return true;
        }
        return false;
    }

    public long receiveEnergy(ItemStack itemStack, long maxReceive, boolean simulate) {
        long current = getStoredEnergy(itemStack);
        long toReceive = Math.min(getEnergyCapacity() - current, Math.min(maxReceive, getEnergyMaxInput()));
        if (!simulate && toReceive > 0) {
            itemStack.set(BGDataComponents.ENERGY, current + toReceive);
        }
        return toReceive;
    }

    public long extractEnergy(ItemStack itemStack, long maxExtract, boolean simulate) {
        long current = getStoredEnergy(itemStack);
        long toExtract = Math.min(current, Math.min(maxExtract, getEnergyMaxOutput()));
        if (!simulate && toExtract > 0) {
            itemStack.set(BGDataComponents.ENERGY, current - toExtract);
        }
        return toExtract;
    }
}

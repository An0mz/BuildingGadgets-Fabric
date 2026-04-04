package com.direwolf20.buildinggadgets.common.items;

import com.direwolf20.buildinggadgets.client.renders.BaseRenderer;
import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.blocks.EffectBlock;
import com.direwolf20.buildinggadgets.common.component.BGDataComponents;
import com.direwolf20.buildinggadgets.common.items.modes.AbstractMode;
import com.direwolf20.buildinggadgets.common.items.modes.BuildingModes;
import com.direwolf20.buildinggadgets.common.network.C2S.PacketBindTool;
import com.direwolf20.buildinggadgets.common.network.C2S.PacketRotateMirror;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.inventory.IItemIndex;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.tainted.inventory.MatchResult;
import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.MaterialList;
import com.direwolf20.buildinggadgets.common.tainted.save.Undo;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.direwolf20.buildinggadgets.common.util.helpers.VectorHelper;
import com.direwolf20.buildinggadgets.common.util.lang.LangUtil;
import com.direwolf20.buildinggadgets.common.util.lang.MessageTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.Styles;
import com.direwolf20.buildinggadgets.common.util.lang.TooltipTranslation;
import com.direwolf20.buildinggadgets.common.util.ref.Reference.TagReference;
import com.direwolf20.buildinggadgets.common.world.MockBuilderWorld;
import com.google.common.collect.ImmutableMultiset;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static com.direwolf20.buildinggadgets.common.util.GadgetUtils.*;

public class GadgetBuilding extends AbstractGadget {

    private static final MockBuilderWorld fakeWorld = new MockBuilderWorld();

    public GadgetBuilding(Properties builder) {
        super(builder, TagReference.WHITELIST_BUILDING, TagReference.BLACKLIST_BUILDING);
    }

    @Override
    public long getEnergyCapacity() {
        return BuildingGadgets.getConfig().gadgets.gadgetBuilding.maxEnergy;
    }

    @Override
    public long getEnergyCost(ItemStack tool) {
        return BuildingGadgets.getConfig().gadgets.gadgetBuilding.energyCost;
    }

    public boolean placeAtop(ItemStack stack) {
        return shouldPlaceAtop(stack);
    }

    private static void setToolMode(ItemStack tool, BuildingModes mode) {
        tool.set(BGDataComponents.BUILDING_MODE, mode.name());
    }

    public static BuildingModes getToolMode(ItemStack tool) {
        String modeName = tool.getOrDefault(BGDataComponents.BUILDING_MODE, BuildingModes.VERTICAL_COLUMN.name());
        return BuildingModes.getFromName(modeName);
    }

    public static boolean shouldPlaceAtop(ItemStack stack) {
        return !stack.getOrDefault(BGDataComponents.PLACE_INSIDE, false);
    }

    public static void togglePlaceAtop(Player player, ItemStack stack) {
        boolean current = shouldPlaceAtop(stack);
        stack.set(BGDataComponents.PLACE_INSIDE, current);
        player.displayClientMessage((!current ? MessageTranslation.PLACE_ATOP : MessageTranslation.PLACE_INSIDE).componentTranslation().setStyle(Styles.AQUA), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        BuildingModes mode = getToolMode(stack);
        addEnergyInformation(tooltipAdder, stack);

        tooltipAdder.accept(TooltipTranslation.GADGET_MODE
                .componentTranslation((mode == BuildingModes.SURFACE && getConnectedArea(stack)
                        ? TooltipTranslation.GADGET_CONNECTED.format(Component.translatable(mode.getTranslationKey()).getString())
                        : Component.translatable(mode.getTranslationKey())))
                .setStyle(Styles.AQUA));

        tooltipAdder.accept(TooltipTranslation.GADGET_BLOCK
                .componentTranslation(LangUtil.getFormattedBlockName(getToolBlock(stack).getState()))
                .setStyle(Styles.DK_GREEN));

        int range = getToolRange(stack);
        if (getToolMode(stack) != BuildingModes.BUILD_TO_ME)
            tooltipAdder.accept(TooltipTranslation.GADGET_RANGE
                    .componentTranslation(range, getRangeInBlocks(range, mode.getMode()))
                    .setStyle(Styles.LT_PURPLE));

        if (getToolMode(stack) == BuildingModes.SURFACE)
            tooltipAdder.accept(TooltipTranslation.GADGET_FUZZY
                    .componentTranslation(String.valueOf(getFuzzy(stack)))
                    .setStyle(Styles.GOLD));

        addInformationRayTraceFluid(tooltipAdder, stack);

        tooltipAdder.accept(TooltipTranslation.GADGET_BUILDING_PLACE_ATOP
                .componentTranslation(String.valueOf(shouldPlaceAtop(stack)))
                .setStyle(Styles.YELLOW));
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        player.startUsingItem(hand);
        if (!world.isClientSide) {
            if (player.isShiftKeyDown()) {
                Optional<Block> result = selectBlock(itemstack, player);
                if (result.isEmpty()) {
                    player.displayClientMessage(MessageTranslation.INVALID_BLOCK.componentTranslation(net.minecraft.world.level.block.Blocks.AIR.getName()).setStyle(Styles.AQUA), true);
                    return super.use(world, player, hand);
                }
            } else if (player instanceof ServerPlayer) {
                build((ServerPlayer) player, itemstack);
            }
        } else {
            if (!player.isShiftKeyDown()) {
                BaseRenderer.updateInventoryCache();
            } else {
                if (Screen.hasControlDown()) {
                    PacketBindTool.send();
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    public void setMode(ItemStack heldItem, int modeInt) {
        BuildingModes mode = BuildingModes.values()[modeInt];
        setToolMode(heldItem, mode);
    }

    public static void rangeChange(Player player, ItemStack heldItem) {
        int range = getToolRange(heldItem);
        int changeAmount = (getToolMode(heldItem) != BuildingModes.SURFACE || (range % 2 == 0)) ? 1 : 2;
        if (player.isShiftKeyDown())
            range = (range == 1) ? BuildingGadgets.getConfig().gadgets.maxRange : range - changeAmount;
        else
            range = (range >= BuildingGadgets.getConfig().gadgets.maxRange) ? 1 : range + changeAmount;

        setToolRange(heldItem, range);
        player.displayClientMessage(MessageTranslation.RANGE_SET.componentTranslation(range).setStyle(Styles.AQUA), true);
    }

    private void build(ServerPlayer player, ItemStack stack) {
        Level world = player.level();
        ItemStack heldItem = getGadget(player);
        if (heldItem.isEmpty())
            return;

        List<BlockPos> coords = GadgetUtils.getAnchor(heldItem).orElse(new ArrayList<>());

        BlockData blockData = getToolBlock(heldItem);
        if (blockData.getState() == Blocks.AIR.defaultBlockState()) {
            return;
        }

        if (coords.size() == 0) {
            BlockHitResult lookingAt = VectorHelper.getLookingAt(player, stack);
            if (world.isEmptyBlock(lookingAt.getBlockPos()))
                return;

            Direction sideHit = lookingAt.getDirection();
            coords = getToolMode(stack).getMode().getCollection(
                    new AbstractMode.UseContext(world, blockData.getState(), lookingAt.getBlockPos(), heldItem, sideHit, placeAtop(stack), getConnectedArea(stack)),
                    player
            );
        } else
            setAnchor(stack);

        BlockPos targetPos = VectorHelper.getLookingAt(player, stack).getBlockPos();
        coords.sort(Comparator.comparingDouble(pos -> pos.distSqr(targetPos)));

        Undo.Builder builder = Undo.builder();
        IItemIndex index = InventoryHelper.index(stack, player);

        fakeWorld.setWorldAndState(player.level(), blockData.getState(), coords);
        for (BlockPos coordinate : coords) {
            placeBlock(world, player, index, builder, coordinate, blockData);
        }

        pushUndo(stack, builder.build(world), world);
    }

    private void placeBlock(Level world, ServerPlayer player, IItemIndex index, Undo.Builder builder, BlockPos pos, BlockData setBlock) {
        if ((pos.getY() > world.getMaxY() || pos.getY() < world.getMinY()) || !player.mayBuild())
            return;

        ItemStack heldItem = getGadget(player);
        if (heldItem.isEmpty())
            return;

        BuildContext buildContext = new BuildContext(world, player, heldItem);
        MaterialList requiredItems = setBlock.getRequiredItems(buildContext, null, pos);

        try (Transaction transaction = Transaction.openOuter()) {
            MatchResult match = index.match(requiredItems, transaction);

            if (!match.isSuccess()) {
                return;
            }

            if (!mayInteract(player, pos)) {
                return;
            }

            if (this.useEnergy(heldItem, player)) {
                ImmutableMultiset<ItemVariant> usedItems = match.getChosenOption();
                builder.record(world, pos, setBlock, usedItems, ImmutableMultiset.of());
                EffectBlock.spawnEffectBlock(world, pos, setBlock, EffectBlock.Mode.PLACE);
                transaction.commit();
            }
        }
    }

    public static ItemStack getGadget(Player player) {
        ItemStack stack = AbstractGadget.getGadget(player);
        if (!(stack.getItem() instanceof GadgetBuilding))
            return ItemStack.EMPTY;
        return stack;
    }

    @Override
    public boolean performRotate(ItemStack stack, Player player) {
        GadgetUtils.rotateOrMirrorToolBlock(stack, player, PacketRotateMirror.Operation.ROTATE);
        return true;
    }

    @Override
    public boolean performMirror(ItemStack stack, Player player) {
        GadgetUtils.rotateOrMirrorToolBlock(stack, player, PacketRotateMirror.Operation.MIRROR);
        return true;
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return getEnergyCapacity();
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return getEnergyMaxInput();
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return getEnergyMaxOutput();
    }
}


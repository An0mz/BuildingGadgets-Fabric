package com.direwolf20.buildinggadgets.common.items;

import com.direwolf20.buildinggadgets.client.renders.BaseRenderer;
import com.direwolf20.buildinggadgets.client.screen.GuiMod;
import com.direwolf20.buildinggadgets.client.screen.tooltip.TemplateData;
import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.blocks.EffectBlock;
import com.direwolf20.buildinggadgets.common.commands.ForceUnloadedCommand;
import com.direwolf20.buildinggadgets.common.commands.OverrideBuildSizeCommand;
import com.direwolf20.buildinggadgets.common.commands.OverrideCopySizeCommand;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.component.BGDataComponents;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.TileSupport;
import com.direwolf20.buildinggadgets.common.network.C2S.PacketBindTool;
import com.direwolf20.buildinggadgets.common.network.Target;
import com.direwolf20.buildinggadgets.common.tainted.building.PlacementChecker;
import com.direwolf20.buildinggadgets.common.tainted.building.Region;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.building.view.IBuildView;
import com.direwolf20.buildinggadgets.common.tainted.building.view.WorldBuildView;
import com.direwolf20.buildinggadgets.common.tainted.concurrent.CopyScheduler;
import com.direwolf20.buildinggadgets.common.tainted.concurrent.PlacementScheduler;
import com.direwolf20.buildinggadgets.common.tainted.concurrent.ServerTickingScheduler;
import com.direwolf20.buildinggadgets.common.tainted.inventory.IItemIndex;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.tainted.template.*;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.direwolf20.buildinggadgets.common.util.TemplateKeyHelper;
import com.direwolf20.buildinggadgets.common.util.helpers.VectorHelper;
import com.direwolf20.buildinggadgets.common.util.lang.*;
import com.direwolf20.buildinggadgets.common.util.ref.Reference.TagReference;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedSet;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;
import java.util.Optional;

public class GadgetCutPaste extends AbstractGadget {

    public enum ToolMode {
        CUT(ModeTranslation.CUT),
        PASTE(ModeTranslation.PASTE);

        private final ITranslationProvider translation;

        ToolMode(ITranslationProvider translation) {
            this.translation = translation;
        }

        public ITranslationProvider getTranslation() {
            return translation;
        }
    }

    private static final Joiner CHUNK_JOINER = Joiner.on("; ");

    public GadgetCutPaste(Properties builder) {
        super(builder, TagReference.WHITELIST_CUT_PASTE, TagReference.BLACKLIST_CUT_PASTE);
    }

    @Override
    public long getEnergyCapacity() {
        return BuildingGadgets.getConfig().gadgets.gadgetCutPaste.maxEnergy;
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return getEnergyCapacity();
    }

    @Override
    public long getEnergyCost(ItemStack tool) {
        return BuildingGadgets.getConfig().gadgets.gadgetCutPaste.energyCost;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return getEnergyMaxInput();
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return getEnergyMaxOutput();
    }

    @Override
    public long getEnergyMaxOutput() {
        return 10000;
    }

    // ---- Component helpers ----

    public static void setRelativeVector(ItemStack stack, BlockPos vec) {
        if (vec.equals(BlockPos.ZERO)) {
            stack.remove(BGDataComponents.CUT_RELATIVE_VECTOR);
        } else {
            stack.set(BGDataComponents.CUT_RELATIVE_VECTOR, vec);
        }
    }

    public static BlockPos getRelativeVector(ItemStack stack) {
        return stack.getOrDefault(BGDataComponents.CUT_RELATIVE_VECTOR, BlockPos.ZERO);
    }

    public static Optional<BlockPos> getActivePos(Player player, ItemStack stack) {
        BlockPos pos = ((AbstractGadget) stack.getItem()).getAnchor(stack);
        if (pos == null) {
            BlockHitResult res = VectorHelper.getLookingAt(player, stack);
            if (res == null || res.getType() == HitResult.Type.MISS) {
                return Optional.empty();
            }
            pos = res.getBlockPos().relative(res.getDirection());
        }
        return Optional.of(pos).map(p -> p.offset(getRelativeVector(stack)));
    }

    public static Optional<Region> getSelectedRegion(ItemStack stack) {
        BlockPos lower = getLowerRegionBound(stack);
        BlockPos upper = getUpperRegionBound(stack);
        if (lower != null && upper != null) {
            return Optional.of(new Region(lower, upper));
        }
        return Optional.empty();
    }

    public static void setSelectedRegion(ItemStack stack, @Nullable Region region) {
        if (region != null) {
            setLowerRegionBound(stack, region.getMin());
            setUpperRegionBound(stack, region.getMax());
        } else {
            setLowerRegionBound(stack, null);
            setUpperRegionBound(stack, null);
        }
    }

    public static void setUpperRegionBound(ItemStack stack, @Nullable BlockPos pos) {
        if (pos != null) stack.set(BGDataComponents.CUT_UPPER_REGION_BOUND, pos);
        else stack.remove(BGDataComponents.CUT_UPPER_REGION_BOUND);
    }

    public static void setLowerRegionBound(ItemStack stack, @Nullable BlockPos pos) {
        if (pos != null) stack.set(BGDataComponents.CUT_LOWER_REGION_BOUND, pos);
        else stack.remove(BGDataComponents.CUT_LOWER_REGION_BOUND);
    }

    @Nullable
    public static BlockPos getUpperRegionBound(ItemStack stack) {
        return stack.get(BGDataComponents.CUT_UPPER_REGION_BOUND);
    }

    @Nullable
    public static BlockPos getLowerRegionBound(ItemStack stack) {
        return stack.get(BGDataComponents.CUT_LOWER_REGION_BOUND);
    }

    public static void setToolMode(ItemStack stack, ToolMode mode) {
        stack.set(BGDataComponents.CUT_PASTE_MODE, mode.name());
    }

    /** Called by PacketToggleMode when the player selects from the radial wheel. */
    public void setMode(ItemStack stack, int modeInt) {
        ToolMode mode = ToolMode.values()[modeInt % ToolMode.values().length];
        setToolMode(stack, mode);
    }

    /**
     * Triggered server-side by the "Cut" button in the radial menu.
     * Validates the region and starts the copy+remove operation.
     */
    public void performCutFromButton(ItemStack stack, Player player) {
        Optional<Region> regionOpt = getSelectedRegion(stack);
        if (regionOpt.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("buildinggadgets.message.cut_no_region").withStyle(ChatFormatting.RED), true);
            return;
        }
        Region region = regionOpt.get();
        if (checkRegion(player.level(), player, region)) {
            tryCut(stack, player.level(), player, region);
        }
    }

    public static ToolMode getToolMode(ItemStack stack) {
        String modeName = stack.getOrDefault(BGDataComponents.CUT_PASTE_MODE, ToolMode.CUT.name());
        try {
            return ToolMode.valueOf(modeName);
        } catch (IllegalArgumentException e) {
            return ToolMode.CUT;
        }
    }

    public static ItemStack getGadget(Player player) {
        ItemStack stack = AbstractGadget.getGadget(player);
        if (!(stack.getItem() instanceof GadgetCutPaste)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    // ---- Tooltip ----

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addEnergyInformation(tooltip, stack);
        tooltip.add(TooltipTranslation.GADGET_MODE
                .componentTranslation(getToolMode(stack).translation.format())
                .setStyle(Styles.AQUA));
        addInformationRayTraceFluid(tooltip, stack);
        GadgetUtils.addTooltipNameAndAuthor(stack, null, tooltip);

        ToolMode mode = getToolMode(stack);
        if (mode == ToolMode.CUT) {
            BlockPos lower = getLowerRegionBound(stack);
            BlockPos upper = getUpperRegionBound(stack);
            if (lower != null) {
                tooltip.add(Component.translatable("buildinggadgets.tooltip.gadget.cut.pos1", lower.toShortString())
                        .withStyle(ChatFormatting.YELLOW));
            }
            if (upper != null) {
                tooltip.add(Component.translatable("buildinggadgets.tooltip.gadget.cut.pos2", upper.toShortString())
                        .withStyle(ChatFormatting.YELLOW));
            }
            if (lower == null) {
                tooltip.add(Component.translatable("buildinggadgets.tooltip.gadget.cut.hint")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    // ---- Use / Interaction ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);

        BlockHitResult posLookingAt = VectorHelper.getLookingAt(player, stack);
        boolean lookingAtInventory = ItemStorage.SIDED.find(world,
                posLookingAt.getBlockPos(), posLookingAt.getDirection()) != null;

        if (!world.isClientSide()) {
            if (player.isShiftKeyDown() && lookingAtInventory) {
                return InteractionResultHolder.pass(stack);
            }

            ToolMode mode = getToolMode(stack);

            if (mode == ToolMode.CUT) {
                if (!world.getBlockState(posLookingAt.getBlockPos()).isAir()) {
                    if (player.isShiftKeyDown()) {
                        // Second position — just set upper bound; cut is triggered from radial menu
                        setUpperRegionBound(stack, posLookingAt.getBlockPos());
                        Optional<Region> regionOpt = getSelectedRegion(stack);
                        if (regionOpt.isPresent()) {
                            player.displayClientMessage(
                                    Component.translatable("buildinggadgets.message.cut_region_ready").withStyle(ChatFormatting.YELLOW), true);
                        } else {
                            player.displayClientMessage(
                                    MessageTranslation.FIRST_COPY.componentTranslation().setStyle(Styles.DK_GREEN), true);
                        }
                    } else {
                        // First position — set lower bound, clear upper
                        setLowerRegionBound(stack, posLookingAt.getBlockPos());
                        setUpperRegionBound(stack, null);
                        player.displayClientMessage(
                                MessageTranslation.FIRST_COPY.componentTranslation().setStyle(Styles.DK_GREEN), true);
                    }
                } else if (posLookingAt.getType() == HitResult.Type.MISS || world.getBlockState(posLookingAt.getBlockPos()).isAir()) {
                    // Right-click air resets selection
                    if (!player.isShiftKeyDown()) {
                        setSelectedRegion(stack, null);
                        player.displayClientMessage(
                                MessageTranslation.AREA_RESET.componentTranslation().setStyle(Styles.AQUA), true);
                    }
                }
            } else if (mode == ToolMode.PASTE && !player.isShiftKeyDown()) {
                getActivePos(player, stack).ifPresent(pos -> build(stack, world, player, pos, hand));
            }
        } else {
            // Client side
            if (player.isShiftKeyDown() && Screen.hasControlDown() && lookingAtInventory) {
                PacketBindTool.send();
                return InteractionResultHolder.pass(stack);
            }
            if (getToolMode(stack) == ToolMode.PASTE) {
                if (player.isShiftKeyDown()) {
                    GuiMod.PASTE.openScreen(player);
                } else {
                    BaseRenderer.updateInventoryCache();
                }
            }
        }

        return InteractionResultHolder.success(stack);
    }

    // ---- Cut Logic ----

    private boolean checkRegion(Level world, Player player, Region region) {
        if (!ForceUnloadedCommand.mayForceUnloadedChunks(player)) {
            ImmutableSortedSet<ChunkPos> unloaded = region.getUnloadedChunks(world);
            if (!unloaded.isEmpty()) {
                player.displayClientMessage(
                        MessageTranslation.COPY_UNLOADED.componentTranslation(unloaded.size()).setStyle(Styles.RED), true);
                return false;
            }
        }
        int maxDimension = BuildingGadgets.getConfig().gadgets.gadgetCutPaste.maxCopySize;
        if (region.getXSize() > 0xFFFF || region.getYSize() > 255 || region.getZSize() > 0xFFFF ||
                ((region.getXSize() > maxDimension || region.getYSize() > maxDimension || region.getZSize() > maxDimension)
                        && !OverrideCopySizeCommand.mayPerformLargeCopy(player))) {
            BlockPos sizeVec = region.getMax().subtract(region.getMin());
            player.displayClientMessage(MessageTranslation.COPY_TOO_LARGE
                    .componentTranslation(sizeVec.getX(), sizeVec.getY(), sizeVec.getZ(),
                            Math.min(maxDimension, 0xFFFF), Math.min(maxDimension, 255), Math.min(maxDimension, 0xFFFF))
                    .setStyle(Styles.RED), true);
            return false;
        }
        return true;
    }

    public void tryCut(ItemStack stack, Level world, Player player, Region region) {
        BuildContext context = BuildContext.builder()
                .player(player)
                .stack(stack)
                .build(world);
        WorldBuildView buildView = WorldBuildView.create(context, region,
                (c, p) -> InventoryHelper.getSafeBlockData(player, p, player.getUsedItemHand()));
        performCut(stack, buildView, region);
    }

    private void performCut(ItemStack stack, WorldBuildView buildView, Region cutRegion) {
        BuildContext context = buildView.getContext();
        assert context.getPlayer() != null;
        Player player = context.getPlayer();
        // Use the Level from the player — the player's level is a Level, not just LevelAccessor
        Level world = player.level();

        CopyScheduler.scheduleCopy((map, region) -> {
            Template newTemplate = new Template(map,
                    TemplateHeader.builder(region)
                            .name("Cut")
                            .author(player.getName().getString())
                            .build());
            onCutFinished(newTemplate.normalize(), stack, player, world, cutRegion);
        }, buildView, BuildingGadgets.getConfig().gadgets.placeSteps);
    }

    private void onCutFinished(Template newTemplate, ItemStack stack, Player player, Level world, Region cutRegion) {
        // Save template using the same key system as GadgetCopyPaste
        if (!TemplateKeyHelper.hasTemplateKey(stack)) {
            TemplateKeyHelper.initializeTemplateKey(stack);
        }
        ITemplateKey key = TemplateKeyHelper.getTemplateKey(stack);
        if (key != null) {
            BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(player.level()).ifPresent(provider -> {
                provider.setTemplate(key, newTemplate);
                provider.requestRemoteUpdate(key, new Target(PacketFlow.CLIENTBOUND, (ServerPlayer) player));
            });
        }

        // Remove blocks from the world using EffectBlock animation (shrink + remove after 20 ticks)
        if (world instanceof ServerLevel serverLevel) {
            for (BlockPos pos : BlockPos.betweenClosed(cutRegion.getMin(), cutRegion.getMax())) {
                BlockState state = serverLevel.getBlockState(pos);
                if (!state.isAir()) {
                    BlockData blockData = TileSupport.createBlockData(state, serverLevel.getBlockEntity(pos));
                    EffectBlock.spawnEffectBlock(serverLevel, pos, blockData, EffectBlock.Mode.REMOVE);
                }
            }
        }

        // Clear selection and notify player; delay switching to PASTE mode until the
        // EffectBlock REMOVE animations have finished (lifespan = 20 ticks, +5 buffer).
        setSelectedRegion(stack, null);
        player.displayClientMessage(
                Component.translatable("buildinggadgets.message.cut_complete").withStyle(ChatFormatting.GREEN), true);

        int[] countdown = {25};
        ServerTickingScheduler.runTicked(() -> {
            if (--countdown[0] <= 0) {
                setToolMode(stack, ToolMode.PASTE);
                return true; // done — remove task
            }
            return false; // keep waiting
        });
    }

    // ---- Paste Logic (same as GadgetCopyPaste) ----

    private void build(ItemStack stack, Level world, Player player, BlockPos pos, InteractionHand hand) {
        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(world).ifPresent((ITemplateProvider provider) -> {
            ITemplateKey key = TemplateKeyHelper.getTemplateKey(stack);
            if (key != null) {
                Template template = provider.getTemplateForKey(key);
                BuildContext buildContext = BuildContext.builder()
                        .stack(stack)
                        .player(player)
                        .build(world);
                IBuildView view = template.createViewInContext(buildContext);
                view.translateTo(pos);
                if (!checkPlacement(world, player, view.getBoundingBox())) {
                    return;
                }
                schedulePlacement(stack, view, player, hand);
            }
        });
    }

    private boolean checkPlacement(Level world, Player player, Region region) {
        if (!ForceUnloadedCommand.mayForceUnloadedChunks(player)) {
            ImmutableSortedSet<ChunkPos> unloaded = region.getUnloadedChunks(world);
            if (!unloaded.isEmpty()) {
                player.displayClientMessage(
                        MessageTranslation.BUILD_UNLOADED.componentTranslation(unloaded.size()).setStyle(Styles.RED), true);
                return false;
            }
        }
        int maxDimension = BuildingGadgets.getConfig().gadgets.gadgetCutPaste.maxBuildSize;
        if ((region.getXSize() > maxDimension || region.getYSize() > maxDimension || region.getZSize() > maxDimension) &&
                !OverrideBuildSizeCommand.mayPerformLargeBuild(player)) {
            BlockPos sizeVec = region.getMax().subtract(region.getMin());
            player.displayClientMessage(MessageTranslation.BUILD_TOO_LARGE
                    .componentTranslation(sizeVec.getX(), sizeVec.getY(), sizeVec.getZ(),
                            maxDimension, maxDimension, maxDimension)
                    .setStyle(Styles.RED), true);
            return false;
        }
        return true;
    }

    private void schedulePlacement(ItemStack stack, IBuildView view, Player player, InteractionHand hand) {
        ServerLevel world = view.getContext().getServerWorld();
        IItemIndex index = InventoryHelper.index(stack, player);
        long energyCost = getEnergyCost(stack);
        boolean overwrite = BuildingGadgets.getConfig().general.allowOverwriteBlocks;
        BlockPlaceContext useContext = new BlockPlaceContext(
                new UseOnContext(player, InteractionHand.MAIN_HAND, VectorHelper.getLookingAt(player, stack)));
        PlacementChecker checker = new PlacementChecker(
                EnergyStorage.ITEM.find(stack, ContainerItemContext.ofPlayerHand(player, hand)),
                t -> energyCost,
                index,
                (c, t) -> overwrite
                        ? world.getBlockState(t.getPos()).canBeReplaced(useContext) && mayInteract((ServerPlayer) player, t.getPos())
                        : world.isEmptyBlock(t.getPos()) && mayInteract((ServerPlayer) player, t.getPos()));
        PlacementScheduler.schedulePlacement(view, checker, BuildingGadgets.getConfig().gadgets.placeSteps)
                .withFinisher(p -> {
                    pushUndo(stack, p.getUndoBuilder().build(world), world);
                    player.displayClientMessage(
                            MessageTranslation.TEMPLATE_BUILD.componentTranslation().setStyle(Styles.DK_GREEN), true);
                    // One-shot paste: remove template and return to CUT mode
                    stack.remove(BGDataComponents.TEMPLATE_KEY);
                    setToolMode(stack, ToolMode.CUT);
                });
    }

    // ---- Rotation / Mirror ----

    @Override
    public boolean performRotate(ItemStack stack, Player player) {
        return BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(player.level()).map(provider -> {
            ITemplateKey key = TemplateKeyHelper.getTemplateKey(stack);
            if (key == null) return false;
            Template template = provider.getTemplateForKey(key);
            provider.setTemplate(key, template.rotate(Rotation.CLOCKWISE_90));
            provider.requestRemoteUpdate(key, new Target(PacketFlow.CLIENTBOUND, (ServerPlayer) player));
            return true;
        }).orElse(false);
    }

    @Override
    public boolean performMirror(ItemStack stack, Player player) {
        return BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(player.level()).map(provider -> {
            ITemplateKey key = TemplateKeyHelper.getTemplateKey(stack);
            if (key == null) return false;
            Template template = provider.getTemplateForKey(key);
            provider.setTemplate(key, template.mirror(player.getDirection().getAxis()));
            provider.requestRemoteUpdate(key, new Target(PacketFlow.CLIENTBOUND, (ServerPlayer) player));
            return true;
        }).orElse(false);
    }

    // ---- Tooltip image (template preview) ----

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack itemStack) {
        return Optional.of(new TemplateData(itemStack));
    }
}




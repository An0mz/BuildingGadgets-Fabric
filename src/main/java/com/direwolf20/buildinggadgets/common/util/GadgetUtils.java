package com.direwolf20.buildinggadgets.common.util;

import com.direwolf20.buildinggadgets.common.blocks.EffectBlock;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.component.BGDataComponents;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.items.modes.AbstractMode;
import com.direwolf20.buildinggadgets.common.network.C2S.PacketRotateMirror;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryLinker;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.tainted.template.Template;
import com.direwolf20.buildinggadgets.common.tainted.template.TemplateHeader;
import com.direwolf20.buildinggadgets.common.util.helpers.VectorHelper;
import com.direwolf20.buildinggadgets.common.util.lang.MessageTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.Styles;
import com.direwolf20.buildinggadgets.common.util.lang.TooltipTranslation;
import com.direwolf20.buildinggadgets.common.util.ref.NBTKeys;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GadgetUtils {
    private static final ImmutableList<Block> DISALLOWED_BLOCKS = ImmutableList.of(
            Blocks.END_PORTAL, Blocks.NETHER_PORTAL, Blocks.END_PORTAL_FRAME, Blocks.BEDROCK, Blocks.SPAWNER
    );

    private static final ImmutableList<String> LINK_STARTS = ImmutableList.of("http", "www");

    public static boolean mightBeLink(final String s) {
        return LINK_STARTS.stream().anyMatch(s::startsWith);
    }

    public static void addTooltipNameAndAuthor(ItemStack stack, @Nullable Level world, java.util.function.Consumer<Component> tooltipAdder) {
        if (world == null) return;

        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(world).ifPresent(provider -> {
            ITemplateKey key = TemplateKeyHelper.getTemplateKey(stack);
            if (key != null) {
                Template template = provider.getTemplateForKey(key);
                TemplateHeader header = template.getHeader();
                if (header.getName() != null && !header.getName().isEmpty())
                    tooltipAdder.accept(TooltipTranslation.TEMPLATE_NAME.componentTranslation(header.getName()).setStyle(Styles.AQUA));
                if (header.getAuthor() != null && !header.getAuthor().isEmpty())
                    tooltipAdder.accept(TooltipTranslation.TEMPLATE_AUTHOR.componentTranslation(header.getAuthor()).setStyle(Styles.AQUA));
            }
        });
    }

    @Nullable
    public static ByteArrayOutputStream getPasteStream(@NotNull CompoundTag compound, @Nullable String name) throws IOException {
        CompoundTag withText = name != null && !name.isEmpty() ? compound.copy() : compound;
        if (name != null && !name.isEmpty()) withText.putString(NBTKeys.TEMPLATE_NAME, name);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NbtIo.writeCompressed(withText, baos);
        return baos.size() < Short.MAX_VALUE - 200 ? baos : null;
    }

    public static void setAnchor(ItemStack stack) {
        setAnchor(stack, new ArrayList<>());
    }

    public static void setAnchor(ItemStack stack, List<BlockPos> coordinates) {
        if (coordinates.isEmpty()) {
            stack.remove(BGDataComponents.ANCHOR_COORDS);
        } else {
            stack.set(BGDataComponents.ANCHOR_COORDS, coordinates);
        }
    }

    public static Optional<List<BlockPos>> getAnchor(ItemStack stack) {
        List<BlockPos> coords = stack.get(BGDataComponents.ANCHOR_COORDS);
        if (coords == null || coords.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(coords);
    }

    public static void setToolRange(ItemStack stack, int range) {
        stack.set(BGDataComponents.TOOL_RANGE, range);
    }

    public static int getToolRange(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(BGDataComponents.TOOL_RANGE, 1), 1, 15);
    }

    public static BlockData rotateOrMirrorBlock(Player player, PacketRotateMirror.Operation operation, BlockData data) {
        if (operation == PacketRotateMirror.Operation.MIRROR)
            return data.mirror(player.getDirection().getAxis() == Axis.X ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK);

        return data.rotate(Rotation.CLOCKWISE_90);
    }

    public static void rotateOrMirrorToolBlock(ItemStack stack, Player player, PacketRotateMirror.Operation operation) {
        setToolBlock(stack, rotateOrMirrorBlock(player, operation, getToolBlock(stack)));
    }

    private static void setToolBlock(ItemStack stack, @Nullable BlockData data) {
        if (data == null)
            data = BlockData.AIR;

        CompoundTag stateTag = data.serialize(true);
        stack.set(BGDataComponents.TOOL_BLOCK, stateTag);
    }

    @NotNull
    public static BlockData getToolBlock(ItemStack stack) {
        CompoundTag stateTag = stack.get(BGDataComponents.TOOL_BLOCK);
        if (stateTag == null) {
            setToolBlock(stack, BlockData.AIR);
            return BlockData.AIR;
        }

        BlockData res = BlockData.tryDeserialize(stateTag, true);
        if (res == null) {
            setToolBlock(stack, BlockData.AIR);
            return BlockData.AIR;
        }
        return res;
    }

    public static void linkToInventory(ItemStack stack, Player player) {
        Level world = player.level();
        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, AbstractGadget.shouldRayTraceFluid(stack) ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE);
        if (world.getBlockState(VectorHelper.getLookingAt(player, stack).getBlockPos()) == Blocks.AIR.defaultBlockState())
            return;

        InventoryLinker.Result result = InventoryLinker.linkInventory(player.level(), stack, lookingAt);
        player.sendSystemMessage(result.i18n().componentTranslation());
    }

    public static Optional<Block> selectBlock(ItemStack stack, Player player) {
        Level world = player.level();
        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, AbstractGadget.shouldRayTraceFluid(stack) ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE);
        if (world.isEmptyBlock(lookingAt.getBlockPos()))
            return Optional.empty();

        BlockState state = world.getBlockState(lookingAt.getBlockPos());
        if (!((AbstractGadget) stack.getItem()).isAllowedBlock(state.getBlock()) || state.getBlock() instanceof EffectBlock)
            return Optional.empty();

        if (DISALLOWED_BLOCKS.contains(state.getBlock())) {
            return Optional.empty();
        }

        if (state.getDestroySpeed(world, lookingAt.getBlockPos()) < 0) {
            return Optional.empty();
        }

        Optional<BlockData> data = InventoryHelper.getSafeBlockData(player, lookingAt.getBlockPos(), player.getUsedItemHand());
        data.ifPresent(placeState -> {
            BlockState actualState = placeState.getState();
            setToolBlock(stack, new BlockData(actualState, placeState.getTileData()));
        });

        return Optional.of(state.getBlock());
    }

    public static InteractionResult setRemoteInventory(ItemStack stack, Player player, Level world, BlockPos pos, boolean setTool) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null)
            return InteractionResult.PASS;

        return InteractionResult.FAIL;
    }

    public static boolean anchorBlocks(Player player, ItemStack stack) {
        Optional<List<BlockPos>> anchorCoords = getAnchor(stack);

        if (anchorCoords.isPresent()) {
            setAnchor(stack);
            player.sendSystemMessage(MessageTranslation.ANCHOR_REMOVED.componentTranslation().setStyle(Styles.AQUA));
            return true;
        }

        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, stack);
        BlockPos startBlock = lookingAt.getBlockPos();
        Direction sideHit = lookingAt.getDirection();

        if (player.level().isEmptyBlock(startBlock))
            return false;

        BlockData blockData = getToolBlock(stack);
        AbstractMode.UseContext context = new AbstractMode.UseContext(player.level(), blockData.getState(), startBlock, stack, sideHit, stack.getItem() instanceof GadgetBuilding && GadgetBuilding.shouldPlaceAtop(stack), GadgetBuilding.getConnectedArea(stack));

        List<BlockPos> coords = stack.getItem() instanceof GadgetBuilding
                ? GadgetBuilding.getToolMode(stack).getMode().getCollection(context, player)
                : GadgetExchanger.getToolMode(stack).getMode().getCollection(context, player);

        setAnchor(stack, coords);
        player.sendSystemMessage(MessageTranslation.ANCHOR_SET.componentTranslation().setStyle(Styles.AQUA));

        return true;
    }

    public static String withSuffix(int count) {
        if (count < 1000) return "" + count;
        int exp = (int) (Math.log(count) / Math.log(1000));
        return String.format("%.1f%c",
                count / Math.pow(1000, exp),
                "kMGTPE".charAt(exp - 1));
    }

    public static void writePOSToNBT(ItemStack stack, @Nullable BlockPos pos, String tagName) {
        // This method is kept for compatibility with AbstractGadget.getAnchor/setAnchor
        // which use BGDataComponents.ANCHOR directly
        // Note: This is now handled by DataComponents in AbstractGadget
    }

    @Nullable
    public static BlockPos getPOSFromNBT(ItemStack stack, String tagName) {
        // This method is kept for compatibility with AbstractGadget.getAnchor
        // which uses BGDataComponents.ANCHOR directly
        // Note: This is now handled by DataComponents in AbstractGadget
        return null;
    }
}
package com.direwolf20.buildinggadgets.client.renders;

import com.direwolf20.buildinggadgets.client.events.WorldRenderContextWrapper;
import com.direwolf20.buildinggadgets.client.renderer.GhostBlockRenderUtil;
import com.direwolf20.buildinggadgets.client.renderer.OurRenderTypes;
import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.blocks.OurBlocks;
import com.direwolf20.buildinggadgets.common.items.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.items.modes.AbstractMode;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.MaterialList;
import com.direwolf20.buildinggadgets.common.util.helpers.VectorHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.direwolf20.buildinggadgets.common.util.GadgetUtils.getAnchor;
import static com.direwolf20.buildinggadgets.common.util.GadgetUtils.getToolBlock;

public class BuildRender extends BaseRenderer {
    private final boolean isExchanger;
    private static final BlockState DEFAULT_EFFECT_BLOCK = OurBlocks.EFFECT_BLOCK.defaultBlockState();
    private BlockState errorState;

    public BuildRender(boolean isExchanger) {
        this.isExchanger = isExchanger;
    }

    @Override
    public void render(WorldRenderContextWrapper evt, Player player, ItemStack heldItem) {
        super.render(evt, player, heldItem);

        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, heldItem);
        BlockState startBlock = player.level().getBlockState(lookingAt.getBlockPos());
        Optional<List<BlockPos>> anchor = getAnchor(heldItem);

        if ((player.level().isEmptyBlock(lookingAt.getBlockPos()) && anchor.isEmpty()) || startBlock == DEFAULT_EFFECT_BLOCK) {
            return;
        }

        BlockData data = getToolBlock(heldItem);
        BlockState renderBlockState = data.getState();

        if (errorState == renderBlockState || renderBlockState == BaseRenderer.AIR) {
            return;
        }

        if (errorState != null) {
            errorState = null;
        }

        List<BlockPos> coordinates = anchor.orElseGet(() -> {
            AbstractMode mode = !this.isExchanger ? GadgetBuilding.getToolMode(heldItem).getMode() : GadgetExchanger.getToolMode(heldItem).getMode();
            return mode.getCollection(
                    new AbstractMode.UseContext(player.level(), renderBlockState, lookingAt.getBlockPos(), heldItem, lookingAt.getDirection(), !this.isExchanger && GadgetBuilding.shouldPlaceAtop(heldItem), GadgetBuilding.getConnectedArea(heldItem)),
                    player
            );
        });

        BlockPos targetPos = lookingAt.getBlockPos();
        coordinates.sort(Comparator.comparingDouble(pos -> pos.distSqr(targetPos)));

        getBuilderWorld().setWorldAndState(player.level(), renderBlockState, coordinates);

        Vec3 playerPos = evt.camera().position();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        // Camera-relative PoseStack – used for ALL MultiBufferSource (CPU) vertex submissions.
        // Both the ghost block quads and the overlay boxes use the same camera-relative space.
        PoseStack matrix = evt.matrixStack();
        matrix.pushPose();
        matrix.translate(-playerPos.x(), -playerPos.y(), -playerPos.z());

        // Render ghost block textures via CPU MultiBufferSource using putBakedQuad.
        // The pose (-cameraPos + coordinate) is camera-relative, which is the correct
        // coordinate space for MultiBufferSource rendering.
        if (renderBlockState.getRenderShape() == RenderShape.MODEL) {
            BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
            BlockStateModel model = modelSet.get(renderBlockState);
            VertexConsumer ghostConsumer = buffer.getBuffer(OurRenderTypes.GhostBlock);

            // Shared QuadInstance; renderPartsWithTint sets the color per-quad to apply
            // the correct biome tint (e.g. grass green) while keeping 50% alpha.
            QuadInstance quadInstance = new QuadInstance();
            quadInstance.setLightCoords(0xF000F0); // full-bright lightmap
            quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

            // Pre-build a set of ghost positions for cull-between-ghost-blocks culling.
            // This eliminates Z-fighting on shared faces between adjacent ghost blocks.
            Set<BlockPos> ghostSet = new HashSet<>(coordinates);

            for (BlockPos coordinate : coordinates) {
                matrix.pushPose();
                matrix.translate(coordinate.getX(), coordinate.getY(), coordinate.getZ());

                try {
                    List<BlockStateModelPart> parts = new ArrayList<>();
                    // Use the block position as the random seed so model variants are
                    // stable across frames (no per-frame flickering).
                    model.collectParts(RandomSource.create(coordinate.asLong()), parts);
                    // Cull faces adjacent to solid real-world blocks and adjacent ghost blocks.
                    parts = GhostBlockRenderUtil.cullAgainstNeighbors(parts, player.level(), coordinate, ghostSet);

                    // Render quads with correct biome tint (e.g. grass green, leaf green).
                    // 0x80 = 50% alpha so the preview is semi-transparent.
                    GhostBlockRenderUtil.renderPartsWithTint(
                            ghostConsumer, matrix.last(), parts, renderBlockState, coordinate, 0x80, quadInstance);
                } catch (Exception e) {
                    BuildingGadgets.LOG.error("Failed to render blockstate with gadget, not rendering blockstate", e);
                    errorState = renderBlockState;
                }

                matrix.popPose();
            }
        }

        // In survival mode, show per-block availability indicator on top of the ghost texture.
        // Creative mode needs no overlay since the blue-tinted ghost texture already looks like a preview.
        if (!player.isCreative()) {
            boolean hasLinkedInventory = getCacheInventory().maintainCache(heldItem);
            BuildContext context = new BuildContext(player.level(), player, heldItem);
            MaterialList materials = data.getRequiredItems(context, null, null);

            // Count available items directly from the player's inventory.
            Map<Item, Integer> itemCounts = new HashMap<>();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack s = player.getInventory().getItem(slot);
                if (!s.isEmpty()) {
                    itemCounts.merge(s.getItem(), s.getCount(), Integer::sum);
                }
            }

            // Count items from the linked remote inventory cache
            int remainingCached = 0;
            if (hasLinkedInventory && getCacheInventory().getCache() != null) {
                remainingCached = getCacheInventory().getCache().size();
            }

            for (BlockPos coordinate : coordinates) {
                boolean renderFree = false;
                boolean hasItems = checkAndConsumeItems(itemCounts, materials);
                VertexConsumer builder = buffer.getBuffer(OurRenderTypes.MissingBlockOverlay);

                if (!hasItems) {
                    if (hasLinkedInventory && remainingCached > 0) {
                        renderFree = true;
                        remainingCached--;
                    } else {
                        renderMissingBlock(matrix.last().pose(), builder, coordinate);
                    }
                }

                if (renderFree) {
                    // Linked inventory: green overlay to indicate it will be supplied remotely
                    renderBoxSolid(matrix.last().pose(), builder, coordinate, 0.5f, 1.0f, 0.6f, 0.35f);
                }
            }
        }

        matrix.popPose();
        buffer.endBatch();
    }

    @Override
    public boolean isLinkable() {
        return true;
    }

    /**
     * Checks whether the required items from the MaterialList are available in the given
     * item count map, and if so, consumes them (decrements the counts).
     * Tries each OR-option in order; the first satisfiable option wins.
     */
    private static boolean checkAndConsumeItems(Map<Item, Integer> itemCounts, MaterialList materials) {
        for (com.google.common.collect.ImmutableMultiset<ItemVariant> option : materials) {
            if (option.isEmpty()) {
                return true; // no items required
            }
            boolean canFulfill = true;
            Map<Item, Integer> toConsume = new HashMap<>();
            for (com.google.common.collect.Multiset.Entry<ItemVariant> entry : option.entrySet()) {
                Item item = entry.getElement().getItem();
                int needed = entry.getCount();
                int available = itemCounts.getOrDefault(item, 0) - toConsume.getOrDefault(item, 0);
                if (available >= needed) {
                    toConsume.merge(item, needed, Integer::sum);
                } else {
                    canFulfill = false;
                    break;
                }
            }
            if (canFulfill) {
                for (Map.Entry<Item, Integer> e : toConsume.entrySet()) {
                    itemCounts.merge(e.getKey(), -e.getValue(), Integer::sum);
                }
                return true;
            }
        }
        return false;
    }
}


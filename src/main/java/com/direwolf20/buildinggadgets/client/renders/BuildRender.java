package com.direwolf20.buildinggadgets.client.renders;

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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Level;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public void render(WorldRenderContext evt, Player player, ItemStack heldItem) {
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

        if(errorState != null) {
            errorState = null;
        }

        // Get the coordinates from the anchor. If the anchor isn't present then build the collector.
        List<BlockPos> coordinates = new ArrayList<>(anchor.orElseGet(() -> {
            AbstractMode mode = !this.isExchanger ? GadgetBuilding.getToolMode(heldItem).getMode() : GadgetExchanger.getToolMode(heldItem).getMode();
            return mode.getCollection(
                    new AbstractMode.UseContext(player.level(), renderBlockState, lookingAt.getBlockPos(), heldItem, lookingAt.getDirection(), !this.isExchanger && GadgetBuilding.shouldPlaceAtop(heldItem), GadgetBuilding.getConnectedArea(heldItem)),
                    player
            );
        }));

        BlockPos targetPos = lookingAt.getBlockPos();
        coordinates.sort(Comparator.comparingDouble(pos -> pos.distSqr(targetPos)));
        // Sort them on a new line for readability
//        coordinates = SortingHelper.Blocks.byDistance(coordinates, player);

        //Prepare the fake world -- using a fake world lets us render things properly, like fences connecting.
        getBuilderWorld().setWorldAndState(player.level(), renderBlockState, coordinates);

        Vec3 playerPos = evt.camera().getPosition();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        //Save the current position that is being rendered (I think)
        PoseStack matrix = evt.matrixStack();
        matrix.pushPose();
        matrix.translate(-playerPos.x(), -playerPos.y(), -playerPos.z());

        BlockRenderDispatcher dispatcher = evt.gameRenderer().getMinecraft().getBlockRenderer();

        RenderSystem.enableDepthTest();
        for (BlockPos coordinate : coordinates) {
            matrix.pushPose();
            matrix.translate(coordinate.getX(), coordinate.getY(), coordinate.getZ());

            if (this.isExchanger) {
                matrix.translate(-0.0005f, -0.0005f, -0.0005f);
                matrix.scale(1.001f, 1.001f, 1.001f);
            }

            try{
                OurRenderTypes.MultiplyAlphaRenderTypeBuffer mutatedBuffer = new OurRenderTypes.MultiplyAlphaRenderTypeBuffer(Minecraft.getInstance().renderBuffers().bufferSource(), .55f);
                dispatcher.renderSingleBlock(renderBlockState, matrix, mutatedBuffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            catch (Exception e) {
                BuildingGadgets.LOG.log(Level.ERROR, "Failed to render blockstate with gadget, not rendering blockstate");
                errorState = renderBlockState;
            }

            matrix.popPose();
            buffer.endBatch();
        }
        RenderSystem.disableDepthTest();

        // Don't even waste the time checking to see if we have the right energy, items, etc for creative mode
        if (!player.isCreative()) {
            boolean hasLinkedInventory = getCacheInventory().maintainCache(heldItem);
            int remainingCached = getCacheInventory().getCache() == null ? -1 : getCacheInventory().getCache().count(ItemVariant.of(data.getState().getBlock().asItem()));

            BuildContext context = new BuildContext(player.level(), player, heldItem);
            MaterialList materials = data.getRequiredItems(context, null, null);

            // Count available items directly from the player's inventory.
            // This avoids Fabric Transfer API transaction issues on the client render thread.
            // Note: energy is intentionally not checked here — the gadget's energy bar already
            // communicates charge level, and including it in the condition caused all preview
            // blocks to show red whenever the gadget was uncharged, even if the player had
            // the required items in their inventory.
            Map<Item, Integer> itemCounts = new HashMap<>();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack s = player.getInventory().getItem(slot);
                if (!s.isEmpty()) {
                    itemCounts.merge(s.getItem(), s.getCount(), Integer::sum);
                }
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
                } else {
                    renderBoxSolid(matrix.last().pose(), builder, coordinate, .97f, 1f, .99f, .1f);
                }

                if (renderFree) {
                    renderBoxSolid(matrix.last().pose(), builder, coordinate, .97f, 1f, .99f, .1f);
                }
            }
        }

        matrix.popPose();
        RenderSystem.disableDepthTest();
        buffer.endBatch(); // @mcp: finish (mcp) = draw (yarn)
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

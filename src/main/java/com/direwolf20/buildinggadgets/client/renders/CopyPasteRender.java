package com.direwolf20.buildinggadgets.client.renders;

import com.direwolf20.buildinggadgets.client.renderer.GhostBlockRenderUtil;
import com.direwolf20.buildinggadgets.client.renderer.OurRenderTypes;
import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.tainted.building.PlacementTarget;
import com.direwolf20.buildinggadgets.common.tainted.building.Region;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.building.view.IBuildView;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateProvider;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateProvider.IUpdateListener;
import com.direwolf20.buildinggadgets.common.tainted.template.Template;
import com.direwolf20.buildinggadgets.common.util.TemplateKeyHelper;
import com.direwolf20.buildinggadgets.common.world.MockDelegationWorld;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.*;
import com.mojang.blaze3d.vertex.*;
import com.direwolf20.buildinggadgets.client.events.WorldRenderContextWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;


public class CopyPasteRender extends BaseRenderer implements IUpdateListener {
    private int tickTrack = 0;
    private UUID lastRendered = null;

    @Override
    public void onTemplateUpdate(ITemplateProvider provider, ITemplateKey key, Template template) {
        if (provider.getId(key).equals(lastRendered))
            lastRendered = null;
    }

    @Override
    public void onTemplateUpdateSend(ITemplateProvider provider, ITemplateKey key, Template template) {
        onTemplateUpdate(provider, key, template);
    }

    @Override
    public void renderAfterSetup(WorldRenderContextWrapper context, Player player, ItemStack heldItem) {
        // Remove this method entirely or leave it empty
        // The matrix stack isn't available at this render phase
    }

    @Override
    public void render(WorldRenderContextWrapper context, Player player, ItemStack heldItem) {
        super.render(context, player, heldItem);

        Vec3 cameraView = context.camera().position();
        PoseStack stack = context.matrixStack();

        stack.pushPose();
        stack.translate(-cameraView.x(), -cameraView.y(), -cameraView.z());

        if (GadgetCopyPaste.getToolMode(heldItem) == GadgetCopyPaste.ToolMode.COPY) {
            GadgetCopyPaste.getSelectedRegion(heldItem).ifPresent(region -> {
                renderCopy(stack, region);
            });
        } else {
            renderPaste(stack, cameraView, player, heldItem, context.submitNodeCollector());
        }

        stack.popPose();
    }

    private void renderCopy(PoseStack matrix, Region region) {
        BlockPos startPos = region.getMin();
        BlockPos endPos = region.getMax();
        BlockPos blankPos = BlockPos.ZERO;

        if (startPos.equals(blankPos) || endPos.equals(blankPos)) {
            return;
        }

        int R = 255, G = 223, B = 127;

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        AABB box = new AABB(
                region.getMin().getX(),
                region.getMin().getY(),
                region.getMin().getZ(),
                region.getMax().getX() + 1,
                region.getMax().getY() + 1,
                region.getMax().getZ() + 1
        );

        // Draw AABB box manually - LevelRenderer.renderLineBox API keeps changing
        VertexConsumer lineBuffer = buffer.getBuffer(OurRenderTypes.CopyGadgetLines);
        float r = R / 255f, g = G / 255f, b = B / 255f, a = 1f;
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ;
        double x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        org.joml.Matrix4f m = matrix.last().pose();
        // 12 edges of the box
        float lw = 2.0f;
        lineBuffer.addVertex(m,(float)x0,(float)y0,(float)z0).setColor(r,g,b,a).setNormal(1,0,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y0,(float)z0).setColor(r,g,b,a).setNormal(1,0,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y1,(float)z0).setColor(r,g,b,a).setNormal(1,0,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y1,(float)z0).setColor(r,g,b,a).setNormal(1,0,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y0,(float)z1).setColor(r,g,b,a).setNormal(1,0,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y0,(float)z1).setColor(r,g,b,a).setNormal(1,0,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y1,(float)z1).setColor(r,g,b,a).setNormal(1,0,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y1,(float)z1).setColor(r,g,b,a).setNormal(1,0,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y0,(float)z0).setColor(r,g,b,a).setNormal(0,1,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y1,(float)z0).setColor(r,g,b,a).setNormal(0,1,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y0,(float)z0).setColor(r,g,b,a).setNormal(0,1,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y1,(float)z0).setColor(r,g,b,a).setNormal(0,1,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y0,(float)z1).setColor(r,g,b,a).setNormal(0,1,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y1,(float)z1).setColor(r,g,b,a).setNormal(0,1,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y0,(float)z1).setColor(r,g,b,a).setNormal(0,1,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y1,(float)z1).setColor(r,g,b,a).setNormal(0,1,0).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y0,(float)z0).setColor(r,g,b,a).setNormal(0,0,1).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y0,(float)z1).setColor(r,g,b,a).setNormal(0,0,1).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y0,(float)z0).setColor(r,g,b,a).setNormal(0,0,1).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y0,(float)z1).setColor(r,g,b,a).setNormal(0,0,1).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y1,(float)z0).setColor(r,g,b,a).setNormal(0,0,1).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x0,(float)y1,(float)z1).setColor(r,g,b,a).setNormal(0,0,1).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y1,(float)z0).setColor(r,g,b,a).setNormal(0,0,1).setLineWidth(lw);
        lineBuffer.addVertex(m,(float)x1,(float)y1,(float)z1).setColor(r,g,b,a).setNormal(0,0,1).setLineWidth(lw);
        buffer.endBatch();
    }

    private void renderPaste(PoseStack matrices, Vec3 cameraView, Player player, ItemStack heldItem, SubmitNodeCollector collector) {
        Level world = player.level();

        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(world).ifPresent((ITemplateProvider provider) -> {
            ITemplateKey key = TemplateKeyHelper.getTemplateKey(heldItem);
            if (key != null) {
                GadgetCopyPaste.getActivePos(player, heldItem).ifPresent(startPos -> {
                    MockDelegationWorld fakeWorld = new MockDelegationWorld(world);

                    BuildContext context = BuildContext.builder().player(player).stack(heldItem).build(fakeWorld);

                    IBuildView view = provider.getTemplateForKey(key).createViewInContext(context);
                    view.translateTo(startPos);  // ADD THIS LINE - Move template to paste position!

                    List<PlacementTarget> targets = new ArrayList<>();
                    for (PlacementTarget target : view) {
                        if (target.placeIn(context)) {
                            targets.add(target);
                        }
                    }
                    UUID id = provider.getId(key);
                    if (!id.equals(lastRendered)) {
                        System.gc();
                    }

                    renderTargets(matrices, cameraView, context, targets, startPos, collector);
                    lastRendered = id;
                });
            }
        });
    }

    private void renderTargets(PoseStack matrix, Vec3 projectedView, BuildContext context, List<PlacementTarget> targets, BlockPos startPos, SubmitNodeCollector collector) {
        tickTrack = 0;

        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer ghostConsumer = buffer.getBuffer(OurRenderTypes.GhostBlock);

        // Blue-tinted semi-transparent ghost using camera-relative CPU rendering.
        // The matrix already has translate(-cameraView) applied; we push+translate by targetPos
        // to get camera-relative block position, same coordinate space as renderBoxSolid.
        QuadInstance quadInstance = new QuadInstance();
        quadInstance.setLightCoords(0xF000F0); // full-bright lightmap
        quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        // Pre-build ghost position set to cull shared faces between adjacent ghost blocks.
        Set<BlockPos> ghostSet = new HashSet<>();
        for (PlacementTarget t : targets) ghostSet.add(t.getPos());

        for (PlacementTarget target : targets) {
            BlockPos targetPos = target.getPos();
            BlockState state = context.getWorld().getBlockState(target.getPos());

            matrix.pushPose();
            matrix.translate(targetPos.getX(), targetPos.getY(), targetPos.getZ());

            try {
                if (state.getRenderShape() == RenderShape.MODEL) {
                    BlockStateModel model = modelSet.get(state);
                    List<BlockStateModelPart> parts = new ArrayList<>();
                    // Use a stable seed from the block position to avoid per-frame flickering.
                    model.collectParts(RandomSource.create(targetPos.asLong()), parts);
                    // Cull faces adjacent to solid real-world blocks and adjacent ghost blocks.
                    parts = GhostBlockRenderUtil.cullAgainstNeighbors(parts, context.getWorld(), targetPos, ghostSet);

                    // Render quads with correct biome tint (e.g. grass green, leaf green).
                    // 0x80 = 50% alpha so the preview is semi-transparent.
                    GhostBlockRenderUtil.renderPartsWithTint(
                            ghostConsumer, matrix.last(), parts, state, targetPos, 0x80, quadInstance);
                }
            } catch (Exception e) {
                BuildingGadgets.LOG.trace("Caught exception whilst rendering {}.", state, e);
            }

            matrix.popPose();
        }
        buffer.endBatch();
    }

    @Override
    public boolean isLinkable() {
        return true;
    }
}
package com.direwolf20.buildinggadgets.client.renders;

import com.direwolf20.buildinggadgets.client.renderer.OurRenderTypes;
import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.items.GadgetCutPaste;
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
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client-side renderer for the Cut-Paste Gadget.
 *
 * CUT mode: renders a red outline of the currently selected region.
 * PASTE mode: renders a ghost preview of the template at the paste target position.
 */
public class CutPasteRender extends BaseRenderer implements IUpdateListener {

    private UUID lastRendered = null;

    @Override
    public void onTemplateUpdate(ITemplateProvider provider, ITemplateKey key, Template template) {
        if (provider.getId(key).equals(lastRendered)) {
            lastRendered = null;
        }
    }

    @Override
    public void onTemplateUpdateSend(ITemplateProvider provider, ITemplateKey key, Template template) {
        onTemplateUpdate(provider, key, template);
    }

    @Override
    public void renderAfterSetup(WorldRenderContext context, Player player, ItemStack heldItem) {
        // No special setup needed
    }

    @Override
    public void render(WorldRenderContext context, Player player, ItemStack heldItem) {
        super.render(context, player, heldItem);

        Vec3 cameraView = context.camera().getPosition();
        PoseStack stack = context.matrixStack();

        stack.pushPose();
        stack.translate(-cameraView.x(), -cameraView.y(), -cameraView.z());

        GadgetCutPaste.ToolMode mode = GadgetCutPaste.getToolMode(heldItem);
        if (mode == GadgetCutPaste.ToolMode.CUT) {
            renderCutSelection(stack, player, heldItem);
        } else {
            renderPaste(stack, cameraView, player, heldItem);
        }

        stack.popPose();
    }

    /**
     * Renders the cut region selection:
     * - If only first pos set: renders a single block outline at that position.
     * - If both positions set: renders an AABB outline around the region.
     */
    private void renderCutSelection(PoseStack matrix, Player player, ItemStack heldItem) {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        BlockPos lower = GadgetCutPaste.getLowerRegionBound(heldItem);
        BlockPos upper = GadgetCutPaste.getUpperRegionBound(heldItem);

        if (lower != null && upper != null) {
            // Full region selected — draw outline box in red
            AABB box = new AABB(
                    Math.min(lower.getX(), upper.getX()),
                    Math.min(lower.getY(), upper.getY()),
                    Math.min(lower.getZ(), upper.getZ()),
                    Math.max(lower.getX(), upper.getX()) + 1,
                    Math.max(lower.getY(), upper.getY()) + 1,
                    Math.max(lower.getZ(), upper.getZ()) + 1
            );
            LevelRenderer.renderLineBox(matrix, buffer.getBuffer(OurRenderTypes.CopyGadgetLines),
                    box, 1f, 0.2f, 0.2f, 1f);
        } else if (lower != null) {
            // First pos only — draw single block outline in orange
            AABB box = new AABB(lower.getX(), lower.getY(), lower.getZ(),
                    lower.getX() + 1, lower.getY() + 1, lower.getZ() + 1);
            LevelRenderer.renderLineBox(matrix, buffer.getBuffer(OurRenderTypes.CopyGadgetLines),
                    box, 1f, 0.6f, 0f, 1f);
        }

        buffer.endBatch();
    }

    /**
     * Renders the ghost paste preview — same logic as CopyPasteRender.renderPaste.
     */
    private void renderPaste(PoseStack matrices, Vec3 cameraView, Player player, ItemStack heldItem) {
        Level world = player.level();

        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(world).ifPresent((ITemplateProvider provider) -> {
            ITemplateKey key = TemplateKeyHelper.getTemplateKey(heldItem);
            if (key != null) {
                GadgetCutPaste.getActivePos(player, heldItem).ifPresent(startPos -> {
                    MockDelegationWorld fakeWorld = new MockDelegationWorld(world);
                    BuildContext context = BuildContext.builder().player(player).stack(heldItem).build(fakeWorld);
                    IBuildView view = provider.getTemplateForKey(key).createViewInContext(context);
                    view.translateTo(startPos);

                    List<PlacementTarget> targets = new ArrayList<>();
                    for (PlacementTarget target : view) {
                        if (target.placeIn(context)) {
                            targets.add(target);
                        }
                    }

                    UUID id = provider.getId(key);
                    lastRendered = id;
                    renderTargets(matrices, context, targets);
                });
            }
        });
    }

    private void renderTargets(PoseStack matrix, BuildContext context, List<PlacementTarget> targets) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        OurRenderTypes.MultiplyAlphaRenderTypeBuffer mutatedBuffer =
                new OurRenderTypes.MultiplyAlphaRenderTypeBuffer(bufferSource, 0.7f);

        BlockRenderDispatcher dispatcher = getMc().getBlockRenderer();

        for (PlacementTarget target : targets) {
            BlockPos targetPos = target.getPos();
            BlockState state = context.getWorld().getBlockState(target.getPos());

            matrix.pushPose();
            matrix.translate(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            try {
                if (state.getRenderShape() == RenderShape.MODEL) {
                    dispatcher.renderSingleBlock(state, matrix, mutatedBuffer,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                }
            } catch (Exception e) {
                BuildingGadgets.LOG.trace("Caught exception whilst rendering {}.", state, e);
            }
            matrix.popPose();
        }

        bufferSource.endBatch();
    }

    @Override
    public boolean isLinkable() {
        return true;
    }
}


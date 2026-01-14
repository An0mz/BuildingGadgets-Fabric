package com.direwolf20.buildinggadgets.client.renders;

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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
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

import java.io.Closeable;
import java.util.*;
import java.util.function.Consumer;


public class CopyPasteRender extends BaseRenderer implements IUpdateListener {
    private MultiVBORenderer renderBuffer;
    private int tickTrack = 0;
    private UUID lastRendered = null;
    private ShaderInstance instance;

    @Override
    public void onTemplateUpdate(ITemplateProvider provider, ITemplateKey key, Template template) {
        if (provider.getId(key).equals(lastRendered))
            renderBuffer = null;
    }

    @Override
    public void onTemplateUpdateSend(ITemplateProvider provider, ITemplateKey key, Template template) {
        onTemplateUpdate(provider, key, template);
    }

    @Override
    public void renderAfterSetup(WorldRenderContext context, Player player, ItemStack heldItem) {
        // Remove this method entirely or leave it empty
        // The matrix stack isn't available at this render phase
    }

    @Override
    public void render(WorldRenderContext context, Player player, ItemStack heldItem) {
        super.render(context, player, heldItem);

        Vec3 cameraView = context.camera().getPosition();
        PoseStack stack = context.matrixStack();

        stack.pushPose();
        stack.translate(-cameraView.x(), -cameraView.y(), -cameraView.z());

        if (GadgetCopyPaste.getToolMode(heldItem) == GadgetCopyPaste.ToolMode.COPY) {
            GadgetCopyPaste.getSelectedRegion(heldItem).ifPresent(region -> {
                renderCopy(stack, region);
            });
        } else {
            renderPaste(stack, cameraView, player, heldItem);
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

        LevelRenderer.renderLineBox(matrix, buffer.getBuffer(OurRenderTypes.CopyGadgetLines), box, R / 255f, G / 255f, B / 255f, 1f);
        buffer.endBatch();
    }

    private void renderPaste(PoseStack matrices, Vec3 cameraView, Player player, ItemStack heldItem) {
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
                        renderBuffer = null;
                        System.gc();
                    }

                    renderTargets(matrices, cameraView, context, targets, startPos);
                    lastRendered = id;
                });
            }
        });
    }

    private void renderTargets(PoseStack matrix, Vec3 projectedView, BuildContext context, List<PlacementTarget> targets, BlockPos startPos) {
        tickTrack = 0;

        if (renderBuffer != null) {
            renderBuffer.close();
            renderBuffer = null;
        }

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        OurRenderTypes.MultiplyAlphaRenderTypeBuffer mutatedBuffer = new OurRenderTypes.MultiplyAlphaRenderTypeBuffer(
                bufferSource, .7f);

        BlockRenderDispatcher dispatcher = getMc().getBlockRenderer();

        for (PlacementTarget target : targets) {
            BlockPos targetPos = target.getPos();
            BlockState state = context.getWorld().getBlockState(target.getPos());

            matrix.pushPose();
            matrix.translate(targetPos.getX(), targetPos.getY(), targetPos.getZ());

            try {
                if (state.getRenderShape() == RenderShape.MODEL) {
                    dispatcher.renderSingleBlock(state, matrix, mutatedBuffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
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

    /**
     * Vertex Buffer Object for caching the render. Pretty similar to how the chunk caching works
     */
    public static class MultiVBORenderer implements Closeable {
        private static final int BUFFER_SIZE = 2 * 1024 * 1024 * 3;

        public static MultiVBORenderer of(Consumer<MultiBufferSource> vertexProducer) {
            final Map<RenderType, BufferBuilder> builders = Maps.newHashMap();

            vertexProducer.accept(rt -> builders.computeIfAbsent(rt, (_rt) -> {
                BufferBuilder builder = new BufferBuilder(BUFFER_SIZE);
                builder.begin(_rt.mode(), _rt.format());

                return builder;
            }));

            Map<RenderType, VertexBuffer> buffers = Maps.transformEntries(builders, (rt, builder) -> {
                Objects.requireNonNull(rt);
                Objects.requireNonNull(builder);

                VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vbo.bind();
                vbo.upload(builder.end());
                return vbo;
            });

            return new MultiVBORenderer(buffers);
        }

        private final ImmutableMap<RenderType, VertexBuffer> buffers;

        protected MultiVBORenderer(Map<RenderType, VertexBuffer> buffers) {
            this.buffers = ImmutableMap.copyOf(buffers);
        }

        //TODO: Sort verts
        public void sort(float x, float y, float z) {
            // Dire the fucking depth buffer. WHAT THE FUCK
            // Fuck you for putting me through this pain


//            for (Map.Entry<RenderType, DireBufferBuilder.State> kv : sortCaches.entrySet()) {
//                RenderType rt = kv.getKey();
//                DireBufferBuilder.State state = kv.getValue();
//                DireBufferBuilder builder = new DireBufferBuilder(BUFFER_SIZE);
//                builder.begin(rt.mode().asGLMode, rt.format());
//                builder.setVertexState(state);
//                builder.sortVertexData(x, y, z);
//                builder.finishDrawing();
//
//                DireVertexBuffer vbo = buffers.get(rt);
//                vbo.upload(builder);
//            }
        }

        public void render(Matrix4f modelViewMatrix) {
            buffers.forEach((rt, vbo) -> {

                rt.setupRenderState();
                vbo.bind();
                vbo.drawWithShader(modelViewMatrix, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                rt.clearRenderState();
            });
        }

        @Override
        public void close() {
            for (VertexBuffer value : buffers.values()) {
                value.close();
            }
        }
    }
}

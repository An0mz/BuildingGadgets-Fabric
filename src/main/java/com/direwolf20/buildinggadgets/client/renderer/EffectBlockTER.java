package com.direwolf20.buildinggadgets.client.renderer;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.blocks.EffectBlock;
import com.direwolf20.buildinggadgets.common.blocks.OurBlocks;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tileentities.EffectBlockTileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EffectBlockTER implements BlockEntityRenderer<EffectBlockTileEntity, EffectBlockTER.RenderState> {

    public static class RenderState extends BlockEntityRenderState {
        public BlockData renderedBlock;
        public EffectBlock.Mode mode;
        public int ticksExisted;
        public int maxLife;
        // which faces to render (not adjacent to another EffectBlock): down, up, north, south, east, west
        public boolean renderDown, renderUp, renderNorth, renderSouth, renderEast, renderWest;
    }

    public EffectBlockTER(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(EffectBlockTileEntity tile, RenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        // MUST populate blockEntityType (and blockPos, lightCoords, etc.) via extractBase,
        // otherwise BlockEntityRenderDispatcher.submit() looks up the renderer by state.blockEntityType
        // which would be null, causing the renderer to be silently skipped.
        BlockEntityRenderState.extractBase(tile, state, crumbling);
        state.renderedBlock = tile.getRenderedBlock();
        state.mode = tile.getReplacementMode();
        state.ticksExisted = tile.getTicksExisted();
        state.maxLife = tile.getLifespan();

        Level level = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (level != null) {
            state.renderDown  = level.getBlockState(pos.below()).getBlock()  != OurBlocks.EFFECT_BLOCK;
            state.renderUp    = level.getBlockState(pos.above()).getBlock()  != OurBlocks.EFFECT_BLOCK;
            state.renderNorth = level.getBlockState(pos.north()).getBlock()  != OurBlocks.EFFECT_BLOCK;
            state.renderSouth = level.getBlockState(pos.south()).getBlock()  != OurBlocks.EFFECT_BLOCK;
            state.renderEast  = level.getBlockState(pos.east()).getBlock()   != OurBlocks.EFFECT_BLOCK;
            state.renderWest  = level.getBlockState(pos.west()).getBlock()   != OurBlocks.EFFECT_BLOCK;
        } else {
            state.renderDown = state.renderUp = state.renderNorth =
            state.renderSouth = state.renderEast = state.renderWest = true;
        }
    }

    @Override
    public void submit(RenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        BlockData renderData = state.renderedBlock;
        if (renderData == null)
            return;

        EffectBlock.Mode toolMode = state.mode;

        int teCounter = state.ticksExisted;
        int maxLife = state.maxLife;
        teCounter = Math.min(teCounter, maxLife);

        float scale = (float) teCounter / (float) maxLife;
        if (scale >= 1.0f)
            scale = 0.99f;
        if (toolMode == EffectBlock.Mode.REMOVE || toolMode == EffectBlock.Mode.REPLACE)
            scale = (float) (maxLife - teCounter) / maxLife;

        float trans = (1 - scale) / 2;

        stack.pushPose();
        stack.translate(trans, trans, trans);
        stack.scale(scale, scale, scale);

        BlockState renderBlockState = renderData.getState();

        // --- Ghost block rendering via submitCustomGeometry ---
        // Collect block model parts (using a fresh random for variant selection)
        try {
            BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
            BlockStateModel model = dispatcher.getBlockModel(renderBlockState);
            List<BlockModelPart> parts = model.collectParts(RandomSource.create());

            collector.submitCustomGeometry(stack, OurRenderTypes.RenderBlock, (pose, consumer) -> {
                for (BlockModelPart part : parts) {
                    // Quads not belonging to any specific face
                    for (BakedQuad quad : part.getQuads(null)) {
                        consumer.putBulkData(pose, quad, 1f, 1f, 1f, 0.55f,
                                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    }
                    // Quads per face direction
                    for (Direction dir : Direction.values()) {
                        for (BakedQuad quad : part.getQuads(dir)) {
                            consumer.putBulkData(pose, quad, 1f, 1f, 1f, 0.55f,
                                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                        }
                    }
                }
            });
        } catch (Exception e) {
            BuildingGadgets.LOG.error("Failed to render effect block: {}", e.getMessage());
        }

        // --- Colored overlay box rendering ---
        float red = 0f, green = 1f, blue = 1f;
        if (toolMode == EffectBlock.Mode.REMOVE || toolMode == EffectBlock.Mode.REPLACE) {
            red = 1f;
            green = 0.25f;
            blue = 0.25f;
        }

        float alpha = (1f - scale);
        if (alpha < 0.051f) alpha = 0.051f;
        if (alpha > 0.33f)  alpha = 0.33f;

        final float fr = red, fg = green, fb = blue, fa = alpha;
        final boolean rd = state.renderDown,  ru = state.renderUp;
        final boolean rn = state.renderNorth, rs = state.renderSouth;
        final boolean re = state.renderEast,  rw = state.renderWest;

        collector.submitCustomGeometry(stack, OurRenderTypes.MissingBlockOverlay, (pose, builder) -> {
            Matrix4f matrix = pose.pose();
            float x = 0, y = 0, z = 0, maxX = 1, maxY = 1, maxZ = 1;

            if (rd) {
                builder.addVertex(matrix, x,    y, z   ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, y, z   ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, y, maxZ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, x,    y, maxZ).setColor(fr, fg, fb, fa);
            }
            if (ru) {
                builder.addVertex(matrix, x,    maxY, z   ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, x,    maxY, maxZ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, maxY, maxZ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, maxY, z   ).setColor(fr, fg, fb, fa);
            }
            if (rn) {
                builder.addVertex(matrix, x,    y,    z).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, x,    maxY, z).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, maxY, z).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, y,    z).setColor(fr, fg, fb, fa);
            }
            if (rs) {
                builder.addVertex(matrix, x,    y,    maxZ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, y,    maxZ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, maxY, maxZ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, x,    maxY, maxZ).setColor(fr, fg, fb, fa);
            }
            if (re) {
                builder.addVertex(matrix, maxX, y,    z   ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, maxY, z   ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, maxY, maxZ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, maxX, y,    maxZ).setColor(fr, fg, fb, fa);
            }
            if (rw) {
                builder.addVertex(matrix, x, y,    z   ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, x, y,    maxZ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, x, maxY, maxZ).setColor(fr, fg, fb, fa);
                builder.addVertex(matrix, x, maxY, z   ).setColor(fr, fg, fb, fa);
            }
        });

        stack.popPose();
    }
}
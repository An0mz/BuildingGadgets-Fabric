package com.direwolf20.buildinggadgets.client.renderer;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.blocks.EffectBlock;
import com.direwolf20.buildinggadgets.common.blocks.OurBlocks;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tileentities.EffectBlockTileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class EffectBlockTER implements BlockEntityRenderer<EffectBlockTileEntity> {

    public BlockRenderDispatcher dispatcher;

    public EffectBlockTER(BlockEntityRendererProvider.Context ctx) {
        dispatcher = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(EffectBlockTileEntity tile, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {
        BlockData renderData = tile.getRenderedBlock();
        if (renderData == null)
            return;
        VertexConsumer builder;

        MultiBufferSource.BufferSource buffer2 = Minecraft.getInstance().renderBuffers().bufferSource();
        EffectBlock.Mode toolMode = tile.getReplacementMode();

        int teCounter = tile.getTicksExisted();
        int maxLife = tile.getLifespan();
        teCounter = Math.min(teCounter, maxLife);

        float scale = (float) (teCounter) / (float) maxLife;
        if (scale >= 1.0f)
            scale = 0.99f;
        if (toolMode == EffectBlock.Mode.REMOVE || toolMode == EffectBlock.Mode.REPLACE)
            scale = (float) (maxLife - teCounter) / maxLife;

        float trans = (1 - scale) / 2;

        stack.pushPose();
        stack.translate(trans, trans, trans);
        stack.scale(scale, scale, scale);

        BlockState renderBlockState = renderData.getState();
        FluidState fluidState = renderBlockState.getFluidState();

        if (!fluidState.isEmpty()) {
            // Fluid blocks (water, lava) have RenderShape.INVISIBLE — renderSingleBlock skips them.
            // Render a filled translucent colored box so the fluid visually shrinks/grows.
            float fr, fg, fb;
            if (fluidState.getType() == Fluids.WATER || fluidState.getType() == Fluids.FLOWING_WATER) {
                fr = 0.24f; fg = 0.46f; fb = 1.0f; // water blue
            } else {
                fr = 1.0f; fg = 0.40f; fb = 0.0f; // lava orange
            }
            float fa = 0.65f;
            VertexConsumer fc = buffer.getBuffer(OurRenderTypes.MissingBlockOverlay);
            Matrix4f m = stack.last().pose();
            float x0 = 0, y0 = 0, z0 = 0, x1 = 1, y1 = 1, z1 = 1;
            // Down
            fc.vertex(m, x0, y0, z0).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y0, z0).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y0, z1).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x0, y0, z1).color(fr, fg, fb, fa).endVertex();
            // Up
            fc.vertex(m, x0, y1, z0).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x0, y1, z1).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y1, z1).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y1, z0).color(fr, fg, fb, fa).endVertex();
            // North
            fc.vertex(m, x0, y0, z0).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x0, y1, z0).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y1, z0).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y0, z0).color(fr, fg, fb, fa).endVertex();
            // South
            fc.vertex(m, x0, y0, z1).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y0, z1).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y1, z1).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x0, y1, z1).color(fr, fg, fb, fa).endVertex();
            // East
            fc.vertex(m, x1, y0, z0).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y1, z0).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y1, z1).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x1, y0, z1).color(fr, fg, fb, fa).endVertex();
            // West
            fc.vertex(m, x0, y0, z0).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x0, y0, z1).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x0, y1, z1).color(fr, fg, fb, fa).endVertex();
            fc.vertex(m, x0, y1, z0).color(fr, fg, fb, fa).endVertex();
        } else {
            OurRenderTypes.MultiplyAlphaRenderTypeBuffer mutatedBuffer = new OurRenderTypes.MultiplyAlphaRenderTypeBuffer(Minecraft.getInstance().renderBuffers().bufferSource(), .55f);
            try {
                dispatcher.renderSingleBlock(
                        renderBlockState, stack, mutatedBuffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                );
            } catch (Exception ignored) {
                BuildingGadgets.LOG.error("Failed to render block.");
            }
        }

        stack.popPose();
        stack.pushPose();

        builder = buffer.getBuffer(OurRenderTypes.MissingBlockOverlay);

        float x = 0,
                y = 0,
                z = 0,
                maxX = 1,
                maxY = 1,
                maxZ = 1,
                red = 0f,
                green = 1f,
                blue = 1f;

        if (toolMode == EffectBlock.Mode.REMOVE || toolMode == EffectBlock.Mode.REPLACE) {
            red = 1f;
            green = 0.25f;
            blue = 0.25f;
        }

        float alpha = (1f - (scale));
        if (alpha < 0.051f)
            alpha = 0.051f;

        if (alpha > 0.33f)
            alpha = 0.33f;

        Matrix4f matrix = stack.last().pose();

        // Down
        if (tile.getLevel().getBlockState(tile.getBlockPos().below()).getBlock() != OurBlocks.EFFECT_BLOCK) {
            builder.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, y, z).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, y, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, x, y, maxZ).color(red, green, blue, alpha).endVertex();
        }
        // Up
        if (tile.getLevel().getBlockState(tile.getBlockPos().above()).getBlock() != OurBlocks.EFFECT_BLOCK) {
            builder.vertex(matrix, x, maxY, z).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, x, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, z).color(red, green, blue, alpha).endVertex();
        }
        // North
        if (tile.getLevel().getBlockState(tile.getBlockPos().north()).getBlock() != OurBlocks.EFFECT_BLOCK) {
            builder.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, x, maxY, z).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, z).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, y, z).color(red, green, blue, alpha).endVertex();
        }
        // South
        if (tile.getLevel().getBlockState(tile.getBlockPos().south()).getBlock() != OurBlocks.EFFECT_BLOCK) {
            builder.vertex(matrix, x, y, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, y, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, x, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        }
        // East
        if (tile.getLevel().getBlockState(tile.getBlockPos().east()).getBlock() != OurBlocks.EFFECT_BLOCK) {
            builder.vertex(matrix, maxX, y, z).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, z).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, y, maxZ).color(red, green, blue, alpha).endVertex();
        }
        // West
        if (tile.getLevel().getBlockState(tile.getBlockPos().west()).getBlock() != OurBlocks.EFFECT_BLOCK) {
            builder.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, x, y, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, x, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, x, maxY, z).color(red, green, blue, alpha).endVertex();
        }
        stack.popPose();
        buffer2.endBatch(); // @mcp: draw (yarn) = finish (mcp)
    }
}

package com.direwolf20.buildinggadgets.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class OurRenderTypes {

    public static final RenderType RenderBlock          = RenderTypes.tripwireMovingBlock();
    public static final RenderType MissingBlockOverlay  = RenderTypes.lightning();
    public static final RenderType CopyGadgetLines      = RenderTypes.lines();
    public static final RenderType CopyPasteRenderBlock = RenderTypes.tripwireMovingBlock();
    public static final RenderType BlockOverlay         = RenderTypes.lightning();

    /**
     * Wraps a MultiBufferSource so that:
     * 1. All render types are replaced with RenderBlock (tripwire = translucent no-cull),
     *    giving the ghost blocks their characteristic tinted look.
     * 2. Alpha is multiplied by constantAlpha (55%) for transparency.
     */
    public static class MultiplyAlphaRenderTypeBuffer implements MultiBufferSource {
        private final MultiBufferSource inner;
        private final float constantAlpha;

        public MultiplyAlphaRenderTypeBuffer(MultiBufferSource inner, float constantAlpha) {
            this.inner = inner;
            this.constantAlpha = constantAlpha;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            // Force all block render types through RenderBlock (tripwire pipeline).
            // This gives the translucent tinted appearance AND works with the block
            // vertex format (position, UV, lightmap, overlay, normal).
            return new MultiplyAlphaVertexBuilder(inner.getBuffer(RenderBlock), constantAlpha);
        }

        public static class MultiplyAlphaVertexBuilder implements VertexConsumer {
            private final VertexConsumer inner;
            private final float constantAlpha;

            public MultiplyAlphaVertexBuilder(VertexConsumer inner, float constantAlpha) {
                this.inner = inner;
                this.constantAlpha = constantAlpha;
            }

            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                return new MultiplyAlphaVertexBuilder(inner.addVertex(x, y, z), constantAlpha);
            }

            @Override
            public VertexConsumer setColor(int red, int green, int blue, int alpha) {
                inner.setColor(red, green, blue, (int)(alpha * constantAlpha));
                return this;
            }

            @Override
            public VertexConsumer setColor(int argb) {
                int a = (int)(((argb >> 24) & 0xFF) * constantAlpha);
                inner.setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, a);
                return this;
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                inner.setUv(u, v);
                return this;
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                inner.setUv1(u, v);
                return this;
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                inner.setUv2(u, v);
                return this;
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                inner.setNormal(x, y, z);
                return this;
            }

            @Override
            public VertexConsumer setLineWidth(float lineWidth) {
                inner.setLineWidth(lineWidth);
                return this;
            }
        }
    }
}
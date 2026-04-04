package com.direwolf20.buildinggadgets.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.OptionalDouble;


public class OurRenderTypes extends RenderType {

    private static final LineStateShard THICK_LINES = new LineStateShard(OptionalDouble.of(3.0D));

    public static final RenderType RenderBlock = create("GadgetRenderBlock",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_SOLID_SHADER)
                    .setLightmapState(LIGHTMAP)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    public static final RenderType MissingBlockOverlay = create("GadgetMissingBlockOverlay",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    public static final RenderType CopyGadgetLines = create("GadgetCopyLines",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.LINES, 256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(2.0D)))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(NO_TEXTURE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    public static final RenderType CopyPasteRenderBlock = create("CopyPasteRenderBlock",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_SOLID_SHADER)
                    .setLightmapState(LIGHTMAP)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    public static final RenderType BlockOverlay = create("BGBlockOverlay",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setTextureState(NO_TEXTURE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    public OurRenderTypes(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
        super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
    }

    /**
     * Wraps a MultiBufferSource to multiply alpha on all vertex colors.
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
            RenderType localType = type;
            if (localType instanceof CompositeRenderType) {
                ResourceLocation texture = ((TextureStateShard) ((CompositeRenderType) localType).state.textureState).texture
                        .orElse(InventoryMenu.BLOCK_ATLAS);
                localType = entityTranslucentCull(texture);
            } else if (localType.toString().equals(Sheets.translucentCullBlockSheet().toString())) {
                localType = Sheets.translucentCullBlockSheet();
            }
            return new MultiplyAlphaVertexBuilder(inner.getBuffer(localType), this.constantAlpha);
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
                return inner.setColor(red, green, blue, (int) (alpha * constantAlpha));
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                return inner.setUv(u, v);
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                return inner.setUv1(u, v);
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                return inner.setUv2(u, v);
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                return inner.setNormal(x, y, z);
            }
        }
    }
}

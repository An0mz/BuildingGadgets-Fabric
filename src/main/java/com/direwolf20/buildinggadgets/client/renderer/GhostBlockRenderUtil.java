package com.direwolf20.buildinggadgets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Utility for rendering ghost/preview blocks without Z-fighting against adjacent
 * solid real-world blocks.
 *
 * <p>The MC 26.1 {@code submitBlockModel} API does not accept a world context, so it
 * renders every face of every part (null + all 6 directions).  When a ghost block sits
 * adjacent to a real solid block the coincident faces Z-fight.  We fix this by wrapping
 * each {@link BlockStateModelPart} so that {@code getQuads(Direction)} returns an empty
 * list for any direction where the real world has a solid block face.
 */
public final class GhostBlockRenderUtil {

    private GhostBlockRenderUtil() {}

    /**
     * Returns a copy of {@code parts} where each part has its direction-specific quads
     * filtered out for faces adjacent to a solid block in {@code world}.
     *
     * @param parts the parts collected from {@code BlockStateModel.collectParts}
     * @param world the real (or mock) world used to query neighbours
     * @param pos   the position the ghost block occupies
     * @return filtered parts, or the original list when no culling is needed
     */
    public static List<BlockStateModelPart> cullAgainstNeighbors(
            List<BlockStateModelPart> parts,
            BlockGetter world,
            BlockPos pos) {
        return cullAgainstNeighbors(parts, world, pos, Collections.emptySet());
    }

    /**
     * Returns a copy of {@code parts} where each part has its direction-specific quads
     * filtered out for faces adjacent to either a solid real-world block or another ghost block.
     * Culling shared faces between adjacent ghost blocks eliminates Z-fighting artifacts.
     *
     * @param parts             the parts collected from {@code BlockStateModel.collectParts}
     * @param world             the real (or mock) world used to query neighbours
     * @param pos               the position the ghost block occupies
     * @param otherGhostPositions the set of positions of all other ghost blocks being rendered
     * @return filtered parts, or the original list when no culling is needed
     */
    public static List<BlockStateModelPart> cullAgainstNeighbors(
            List<BlockStateModelPart> parts,
            BlockGetter world,
            BlockPos pos,
            Set<BlockPos> otherGhostPositions) {

        Set<Direction> toHide = EnumSet.noneOf(Direction.class);
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = world.getBlockState(neighborPos);
            // isSolidRender() is true when the block completely fills its cube on all faces,
            // which is exactly when the adjacent ghost face would be 100% occluded.
            // Also hide faces shared with other ghost blocks to eliminate Z-fighting.
            if (neighborState.isSolidRender() || otherGhostPositions.contains(neighborPos)) {
                toHide.add(dir);
            }
        }

        if (toHide.isEmpty()) {
            return parts;
        }

        List<BlockStateModelPart> filtered = new ArrayList<>(parts.size());
        for (BlockStateModelPart part : parts) {
            filtered.add(new CullFilteredPart(part, toHide));
        }
        return filtered;
    }

    // -----------------------------------------------------------------------

    /**
     * Renders all quads from the given (already-culled) parts into {@code consumer},
     * applying the correct biome tint color for tinted quads (e.g. grass, leaves, water)
     * so the preview shows the natural block colour rather than a gray/blue tint.
     *
     * @param consumer   the vertex consumer for the ghost render type
     * @param pose       the current camera-relative pose
     * @param parts      pre-culled parts from {@code BlockStateModel.collectParts}
     * @param blockState the block state being previewed
     * @param pos        the world position of the ghost block (used for biome colour lookup)
     * @param alpha      0–255 alpha value for the ghost transparency
     * @param qi         a reusable QuadInstance (its color is overwritten per quad)
     */
    public static void renderPartsWithTint(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            List<BlockStateModelPart> parts,
            BlockState blockState,
            BlockPos pos,
            int alpha,
            QuadInstance qi) {

        Minecraft mc = Minecraft.getInstance();
        ClientLevel clientLevel = mc.level;
        BlockColors blockColors = mc.getBlockColors();

        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(null)) {
                setQuadColor(qi, quad, blockState, clientLevel, pos, alpha, blockColors);
                consumer.putBakedQuad(pose, quad, qi);
            }
            for (Direction dir : Direction.values()) {
                for (BakedQuad quad : part.getQuads(dir)) {
                    setQuadColor(qi, quad, blockState, clientLevel, pos, alpha, blockColors);
                    consumer.putBakedQuad(pose, quad, qi);
                }
            }
        }
    }

    /**
     * Sets the color on {@code qi} for the given quad.
     * Tinted quads (grass top, leaves, water…) receive the biome-appropriate RGB color
     * multiplied in so the texture shows its correct hue. Non-tinted quads get white
     * (full texture color).  Alpha is always the {@code alpha} parameter (0–255).
     */
    private static void setQuadColor(
            QuadInstance qi,
            BakedQuad quad,
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            int alpha,
            BlockColors blockColors) {

        BakedQuad.MaterialInfo mat = quad.materialInfo();
        int rgb;
        if (mat.isTinted() && level != null) {
            BlockTintSource tintSource = blockColors.getTintSource(state, mat.tintIndex());
            rgb = tintSource.colorInWorld(state, level, pos) & 0xFFFFFF;
        } else {
            rgb = 0xFFFFFF;
        }
        qi.setColor((alpha << 24) | rgb);
    }

    // -----------------------------------------------------------------------

    private static final class CullFilteredPart implements BlockStateModelPart {
        private final BlockStateModelPart inner;
        private final Set<Direction> culledFaces;

        CullFilteredPart(BlockStateModelPart inner, Set<Direction> culledFaces) {
            this.inner = inner;
            this.culledFaces = culledFaces;
        }

        @Override
        public List<BakedQuad> getQuads(Direction dir) {
            // dir == null  →  "always render" quads; never cull these.
            if (dir != null && culledFaces.contains(dir)) {
                return List.of();
            }
            return inner.getQuads(dir);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return inner.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return inner.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return inner.materialFlags();
        }
    }
}


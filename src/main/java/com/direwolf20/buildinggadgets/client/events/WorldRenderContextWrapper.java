package com.direwolf20.buildinggadgets.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

/**
 * A replacement for the removed Fabric API WorldRenderContext, providing the
 * camera, a PoseStack, and a SubmitNodeCollector for world-space rendering.
 */
public final class WorldRenderContextWrapper {
    private final Camera camera;
    private final PoseStack matrixStack;
    private final SubmitNodeCollector submitNodeCollector;

    public WorldRenderContextWrapper(Camera camera, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector) {
        this.camera = camera;
        this.matrixStack = matrixStack;
        this.submitNodeCollector = submitNodeCollector;
    }

    public Camera camera() {
        return camera;
    }

    public PoseStack matrixStack() {
        return matrixStack;
    }

    public SubmitNodeCollector submitNodeCollector() {
        return submitNodeCollector;
    }

    public Vec3 cameraPos() {
        return camera.position();
    }
}


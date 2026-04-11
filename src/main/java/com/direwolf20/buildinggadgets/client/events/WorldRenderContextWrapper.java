package com.direwolf20.buildinggadgets.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/**
 * A replacement for the removed Fabric API WorldRenderContext, providing the
 * camera and a PoseStack for world-space rendering.
 */
public final class WorldRenderContextWrapper {
    private final Camera camera;
    private final PoseStack matrixStack;

    public WorldRenderContextWrapper(Camera camera, PoseStack matrixStack) {
        this.camera = camera;
        this.matrixStack = matrixStack;
    }

    public Camera camera() {
        return camera;
    }

    public PoseStack matrixStack() {
        return matrixStack;
    }

    public Vec3 cameraPos() {
        return camera.getPosition();
    }
}


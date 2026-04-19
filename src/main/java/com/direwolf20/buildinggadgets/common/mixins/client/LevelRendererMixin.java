package com.direwolf20.buildinggadgets.common.mixins.client;

import com.direwolf20.buildinggadgets.client.events.EventRenderWorldLast;
import com.direwolf20.buildinggadgets.client.events.WorldRenderContextWrapper;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.LevelRenderer;

/**
 * Adds a custom frame-graph pass that fires after the "late_debug" pass so the
 * camera UBO is still bound when bufferSource.endBatch() is called, matching
 * what the (now-removed) Fabric API WorldRenderEvents.END_MAIN event used to do.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow private RenderBuffers renderBuffers;
    @Final @Shadow private LevelTargetBundle targets;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V"
            )
    )
    private void buildingGadgets$addRenderPass(
            GraphicsResourceAllocator graphicsResourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            Matrix4f cullingProjectionMatrix,
            GpuBufferSlice shaderFog,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci,
            @Local FrameGraphBuilder frameGraphBuilder
    ) {
        FramePass framePass = frameGraphBuilder.addPass("buildinggadgets_render");
        this.targets.main = framePass.readsAndWrites(this.targets.main);
        ResourceHandle<RenderTarget> mainHandle = this.targets.main;

        RenderBuffers rb = this.renderBuffers;

        framePass.executes(() -> {
            RenderSystem.setShaderFog(shaderFog);
            PoseStack poseStack = new PoseStack();
            MultiBufferSource.BufferSource bufferSource = rb.bufferSource();

            RenderSystem.outputColorTextureOverride = mainHandle.get().getColorTextureView();
            RenderSystem.outputDepthTextureOverride = mainHandle.get().getDepthTextureView();

            WorldRenderContextWrapper ctx = new WorldRenderContextWrapper(camera, poseStack);
            EventRenderWorldLast.renderAfterSetup(ctx);
            EventRenderWorldLast.renderWorldLastEvent(ctx);
            bufferSource.endBatch();

            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
        });
    }
}

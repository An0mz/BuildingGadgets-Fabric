package com.direwolf20.buildinggadgets.common.mixins.client;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Kept as a stub. World rendering events are now handled via
 * Fabric API WorldRenderEvents.END_MAIN registered in BuildingGadgetsClient.
 *
 * The old TAIL-of-renderLevel approach was broken in MC 1.21+ because
 * renderLevel builds a FrameGraph and schedules GPU passes (including
 * method_62214 which holds the active camera UBO). By the time renderLevel's
 * TAIL executes, the frame graph has already finished and the camera uniform
 * is no longer bound, so bufferSource.endBatch() used an incorrect transform,
 * causing the preview to float in the wrong world-space position.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
}


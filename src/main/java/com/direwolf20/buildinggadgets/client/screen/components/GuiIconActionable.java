package com.direwolf20.buildinggadgets.client.screen.components;

import com.direwolf20.buildinggadgets.client.OurSounds;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.util.function.Predicate;

/**
 * A one stop shop for all your icon gui related needs. We support colors,
 * icons, selected and deselected states, sound and loads more. Come on
 * down!
 */
public class GuiIconActionable extends Button {
    private final Predicate<Boolean> action;
    private boolean selected;
    private final boolean isSelectable;

    private final Color selectedColor = Color.GREEN;
    private final Color deselectedColor = new Color(255, 255, 255);
    private Color activeColor;

    private final ResourceLocation selectedTexture;
    private final ResourceLocation deselectedTexture;

    public GuiIconActionable(int x, int y, String texture, Component message, boolean isSelectable, Predicate<Boolean> action) {
        super(x, y, 25, 25, message, (b) -> {
        }, Button.DEFAULT_NARRATION);
        this.activeColor = deselectedColor;
        this.isSelectable = isSelectable;
        this.action = action;

        this.setSelected(action.test(false));

        // Set the selected and deselected textures.
        String assetLocation = "textures/gui/setting/%s.png";

        this.deselectedTexture = ResourceLocation.fromNamespaceAndPath(Reference.MODID, String.format(assetLocation, texture));
        this.selectedTexture = !isSelectable ? this.deselectedTexture : ResourceLocation.fromNamespaceAndPath(Reference.MODID, String.format(assetLocation, texture + "_selected"));
    }

    /**
     * If yo do not need to be able to select / toggle something then use this constructor as
     * you'll hit missing texture issues if you don't have an active (_selected) texture.
     */
    public GuiIconActionable(int x, int y, String texture, Component message, Predicate<Boolean> action) {
        this(x, y, texture, message, false, action);
    }

    public void setFaded(boolean faded) {
        alpha = faded ? .6f : 1f;
    }

    /**
     * This should be used when ever changing select.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        this.activeColor = selected ? selectedColor : deselectedColor;
    }

    @Override
    public void playDownSound(SoundManager soundHandler) {
        soundHandler.play(SimpleSoundInstance.forUI(OurSounds.BEEP, selected ? .6F : 1F));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        this.action.test(true);

        if (!this.isSelectable)
            return;

        this.setSelected(!this.selected);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(
                activeColor.getRed() / 255f,
                activeColor.getGreen() / 255f,
                activeColor.getBlue() / 255f,
                0.15f
        );
        guiGraphics.fill(
                this.getX(),
                this.getY(),
                this.getX() + this.width,
                this.getY() + this.height,
                -1873784752
        );

        RenderSystem.setShaderColor(
                activeColor.getRed() / 255f,
                activeColor.getGreen() / 255f,
                activeColor.getBlue() / 255f,
                alpha
        );

        guiGraphics.blit(
                selected ? selectedTexture : deselectedTexture,
                this.getX(),
                this.getY(),
                0,
                0,
                this.width,
                this.height,
                this.width,
                this.height
        );

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (mouseX >= getX() && mouseY >= getY()
                && mouseX < getX() + width && mouseY < getY() + height) {
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    this.getMessage(),
                    mouseX > (Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2)
                            ? mouseX + 2
                            : mouseX - Minecraft.getInstance().font.width(getMessage()),
                    mouseY - 10,
                    activeColor.getRGB()
            );
        }

        RenderSystem.disableBlend();
    }
}

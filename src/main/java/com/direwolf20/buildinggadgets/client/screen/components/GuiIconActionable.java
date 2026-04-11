package com.direwolf20.buildinggadgets.client.screen.components;

import com.direwolf20.buildinggadgets.client.OurSounds;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import net.minecraft.client.input.MouseButtonEvent;
import java.awt.*;
import java.util.function.Predicate;

public class GuiIconActionable extends Button {
    private final Predicate<Boolean> action;
    private boolean selected;
    private final boolean isSelectable;

    private final Color selectedColor = Color.GREEN;
    private final Color deselectedColor = new Color(255, 255, 255);
    private Color activeColor;

    private final Identifier selectedTexture;
    private final Identifier deselectedTexture;

    public GuiIconActionable(int x, int y, String texture, Component message, boolean isSelectable, Predicate<Boolean> action) {
        super(x, y, 25, 25, message, (b) -> {}, Button.DEFAULT_NARRATION);
        this.activeColor = deselectedColor;
        this.isSelectable = isSelectable;
        this.action = action;

        this.setSelected(action.test(false));

        String assetLocation = "textures/gui/setting/%s.png";
        this.deselectedTexture = Identifier.fromNamespaceAndPath(Reference.MODID, String.format(assetLocation, texture));
        this.selectedTexture = !isSelectable ? this.deselectedTexture : Identifier.fromNamespaceAndPath(Reference.MODID, String.format(assetLocation, texture + "_selected"));
    }

    public GuiIconActionable(int x, int y, String texture, Component message, Predicate<Boolean> action) {
        this(x, y, texture, message, false, action);
    }

    public void setFaded(boolean faded) {
        alpha = faded ? .6f : 1f;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        this.activeColor = selected ? selectedColor : deselectedColor;
    }

    @Override
    public void playDownSound(SoundManager soundHandler) {
        soundHandler.play(SimpleSoundInstance.forUI(OurSounds.BEEP, selected ? .6F : 1F));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean ingame) {
        super.onClick(event, ingame);
        this.action.test(true);

        if (!this.isSelectable)
            return;

        this.setSelected(!this.selected);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;


        // Background fill — use the color int directly, guiGraphics.fill ignores setShaderColor
        guiGraphics.fill(
                this.getX(),
                this.getY(),
                this.getX() + this.width,
                this.getY() + this.height,
                -1873784752
        );

        // Icon blit — pass ARGB color with alpha baked in as the last parameter.
        // In 1.21.6, RenderType::guiTextured is replaced by RenderPipelines.GUI_TEXTURED
        int argb = toArgb(activeColor, alpha);
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                selected ? selectedTexture : deselectedTexture,
                this.getX(),
                this.getY(),
                0f,
                0f,
                this.width,
                this.height,
                this.width,
                this.height,
                argb
        );

        // Tooltip on hover
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

    }

    /** Pack r,g,b from a Color and a separate float alpha into an ARGB int. */
    private static int toArgb(Color color, float alpha) {
        int a = Math.round(alpha * 255f);
        return (a << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }
}
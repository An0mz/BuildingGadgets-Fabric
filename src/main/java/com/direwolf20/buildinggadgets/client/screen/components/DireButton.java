package com.direwolf20.buildinggadgets.client.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class DireButton extends Button {

    public DireButton(int x, int y, int widthIn, int heightIn, Component buttonText, OnPress action) {
        super(x, y, widthIn, heightIn, buttonText, action, Button.DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        this.isHovered = this.isMouseOver(mouseX, mouseY);

        // Button background
        int bgColor   = this.active ? (this.isHovered ? 0xFF808080 : 0xFF606060) : 0xFF404040;
        int borderColor = 0xFFA0A0A0;
        // Border
        guiGraphics.fill(this.getX(),                  this.getY(),                   this.getX() + this.width,     this.getY() + 1,          borderColor);
        guiGraphics.fill(this.getX(),                  this.getY() + this.height - 1, this.getX() + this.width,     this.getY() + this.height, borderColor);
        guiGraphics.fill(this.getX(),                  this.getY(),                   this.getX() + 1,              this.getY() + this.height, borderColor);
        guiGraphics.fill(this.getX() + this.width - 1, this.getY(),                   this.getX() + this.width,     this.getY() + this.height, borderColor);
        // Fill
        guiGraphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, bgColor);

        int textColor = 0xFFE0E0E0;
        if (!this.active) textColor = 0xFFA0A0A0;
        else if (this.isHovered) textColor = 0xFFFFFFA0;
        guiGraphics.centeredText(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 7) / 2, textColor);
    }
}

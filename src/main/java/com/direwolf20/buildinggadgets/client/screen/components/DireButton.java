package com.direwolf20.buildinggadgets.client.screen.components;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class DireButton extends Button {

    private static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
    
    public DireButton(int x, int y, int widthIn, int heightIn, Component buttonText, OnPress action) {
        super(x, y, widthIn, heightIn, buttonText, action, Button.DEFAULT_NARRATION);

    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        this.isHovered = this.isMouseOver(mouseX, mouseY);

        guiGraphics.blit(WIDGETS_LOCATION, this.getX(), this.getY(), 0, 46, this.width / 2, this.height);
        guiGraphics.blit(WIDGETS_LOCATION, this.getX() + this.width / 2, this.getY(), 200 - this.width / 2, 46, this.width / 2, this.height);

        int bottomToDraw = 2;
        guiGraphics.blit(WIDGETS_LOCATION, this.getX(), this.getY() + this.height - bottomToDraw, 0, 66 - bottomToDraw, this.width / 2, bottomToDraw);
        guiGraphics.blit(WIDGETS_LOCATION, this.getX() + this.width / 2, this.getY() + this.height - bottomToDraw, 200 - this.width / 2, 66 - bottomToDraw, this.width / 2, bottomToDraw);

        int textColor = 14737632;
        if (!this.active) textColor = 10526880;
        else if (this.isHovered) textColor = 16777120;
        guiGraphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 7) / 2, textColor);
    }

}

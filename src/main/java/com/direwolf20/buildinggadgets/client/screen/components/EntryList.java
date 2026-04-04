package com.direwolf20.buildinggadgets.client.screen.components;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList.Entry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class EntryList<E extends Entry<E>> extends ObjectSelectionList<E> {

    public static final int SCROLL_BAR_WIDTH = 6;

    public EntryList(int left, int top, int width, int height, int slotHeight) {
        super(Minecraft.getInstance(), width, height, top, slotHeight);
        this.setX(left);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        glEnable(GL_SCISSOR_TEST);
        double guiScaleFactor = Minecraft.getInstance().getWindow().getGuiScale();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (getX() * guiScaleFactor),
                (int) (Minecraft.getInstance().getWindow().getHeight() - (getY() + getHeight() * guiScaleFactor)),
                (int) (width * guiScaleFactor),
                (int) (height * guiScaleFactor));

        renderParts(guiGraphics, mouseX, mouseY, partialTicks);
        glDisable(GL_SCISSOR_TEST);
    }

    private void renderParts(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderContentBackground(guiGraphics);

        int k = getRowLeft();
        int l = getY() + 4 - (int) scrollAmount();
        renderHeader(guiGraphics, k, l);

        this.renderListItems(guiGraphics, mouseX, mouseY, partialTicks);
        RenderSystem.disableDepthTest();

        int j1 = maxScrollAmount();
        if (j1 > 0) {
            int k1 = (int) ((float) ((getY() + getHeight() - getY()) * (getY() + getHeight() - getY())) / (float) contentHeight());
            k1 = Mth.clamp(k1, 32, getY() + getHeight() - getY() - 8);
            int l1 = (int) scrollAmount() * (getY() + getHeight() - getY() - k1) / j1 + getY();
            if (l1 < getY()) {
                l1 = getY();
            }
            int x1 = scrollBarX();
            int x2 = x1 + 6;

            GlStateManager._bindTexture(0);
            RenderSystem.setShader(net.minecraft.client.renderer.CoreShaders.POSITION_COLOR);

            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            bufferbuilder.addVertex(x1, getY() + getHeight(), 0.0f).setColor(0, 0, 0, 255);
            bufferbuilder.addVertex(x2, getY() + getHeight(), 0.0f).setColor(0, 0, 0, 255);
            bufferbuilder.addVertex(x2, getY(), 0.0f).setColor(0, 0, 0, 255);
            bufferbuilder.addVertex(x1, getY(), 0.0f).setColor(0, 0, 0, 255);

            bufferbuilder.addVertex(x1, (l1 + k1), 0.0f).setColor(128, 128, 128, 255);
            bufferbuilder.addVertex(x2, (l1 + k1), 0.0f).setColor(128, 128, 128, 255);
            bufferbuilder.addVertex(x2, l1, 0.0f).setColor(128, 128, 128, 255);
            bufferbuilder.addVertex(x1, l1, 0.0f).setColor(128, 128, 128, 255);

            bufferbuilder.addVertex(x1, (l1 + k1 - 1), 0.0f).setColor(192, 192, 192, 255);
            bufferbuilder.addVertex((x2 - 1), (l1 + k1 - 1), 0.0f).setColor(192, 192, 192, 255);
            bufferbuilder.addVertex((x2 - 1), l1, 0.0f).setColor(192, 192, 192, 255);
            bufferbuilder.addVertex(x1, l1, 0.0f).setColor(192, 192, 192, 255);

            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        }

        renderDecorations(guiGraphics, mouseX, mouseY);
        RenderSystem.disableBlend();
    }

    protected void renderContentBackground(GuiGraphics guiGraphics) {
        guiGraphics.fillGradient(RenderType.guiOverlay().bufferSize(), getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xC0101010, 0xD0101010);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        setDragging(true);
        super.mouseClicked(x, y, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        setDragging(false);
        return super.mouseReleased(x, y, button);
    }
}
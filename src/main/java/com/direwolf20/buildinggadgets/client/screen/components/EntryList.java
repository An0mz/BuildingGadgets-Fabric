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
        super(Minecraft.getInstance(), width, height, top, top + height);
        // Set left x and right x, somehow MCP gave it a weird name
        this.setX(left);
    }

    public void render(PoseStack matrices, int mouseX, int mouseY, float partialTicks) {
        glEnable(GL_SCISSOR_TEST);
        double guiScaleFactor = Minecraft.getInstance().getWindow().getGuiScale();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (getX() * guiScaleFactor),
                (int) (Minecraft.getInstance().getWindow().getHeight() - (getY() + getHeight() * guiScaleFactor)),
                (int) (width * guiScaleFactor),
                (int) (height * guiScaleFactor));

        renderParts(matrices, mouseX, mouseY, partialTicks);
        glDisable(GL_SCISSOR_TEST);
    }

    // Copied and modified from AbstractLists#render(int, int, float)
    private void renderParts(PoseStack matrices, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
//        RenderSystem.disableLighting();
//        RenderSystem.disableFog();
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();

        renderContentBackground(guiGraphics);

        int k = getRowLeft();
        int l = getY() + 4 - (int) getScrollAmount();
        renderHeader(guiGraphics, k, l);

        renderList(guiGraphics, mouseX, mouseY, partialTicks);
        RenderSystem.disableDepthTest();

        int j1 = getMaxScroll();
        if (j1 > 0) {
            int k1 = (int) ((float) ((getY() + getHeight() - getY()) * (getY() + getHeight() - getY())) / (float) getMaxPosition());
            k1 = Mth.clamp(k1, 32, getY() + getHeight() - getY() - 8);
            int l1 = (int) getScrollAmount() * (getY() + getHeight() - getY() - k1) / j1 + getY();
            if (l1 < getY()) {
                l1 = getY();
            }
            int x1 = getScrollbarPosition();
            int x2 = x1 + 6;

            GlStateManager._bindTexture(0);
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bufferbuilder.vertex(x1, getY() + getHeight(), 0.0D).color(0, 0, 0, 255).endVertex();
            bufferbuilder.vertex(x2, getY() + getHeight(), 0.0D).color(0, 0, 0, 255).endVertex();
            bufferbuilder.vertex(x2, getY(), 0.0D).color(0, 0, 0, 255).endVertex();
            bufferbuilder.vertex(x1, getY(), 0.0D).color(0, 0, 0, 255).endVertex();

            bufferbuilder.vertex(x1, (l1 + k1), 0.0D).color(128, 128, 128, 255).endVertex();
            bufferbuilder.vertex(x2, (l1 + k1), 0.0D).color(128, 128, 128, 255).endVertex();
            bufferbuilder.vertex(x2, l1, 0.0D).color(128, 128, 128, 255).endVertex();
            bufferbuilder.vertex(x1, l1, 0.0D).color(128, 128, 128, 255).endVertex();

            bufferbuilder.vertex(x1, (l1 + k1 - 1), 0.0D).color(192, 192, 192, 255).endVertex();
            bufferbuilder.vertex((x2 - 1), (l1 + k1 - 1), 0.0D).color(192, 192, 192, 255).endVertex();
            bufferbuilder.vertex((x2 - 1), l1, 0.0D).color(192, 192, 192, 255).endVertex();
            bufferbuilder.vertex(x1, l1, 0.0D).color(192, 192, 192, 255).endVertex();
            tessellator.end();
        }

        renderDecorations(guiGraphics, mouseX, mouseY);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
    }

    protected void renderContentBackground(GuiGraphics guiGraphics) {
        guiGraphics.fillGradient(RenderType.guiOverlay().bufferSize(), getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xC0101010, 0xD0101010);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        setDragging(true);
        super.mouseClicked(x, y, button);
        return isMouseOver(x, y);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        setDragging(false);
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (super.mouseDragged(x, y, button, dx, dy))
            return true;

        // Dragging elements in panel
        if (isMouseOver(x, y)) {
            setScrollAmount(getScrollAmount() - dy);
        }
        return true;
    }
}

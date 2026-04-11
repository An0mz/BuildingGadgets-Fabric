package com.direwolf20.buildinggadgets.client.screen.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList.Entry;
import net.minecraft.client.input.MouseButtonEvent;
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

        this.renderListItems(guiGraphics, mouseX, mouseY, partialTicks);

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

            guiGraphics.fill(x1, getY(), x2, getY() + getHeight(), 0xFF000000);
            guiGraphics.fill(x1, l1, x2, l1 + k1, 0xFF808080);
            guiGraphics.fill(x1, l1, x2 - 1, l1 + k1 - 1, 0xFFC0C0C0);
        }
    }

    protected void renderContentBackground(GuiGraphics guiGraphics) {
        guiGraphics.fillGradient(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xC0101010, 0xD0101010);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean ingame) {
        setDragging(true);
        super.mouseClicked(event, ingame);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        setDragging(false);
        return super.mouseReleased(event);
    }
}
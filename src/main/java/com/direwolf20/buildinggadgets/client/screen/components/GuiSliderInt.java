package com.direwolf20.buildinggadgets.client.screen.components;

import com.direwolf20.buildinggadgets.client.BuildingGadgetsClient;
import com.direwolf20.buildinggadgets.client.screen.GuiMod;
import com.direwolf20.buildinggadgets.common.network.C2S.PacketChangeRange;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.Collection;
import java.util.function.BiConsumer;

public class GuiSliderInt extends AbstractSliderButton {
    private final int colorBackground;
    private final int colorSliderBackground;
    private final int colorSlider;
    private final BiConsumer<GuiSliderInt, Integer> increment;
    private final int minVal, maxVal;
    private final Component prefix;

    public GuiSliderInt(int xPos, int yPos, int width, int height, Component prefix, int minVal, int maxVal,
                        int currentVal, Color color,
                        BiConsumer<GuiSliderInt, Integer> increment) {
        super(xPos, yPos, width, height, prefix, (maxVal == minVal) ? 0.0 : (double) (currentVal - minVal) / (maxVal - minVal));

        this.colorBackground = GuiMod.getColor(color, 200).getRGB();
        this.colorSliderBackground = GuiMod.getColor(color.darker(), 200).getRGB();
        this.colorSlider = GuiMod.getColor(color.brighter().brighter(), 200).getRGB();
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.increment = increment;
        this.prefix = prefix;

        this.updateMessage();
    }

    public int getValueInt() {
        return (int) Mth.clamp(Math.round(minVal + value * (maxVal - minVal)), minVal, maxVal);
    }

    public void setValueInt(int i) {
        setSliderValue((maxVal == minVal) ? 0.0 : (double) (i - minVal) / (maxVal - minVal));
    }

    @Override
    public void onRelease(MouseButtonEvent event) {}

    @Override
    protected void updateMessage() {
        this.setMessage(this.prefix.copy().append(String.valueOf(getValueInt())));
    }

    private void setSliderValue(double d) {
        int oldIntValue = getValueInt();
        this.value = Mth.clamp(d, 0.0D, 1.0D);

        if(oldIntValue != getValueInt()) {
            this.applyValue();
            playSound();
            updateMessage();
        }
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        this.setSliderValue(this.value + deltaX / (this.width - 8));
    }

    @Override
    public void applyValue() {
        PacketChangeRange.send(getValueInt());
    }

    private void playSound() {
        BuildingGadgetsClient.playSound(SoundEvents.DISPENSER_FAIL, 2F);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
        isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, colorBackground);
        renderBg(guiGraphics, mc, mouseX, mouseY);
        renderText(guiGraphics, mc, this);

    }

    private void renderText(GuiGraphics guiGraphics, Minecraft mc, AbstractWidget component) {
        int color = !active ? 10526880 : (isHovered ? 0xFFFFFFA0 : -1);
        String buttonText = component.getMessage().getString();
        int strWidth = mc.font.width(buttonText);
        int ellipsisWidth = mc.font.width("...");

        if (strWidth > component.getWidth() - 6 && strWidth > ellipsisWidth) {
            buttonText = mc.font.plainSubstrByWidth(buttonText, component.getWidth() - 6 - ellipsisWidth).trim() + "...";
        }

        guiGraphics.drawCenteredString(mc.font, buttonText, component.getX() + component.getWidth() / 2, component.getY() + (component.getHeight() - 8) / 2, color);
    }

    @Override
    public void playDownSound(SoundManager p_playDownSound_1_) {
    }

    protected void renderBg(GuiGraphics guiGraphics, Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }

        drawBorderedRect(guiGraphics, (int) (getX() + (value * (width - 8))), getY(), 8, height);
    }

    private void drawBorderedRect(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, colorSliderBackground);
        guiGraphics.fill(++x, ++y, x + width - 2, y + height - 2, colorSlider);
    }

    public Collection<AbstractWidget> getComponents() {
        return ImmutableSet.of(
                this,
                new GuiButtonIncrement(this, getX() - height, getY(), height, height, Component.literal("-"), b -> increment.accept(this, -1)),
                new GuiButtonIncrement(this, getX() + width, getY(), height, height, Component.literal("+"), b -> increment.accept(this, 1)
                ));
    }

    private static class GuiButtonIncrement extends Button {
        private final GuiSliderInt parent;

        public GuiButtonIncrement(GuiSliderInt parent, int x, int y, int width, int height, Component buttonText, OnPress action) {
            super(x, y, width, height, buttonText, action, DEFAULT_NARRATION);
            this.parent = parent;
        }

        @Override
        protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            if (!visible) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, parent.colorBackground);
            parent.drawBorderedRect(guiGraphics, getX(), getY(), width, height);
            parent.renderText(guiGraphics, mc, this);
        }

        @Override
        public void playDownSound(SoundManager p_playDownSound_1_) {
        }
    }
}

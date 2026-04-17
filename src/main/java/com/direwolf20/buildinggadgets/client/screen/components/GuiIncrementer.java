package com.direwolf20.buildinggadgets.client.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class GuiIncrementer extends AbstractWidget {
    // this is the width of all components in a line
    public static final int WIDTH = 64;

    private final int x;
    private final int y;
    private final int min;
    private final int max;

    private int value;
    private final IIncrementerChanged onChange;

    private final Button minusButton;
    private final GuiTextFieldBase field;
    private final Button plusButton;

    public GuiIncrementer(int x, int y, int min, int max, @Nullable IIncrementerChanged onChange) {
        super(x, y, WIDTH, 20, Component.empty());

        this.x = x;
        this.y = y;
        this.min = min;
        this.max = max;
        this.value = 0;
        this.onChange = onChange;

        this.minusButton = Button.builder(Component.literal("-"), (button) -> this.updateValue(true)).bounds(this.x, this.y - 1, 12, 17).build();
        this.field = new GuiTextFieldBase(Minecraft.getInstance().font, x + 13, y, 40).setDefaultInt(this.value).restrictToNumeric();
        this.plusButton = Button.builder(Component.literal("+"), (button) -> this.updateValue(false)).bounds(this.x + 40 + 14, this.y - 1, 12, 17).build();

        this.field.setValue(String.valueOf(this.value));
    }

    public GuiIncrementer(int x, int y) {
        this(x, y, Integer.MIN_VALUE, Integer.MAX_VALUE, null);
    }

    public int getValue() {
        return this.value;
    }

    private void updateValue(boolean isMinus) {
        int modifier = 1;
        long handle = GLFW.glfwGetCurrentContext();
        if (GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS)
            modifier *= 10;

        int value = isMinus ? this.value - modifier : this.value + modifier;
        this.setValue(value);
    }

    public void setValue(int value) {
        // We don't want to fire events for no reason
        if (value == this.value)
            return;

        this.value = Mth.clamp(value, this.min, this.max);
        this.field.setValue(String.valueOf(this.value));

        if (this.onChange != null)
            this.onChange.onChange(value);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.plusButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.minusButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.field.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean ingame) {
        this.field.mouseClicked(event, ingame);
        this.plusButton.mouseClicked(event, ingame);
        this.minusButton.mouseClicked(event, ingame);
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.field.isFocused())
            return false;

        this.field.keyPressed(event);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!this.field.isFocused())
            return false;

        this.field.charTyped(event);
        if (this.field.getValue().length() > 1 && this.field.getValue().charAt(0) == '0')
            this.field.setValue(String.valueOf(this.field.getInt()));

        if (this.field.getInt() > this.max)
            this.field.setValue(String.valueOf(this.max));

        return true;
    }

    protected void onFocusedChanged(boolean isFocused) {
        this.field.setFocused(isFocused);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public interface IIncrementerChanged {
        void onChange(int value);
    }
}

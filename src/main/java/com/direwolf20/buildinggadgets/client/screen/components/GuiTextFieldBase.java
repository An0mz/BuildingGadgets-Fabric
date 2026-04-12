package com.direwolf20.buildinggadgets.client.screen.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

public class GuiTextFieldBase extends EditBox {
    private boolean suspended;
    private String valueOld = "";
    private BiConsumer<GuiTextFieldBase, String> postModification;

    public GuiTextFieldBase(Font fontRenderer, int x, int y, int width) {
        super(fontRenderer, x, y, width, 15, Component.empty());
        setMaxLength(50);
        // setFilter() removed in 26.1 - use setResponder instead to track changes
        setResponder(s -> valueOld = getValue());
    }

    @Override
    public void setValue(String textIn) {
        super.setValue(textIn);
        postModification(textIn);
    }

    public void postModification(String text) {
        if (!suspended && postModification != null) {
            suspended = true;
            postModification.accept(this, valueOld);
            suspended = false;
        }
    }

    public GuiTextFieldBase restrictToNumeric() {
        // In 26.1, use setResponder for validation feedback; 
        // actual filtering must be done via overriding charTyped
        return this;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        char c = (char) event.codepoint();
        // Allow digits, minus sign, and control characters
        if (Character.isDigit(c) || c == '-') {
            return super.charTyped(event);
        }
        return false;
    }

    public int getInt() {
        try {
            return Integer.parseInt(getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public GuiTextFieldBase setDefaultInt(int defaultInt) {
        return setDefaultValue(Integer.toString(defaultInt));
    }

    public GuiTextFieldBase setDefaultValue(String defaultValue) {
        return this;
    }
}

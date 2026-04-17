/**
 * Parts of this class were adapted from code written by TTerrag for the Chisel mod: https://github.com/Chisel-Team/Chisel
 * Chisel is Open Source and distributed under GNU GPL v2
 */

package com.direwolf20.buildinggadgets.client.screen;

import com.direwolf20.buildinggadgets.client.screen.components.GuiIncrementer;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.network.C2S.PacketPasteGUI;
import com.direwolf20.buildinggadgets.common.util.lang.GuiTranslation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PasteGUI extends Screen {
    private GuiIncrementer X, Y, Z;
    private final List<GuiIncrementer> fields = new ArrayList<>();
    private final ItemStack copyPasteTool;

    PasteGUI(ItemStack tool) {
        super(Component.literal(""));
        this.copyPasteTool = tool;
    }

    @Override
    public void init() {
        super.init();

        int x = width / 2;
        int y = height / 2;

        fields.add(X = new GuiIncrementer(x - (GuiIncrementer.WIDTH + (GuiIncrementer.WIDTH / 2)) - 10, y - 10, -16, 16, this::onChange));
        fields.add(Y = new GuiIncrementer(x - GuiIncrementer.WIDTH / 2, y - 10, -16, 16, this::onChange));
        fields.add(Z = new GuiIncrementer(x + (GuiIncrementer.WIDTH / 2) + 10, y - 10, -16, 16, this::onChange));

        BlockPos currentOffset = GadgetCopyPaste.getRelativeVector(this.copyPasteTool);
        X.setValue(currentOffset.getX());
        Y.setValue(currentOffset.getY());
        Z.setValue(currentOffset.getZ());

        List<AbstractButton> buttons = new ArrayList<>() {{
            add(Button.builder(GuiTranslation.SINGLE_CONFIRM.componentTranslation(), (button) -> {
                PacketPasteGUI.send(X.getValue(), Y.getValue(), Z.getValue());
                onClose();
            }).pos(0, y + 20).size(70, 20).build());

            add(Button.builder(GuiTranslation.SINGLE_RESET.componentTranslation(), (button) -> {
                X.setValue(0);
                Y.setValue(0);
                Z.setValue(0);
                sendPacket();
            }).pos(0, y + 20).size(40, 20).build());
        }};

        centerButtonList(buttons, x);

        buttons.forEach(this::addRenderableWidget);
        fields.forEach(this::addRenderableWidget);
    }

    private void sendPacket() {
        PacketPasteGUI.send(X.getValue(), Y.getValue(), Z.getValue());
    }

    private void onChange(int value) {
        PacketPasteGUI.send(X.getValue(), Y.getValue(), Z.getValue());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        fields.forEach(button -> button.keyPressed(event));
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        fields.forEach(button -> button.charTyped(event));
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Labels
        drawLabel(guiGraphics, "X", -75);
        drawLabel(guiGraphics, "Y", 0);
        drawLabel(guiGraphics, "Z", 75);

        // Heading
        guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                I18n.get(GuiTranslation.COPY_LABEL_HEADING.getTranslationKey()),
                width / 2,
                height / 2 - 60,
                0xFFFFFFFF
        );
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawLabel(GuiGraphics guiGraphics, String name, int xOffset) {
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                name,
                (int) (width / 2f) + xOffset,
                (int) (height / 2f) - 30,
                0xFFFFFFFF,
                true // shadow
        );
    }

    private static void centerButtonList(List<AbstractButton> buttons, int startX) {
        int collectiveWidth = buttons.stream().mapToInt(AbstractButton::getWidth).sum() + (buttons.size() - 1) * 5;
        int nextX = startX - collectiveWidth / 2;
        for (AbstractButton button : buttons) {
            button.setX(nextX);
            nextX += button.getWidth() + 5;
        }
    }

}

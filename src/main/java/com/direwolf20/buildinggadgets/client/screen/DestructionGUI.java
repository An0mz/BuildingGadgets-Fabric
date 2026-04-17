package com.direwolf20.buildinggadgets.client.screen;

import com.direwolf20.buildinggadgets.client.screen.components.GuiSliderInt;
import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets.common.network.C2S.PacketDestructionGUI;
import com.direwolf20.buildinggadgets.common.util.lang.GuiTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.MessageTranslation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class DestructionGUI extends Screen {
    private final Set<GuiDestructionSlider> sliders = new HashSet<>();
    private GuiDestructionSlider left;
    private GuiDestructionSlider right;
    private GuiDestructionSlider up;
    private GuiDestructionSlider down;
    private GuiDestructionSlider depth;
    private Button confirm;

    private String sizeString = "";
    private boolean isValidSize = true;

    private final ItemStack destructionTool;

    public DestructionGUI(ItemStack tool) {
        super(Component.literal("Destruction Gui?!?"));
        this.destructionTool = tool;
    }

    @Override
    public void init() {
        super.init();

        int x = width / 2;
        int y = height / 2;

        // CHANGED: Store the button in this.confirm
        this.confirm = this.addRenderableWidget(
                Button.builder(Component.translatable(GuiMod.getLangKeySingle("confirm")), b -> {
                            if (Minecraft.getInstance().player == null) return;

                            if (isWithinBounds()) {
                                PacketDestructionGUI.send(left.getValueInt(), right.getValueInt(), up.getValueInt(), down.getValueInt(), depth.getValueInt());
                                this.onClose();
                            } else {
                                Minecraft.getInstance().player.displayClientMessage(
                                        MessageTranslation.DESTRCUT_TOO_LARGE.componentTranslation(BuildingGadgets.getConfig().gadgets.gadgetDestruction.destroySize),
                                        true
                                );
                            }
                        }).bounds((x - 30) + 32, y + 65, 60, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.translatable(GuiMod.getLangKeySingle("cancel")), b -> onClose())
                        .bounds((x - 30) - 32, y + 65, 60, 20)
                        .build()
        );

        sliders.clear();
        sliders.add(depth = new GuiDestructionSlider(x - (GuiDestructionSlider.width / 2), y - (GuiDestructionSlider.height / 2), GuiTranslation.SINGLE_DEPTH.format() + ":", GadgetDestruction.getToolValue(destructionTool, "depth")));
        sliders.add(right = new GuiDestructionSlider(x + (GuiDestructionSlider.width + 5), y - (GuiDestructionSlider.height / 2), GuiTranslation.SINGLE_RIGHT.format() + ":", GadgetDestruction.getToolValue(destructionTool, "right")));
        sliders.add(left = new GuiDestructionSlider(x - (GuiDestructionSlider.width * 2) - 5, y - (GuiDestructionSlider.height / 2), GuiTranslation.SINGLE_LEFT.format() + ":", GadgetDestruction.getToolValue(destructionTool, "left")));
        sliders.add(up = new GuiDestructionSlider(x - (GuiDestructionSlider.width / 2), y - 35, GuiTranslation.SINGLE_UP.format() + ":", GadgetDestruction.getToolValue(destructionTool, "up")));
        sliders.add(down = new GuiDestructionSlider(x - (GuiDestructionSlider.width / 2), y + 20, GuiTranslation.SINGLE_DOWN.format() + ":", GadgetDestruction.getToolValue(destructionTool, "down")));

        updateSizeString();
        updateIsValid();

        sliders.forEach(gui -> gui.getComponents().forEach(this::addRenderableWidget));
    }

    private boolean isWithinBounds() {
        int x = 1 + left.getValueInt() + right.getValueInt();
        int y = 1 + up.getValueInt() + down.getValueInt();
        int z = depth.getValueInt();
        int dim = BuildingGadgets.getConfig().gadgets.gadgetDestruction.destroySize;

        return x <= (dim + 1) && y <= (dim + 1) && z <= dim;
    }

    private String getSizeString() {
        return String.format("%d x %d x %d",
                left.getValueInt() + right.getValueInt() + 1,
                up.getValueInt() + down.getValueInt() + 1,
                depth.getValueInt()
        );
    }

    private void updateIsValid() {
        this.isValidSize = isWithinBounds();
        if (!isValidSize && this.confirm.active) {
            //this.confirm.setFGColor(0xFF2000);
            this.confirm.active = false;
        }

        if (isValidSize && !this.confirm.active) {
            //this.confirm.clearFGColor();
            this.confirm.active = true;
        }
    }

    private void updateSizeString() {
        this.sizeString = getSizeString();
    }

    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawCenteredString(font, this.sizeString, width / 2, (height / 2) + 40, this.isValidSize ? 0xFF00FF00 : 0xFFFF2000);
        if (!this.isValidSize) {
            guiGraphics.drawCenteredString(font, MessageTranslation.DESTRCUT_TOO_LARGE.format(BuildingGadgets.getConfig().gadgets.gadgetDestruction.destroySize), width / 2, (height / 2) + 50, 0xFFFF2000);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (GuiDestructionSlider slider : sliders) {
            slider.onRelease(event);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // This is only done to reduce code dupe in this class.
    private class GuiDestructionSlider extends GuiSliderInt {
        public static final int width = 70;
        public static final int height = 14;

        private static final int min = 0;
        private static final int max = 16;


        GuiDestructionSlider(int x, int y, String prefix, int current) {
            super(
                    x, y, width, height, Component.literal(String.format("%s ", prefix)), min, max, current, Color.DARK_GRAY, (GuiSliderInt slider, Integer amount) -> {
                        slider.setValueInt(Mth.clamp(slider.getValueInt() + amount, min, max));
                        slider.applyValue();
                    }
            );
        }

        @Override
        public void applyValue() {
            super.applyValue();
            DestructionGUI.this.updateSizeString();
            DestructionGUI.this.updateIsValid();
        }
    }
}

package com.direwolf20.buildinggadgets.client.screen.tooltip;

import com.direwolf20.buildinggadgets.client.EventUtil;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.inventory.IItemIndex;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.tainted.inventory.MatchResult;
import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.MaterialList;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateProvider;
import com.direwolf20.buildinggadgets.common.tainted.template.Template;
import com.direwolf20.buildinggadgets.common.tainted.template.TemplateHeader;
import com.direwolf20.buildinggadgets.common.util.TemplateKeyHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TemplateTooltip implements ClientTooltipComponent {

    ItemStack itemStack;
    Minecraft mc = Minecraft.getInstance();
    int count = 0;

    public TemplateTooltip(TemplateData data) {
        itemStack = data.getStack();
    }

    private static boolean isShiftDown() {
        long handle = GLFW.glfwGetCurrentContext();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public int getHeight(Font font) {
        if(this.getCount() > 0 && isShiftDown()) {
            return (((count - 1) / EventUtil.STACKS_PER_LINE) + 1) * 21;
        }
        return 0;
    }

    @Override
    public int getWidth(Font font) {
        if(this.getCount() > 0 && isShiftDown()) {
            return Math.min(EventUtil.STACKS_PER_LINE, count) * 18;
        }
        return 0;
    }

    private int getCount() {
        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(mc.level).ifPresent((ITemplateProvider provider) -> {
            ITemplateKey templateKey = TemplateKeyHelper.getTemplateKey(itemStack);
            if (templateKey != null) {
                Template template = provider.getTemplateForKey(templateKey);
                IItemIndex index = InventoryHelper.index(itemStack, mc.player);

                BuildContext buildContext = BuildContext.builder()
                        .stack(itemStack)
                        .player(mc.player)
                        .build(mc.level);

                TemplateHeader header = template.getHeaderAndForceMaterials(buildContext);
                MaterialList list = header.getRequiredItems();
                if (list == null)
                    list = MaterialList.empty();

                MatchResult match;

                try (Transaction transaction = Transaction.openOuter()) {
                    match = index.match(list, transaction);
                }
                count = match.isSuccess() ? match.getChosenOption().entrySet().size() : match.getChosenOption().entrySet().size() + 1;
            }
        });
        return count;
    }

    @Override
    public void extractImage(Font font, int xin, int yin, int width, int height, GuiGraphicsExtractor guiGraphics) {
        if (!isShiftDown())
            return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null)
            return;

        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(mc.level).ifPresent((ITemplateProvider provider) -> {
            ITemplateKey templateKey = TemplateKeyHelper.getTemplateKey(itemStack);
            if (templateKey != null) {
                Template template = provider.getTemplateForKey(templateKey);
                IItemIndex index = InventoryHelper.index(itemStack, mc.player);
                BuildContext buildContext = BuildContext.builder()
                        .stack(itemStack)
                        .player(mc.player)
                        .build(mc.level);
                TemplateHeader header = template.getHeaderAndForceMaterials(buildContext);
                MaterialList list = header.getRequiredItems();
                if (list == null)
                    list = MaterialList.empty();

                MatchResult match;

                try (Transaction transaction = Transaction.openOuter()) {
                    match = index.match(list, transaction);
                }

                Multiset<ItemVariant> existing = match.getFoundItems();
                List<Multiset.Entry<ItemVariant>> sortedEntries = ImmutableList.sortedCopyOf(EventUtil.ENTRY_COMPARATOR, match.getChosenOption().entrySet());

                int j = 0;
                for (Multiset.Entry<ItemVariant> entry : sortedEntries) {
                    int x = xin + (j % EventUtil.STACKS_PER_LINE) * 18;
                    int y = yin + (j / EventUtil.STACKS_PER_LINE) * 20;
                    renderRequiredBlocks(
                            guiGraphics,
                            entry.getElement().toStack(),
                            font,
                            x,
                            y,
                            existing.count(entry.getElement()),
                            entry.getCount()
                    );
                    j++;
                }
            }
        });
    }

    private int renderRequiredBlocks(
            GuiGraphicsExtractor guiGraphics,
            ItemStack itemStack,
            Font font,
            int x,
            int y,
            int count,
            int req
    ) {
        if (itemStack.isEmpty()) return 0;

        String s1 = req == Integer.MAX_VALUE ? "\u221E" : Integer.toString(req);
        int w1 = font.width(s1);

        boolean hasReq = req > 0;

        guiGraphics.item(itemStack, x, y);
        guiGraphics.itemDecorations(font, itemStack, x, y);

        // Draw count text at 0.5x scale using Matrix3x2fStack
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x + 8 - w1 / 2f, y + (hasReq ? 12 : 14));
        guiGraphics.pose().scale(0.5f, 0.5f);
        guiGraphics.text(font, s1, 0, 0, 0xFFFFFFFF, true);
        guiGraphics.pose().popMatrix();

        int missingCount = 0;

        if (hasReq && count < req) {
            String s2 = "(" + (req - count) + ")";
            int w2 = font.width(s2);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(x + 8 - w2 / 2f, y + 17);
            guiGraphics.pose().scale(0.5f, 0.5f);
            guiGraphics.text(font, s2, 0, 0, 0xFFFF0000, true);
            guiGraphics.pose().popMatrix();

            missingCount = req - count;
        }

        return missingCount;
    }
}

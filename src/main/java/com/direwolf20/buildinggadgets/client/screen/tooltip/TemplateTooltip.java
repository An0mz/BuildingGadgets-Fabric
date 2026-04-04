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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class TemplateTooltip implements ClientTooltipComponent {

    ItemStack itemStack;
    Minecraft mc = Minecraft.getInstance();
    int count = 0;

    public TemplateTooltip(TemplateData data) {
        itemStack = data.getStack();
    }

    @Override
    public int getHeight(net.minecraft.client.gui.Font font) {
        if(this.getCount() > 0 && Screen.hasShiftDown()) {
            return (((count - 1) / EventUtil.STACKS_PER_LINE) + 1) * 21;
        }
        return 0;
    }

    @Override
    public int getWidth(Font font) {
        if(this.getCount() > 0 && Screen.hasShiftDown()) {
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

    public void renderImage(GuiGraphics guiGraphics, Font font, int xin, int yin, int k) {
        if (!Screen.hasShiftDown())
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

                int by = yin;
                int j = 0;
                int totalMissing = 0;
                for (Multiset.Entry<ItemVariant> entry : sortedEntries) {
                    int x = xin + (j % EventUtil.STACKS_PER_LINE) * 18;
                    int y = yin + (j / EventUtil.STACKS_PER_LINE) * 20;
                    totalMissing += renderRequiredBlocks(
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
            GuiGraphics guiGraphics,
            ItemStack itemStack,
            Font font,
            int x,
            int y,
            int count,
            int req
    ) {
        if (itemStack.isEmpty()) return 0;
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        String s1 = req == Integer.MAX_VALUE ? "\u221E" : Integer.toString(req);
        int w1 = font.width(s1);

        boolean hasReq = req > 0;

        itemRenderer.renderStatic(
                itemStack,
                ItemDisplayContext.GUI,
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                guiGraphics.pose(),
                net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource(),
                Minecraft.getInstance().level,
                0
        );

        guiGraphics.renderItemDecorations(font, itemStack, x, y);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 8 - w1 / 2f, y + (hasReq ? 12 : 14), 500f);
        guiGraphics.pose().scale(0.5f, 0.5f, 1f);
        font.drawInBatch(
                s1,
                0f,
                0f,
                0xFFFFFF,
                true,
                guiGraphics.pose().last().pose(),
                net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource(),
                Font.DisplayMode.NORMAL,
                0,
                15728880
        );
        guiGraphics.pose().popPose();

        int missingCount = 0;

        if (hasReq && count < req) {
            String s2 = "(" + (req - count) + ")";
            int w2 = font.width(s2);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + 8 - w2 / 2f, y + 17, 500f);
            guiGraphics.pose().scale(0.5f, 0.5f, 1f);
            font.drawInBatch(
                    s2,
                    0f,
                    0f,
                    0xFF0000,
                    true,
                    guiGraphics.pose().last().pose(),
                    net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource(),
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
            );
            guiGraphics.pose().popPose();

            missingCount = req - count;
        }

        return missingCount;
    }
}
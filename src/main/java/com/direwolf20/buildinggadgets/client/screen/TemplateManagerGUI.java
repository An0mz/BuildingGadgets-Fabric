package com.direwolf20.buildinggadgets.client.screen;

/**
 * Parts of this class were adapted from code written by TTerrag for the Chisel mod: https://github.com/Chisel-Team/Chisel
 * Chisel is Open Source and distributed under GNU GPL v2
 */

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.containers.TemplateManagerContainer;
import com.direwolf20.buildinggadgets.common.items.OurItems;
import com.direwolf20.buildinggadgets.common.network.C2S.PacketTemplateManagerTemplateCreated;
import com.direwolf20.buildinggadgets.common.tainted.building.PlacementTarget;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.building.view.IBuildView;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.tainted.inventory.MatchResult;
import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.MaterialList;
import com.direwolf20.buildinggadgets.common.tainted.template.*;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateProvider.IUpdateListener;
import com.direwolf20.buildinggadgets.common.tileentities.TemplateManagerTileEntity;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.direwolf20.buildinggadgets.common.util.TemplateKeyHelper;
import com.direwolf20.buildinggadgets.common.util.exceptions.TemplateParseException.IllegalMinecraftVersionException;
import com.direwolf20.buildinggadgets.common.util.exceptions.TemplateParseException.UnknownTemplateVersionException;
import com.direwolf20.buildinggadgets.common.util.exceptions.TemplateReadException;
import com.direwolf20.buildinggadgets.common.util.exceptions.TemplateReadException.CorruptJsonException;
import com.direwolf20.buildinggadgets.common.util.exceptions.TemplateWriteException.DataCannotBeWrittenException;
import com.direwolf20.buildinggadgets.common.util.lang.GuiTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.MessageTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.Styles;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.direwolf20.buildinggadgets.common.world.MockDelegationWorld;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@IPNIgnore
public class TemplateManagerGUI extends AbstractContainerScreen<TemplateManagerContainer> {
    private static final ResourceLocation background = ResourceLocation.fromNamespaceAndPath(Reference.MODID, "textures/gui/template_manager.png");

    private final Rect2i panel = new Rect2i(8, 23, 136, 80);
    private boolean panelClicked;
    private int clickButton, clickX, clickY;
    private float initRotX, initRotY, initZoom, initPanX, initPanY;
    private float momentumX, momentumY;
    private float rotX = 0, rotY = 0, zoom = 1;
    private float panX = 0, panY = 0;

    private EditBox nameField;
    private Button buttonSave, buttonLoad, buttonCopy, buttonPaste;

    private final TemplateManagerTileEntity be;
    private final TemplateManagerContainer container;
    private final Optional<ITemplateProvider> templateProvider = BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(getWorld());

    // It is so stupid I can't get the key from the template.
    private Template template;

    public TemplateManagerGUI(TemplateManagerContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, Component.literal(""));

        this.container = container;
        this.be = container.getTe();
        this.imageWidth = 250;
        this.imageHeight = 192;
    }

    @Override
    public void init() {
        super.init();

        this.nameField = new EditBox(
                this.font,
                this.leftPos + 8,
                topPos + 6,
                imageWidth - 90,
                this.font.lineHeight + 3,
                GuiTranslation.TEMPLATE_NAME_TIP.componentTranslation()
        );

        int x = leftPos + 182;

        buttonSave = addRenderableWidget(Button.builder(
                GuiTranslation.BUTTON_SAVE.componentTranslation(),
                b -> onSave()
        ).bounds(x, topPos + 41, 60, 20).build());

        buttonLoad = addRenderableWidget(Button.builder(
                GuiTranslation.BUTTON_LOAD.componentTranslation(),
                b -> onLoad()
        ).bounds(x, topPos + 63, 60, 20).build());

        buttonCopy = addRenderableWidget(Button.builder(
                GuiTranslation.BUTTON_COPY.componentTranslation(),
                b -> onCopy()
        ).bounds(x, topPos + 90, 60, 20).build());

        buttonPaste = addRenderableWidget(Button.builder(
                GuiTranslation.BUTTON_PASTE.componentTranslation(),
                b -> onPaste()
        ).bounds(x, topPos + 112, 60, 20).build());

        this.nameField.setMaxLength(50);
        this.nameField.setVisible(true);
        addRenderableWidget(nameField);
    }


    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        guiGraphics.drawString(font, "Preview disabled for now...", leftPos + 10, topPos + 56, 0xFFFFFF);
        if (this.template != null) {
            renderRequirement(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        RenderSystem.setShaderTexture(0, background);
        guiGraphics.blit(background, leftPos, topPos, 0, 0, 176, 192);
        guiGraphics.blit(background, leftPos + 176, topPos + 29, 176, 28, 76, 113);

        if (!buttonCopy.isHoveredOrFocused() && !buttonPaste.isHoveredOrFocused()) {
            int x = (leftPos + imageWidth) - 98;
            int y = topPos + 49;

            if (buttonLoad.isHoveredOrFocused())
                guiGraphics.blit(background,x, y, 176, 0, 17, 24);
            else
                guiGraphics.blit(background,x, y, 193, 0, 16, 24);
        }

        this.nameField.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.fill(leftPos + panel.getX() - 1, topPos + panel.getY() - 1, leftPos + panel.getX() + panel.getWidth() + 1, topPos + panel.getY() + panel.getHeight() + 1, 0xFF8A8A8A);

    }

    private void validateCache(float partialTicks) {
        if (templateProvider.isEmpty()) {
            return;
        }

        // Invalidate the render
        if (container.getSlot(0).getItem().isEmpty() && template != null) {
            template = null;
            resetViewport();
            return;
        }

        ITemplateKey key = TemplateKeyHelper.getTemplateKey(container.getSlot(0).getItem());
        // Make sure we're not re-creating the same cache.
        if(key == null)
            return;

        Template template = templateProvider.get().getTemplateForKey(key);
        if (this.template == template)
            return;

        this.template = template;
        //TODO: fix rendering
    }

    private void renderStructure(IBuildView view, float partialTicks) {
        Random rand = new Random();
        BlockRenderDispatcher dispatcher = getMinecraft().getBlockRenderer();

        for (PlacementTarget target : view) {
            target.placeIn(view.getContext());
            BlockPos targetPos = target.getPos();
            BlockState renderBlockState = view.getContext().getWorld().getBlockState(targetPos);
            BlockEntity be = view.getContext().getWorld().getBlockEntity(targetPos);

            if (renderBlockState.getRenderShape() == RenderShape.MODEL) {
                BakedModel model = dispatcher.getBlockModel(renderBlockState);
            }

            if (be != null) {
                try {
                    BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
                    if (renderer != null) {
                    }
                    //remember vanilla Tiles rebinding the TextureAtlas
                    RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
                } catch (Exception e) {
                    BuildingGadgets.LOG.error("Error rendering TileEntity", e);
                }
            }
        }

        // end structure preview rendering
    }

    private void renderRequirement(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MaterialList requirements = this.template.getHeaderAndForceMaterials(BuildContext.builder().build(getWorld())).getRequiredItems();
        if (requirements == null) return;

        Lighting.setupForFlatItems();

        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(leftPos - 30, topPos - 5, 200);
        poseStack.scale(0.8f, 0.8f, 0.8f);

        String title = "Requirements";
        guiGraphics.drawString(getMinecraft().font, title, 5 - getMinecraft().font.width(title), 0, Color.WHITE.getRGB());

        MatchResult list;
        try (Transaction transaction = Transaction.openOuter()) {
            list = InventoryHelper.CREATIVE_INDEX.match(requirements, transaction);
        }

        ImmutableMultiset<ItemVariant> foundItems = list.getFoundItems();

        List<Multiset.Entry<ItemVariant>> sortedEntries = ImmutableList.sortedCopyOf(
                Comparator.<Multiset.Entry<ItemVariant>, Integer>comparing(Multiset.Entry::getCount).reversed(),
                list.getChosenOption().entrySet()
        );

        int index = 0, column = 0;
        for (Multiset.Entry<ItemVariant> e : sortedEntries) {
            ItemStack stack = e.getElement().toStack();
            int x = -20 - (column * 25);
            int y = 20 + (index * 25);

            guiGraphics.renderItem(stack, x + 4, y + 4);
            guiGraphics.renderItemDecorations(getMinecraft().font, stack, x + 4, y + 4, GadgetUtils.withSuffix(foundItems.count(e.getElement())));

            int space = (int) (25 - (.2f * 25));
            int zoneX = (leftPos - 32) + (-15 - (column * space));
            int zoneY = (topPos - 9) + (20 + (index * space));

            if (mouseX > zoneX && mouseX < (zoneX + space) && mouseY > zoneY && mouseY < (zoneY + space)) {
                List<Component> tooltip = stack.getTooltipLines(
                        Item.TooltipContext.of(getMinecraft().level),
                        getMinecraft().player,
                        TooltipFlag.NORMAL
                );

                guiGraphics.renderTooltip(
                        getMinecraft().font,
                        (Component) tooltip,
                        mouseX,
                        mouseY
                );
            }

            index++;
            if (index % 8 == 0) {
                column++;
                index = 0;
            }
        }

        Lighting.setupFor3DItems();
        poseStack.popPose();
    }


    private void pasteTemplateToStack(Level world, ItemStack stack, Template newTemplate, boolean replaced) {
        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(world).ifPresent((ITemplateProvider provider) ->
                pasteTemplateToStack(provider, stack, newTemplate, replaced && world.isClientSide()));
    }

    private void pasteTemplateToStack(ITemplateProvider provider, ItemStack stack, Template newTemplate, boolean replaced) {
        ITemplateKey key = TemplateKeyHelper.getTemplateKey(stack);
        if (key != null) {
            provider.setTemplate(key, newTemplate);
            if (replaced) {
                PacketTemplateManagerTemplateCreated.send(provider.getId(key), be.getBlockPos());
            } else {
                provider.requestRemoteUpdate(key, be.getLevel());
            }
        }
    }

    private boolean replaceStack() {
        ItemStack stack = container.getSlot(1).getItem();
        if (stack.isEmpty())
            return false;

        if (TemplateKeyHelper.hasTemplateKey(stack))
            return false;

        else if (stack.is(TemplateManagerTileEntity.TEMPLATE_CONVERTIBLES)) {
            ItemStack newStack = new ItemStack(OurItems.TEMPLATE_ITEM);
            TemplateKeyHelper.initializeTemplateKey(newStack);
            container.getSlot(1).set(newStack);
            return true;
        }

        return false;
    }

    private void rename(ItemStack stack) {
        if (nameField.getValue().isEmpty())
            return;

        ITemplateKey key = TemplateKeyHelper.getTemplateKey(stack);
        if (key != null) {
            templateProvider.ifPresent((ITemplateProvider provider) -> {
                Template template = provider.getTemplateForKey(key);
                template = template.withName(nameField.getValue());
                provider.setTemplate(key, template);
                provider.requestRemoteUpdate(key, getWorld());
            });
        }
    }

    private void renderPanel(PoseStack pose, int partialTicks) {
        validateCache(partialTicks);

        if(template == null)
            return;

        double scale = getMinecraft().getWindow().getGuiScale();

        BlockPos startPos = template.getHeader().getBoundingBox().getMin();
        BlockPos endPos = template.getHeader().getBoundingBox().getMax();

        double lengthX = Math.abs(startPos.getX() - endPos.getX());
        double lengthY = Math.abs(startPos.getY() - endPos.getY());
        double lengthZ = Math.abs(startPos.getZ() - endPos.getZ());

        final double maxW = 6 * 16;
        final double maxH = 11 * 16;

        double overW = Math.max(lengthX * 16 - maxW, lengthZ * 16 - maxW);
        double overH = lengthY * 16 - maxH;

        double sc = 1;
        double zoomScale = 1;

        if (overW > 0 && overW >= overH) {
            sc = maxW / (overW + maxW);
            zoomScale = overW / 40;
        } else if (overH > 0 && overH >= overW) {
            sc = maxH / (overH + maxH);
            zoomScale = overH / 40;
        }

        pose.pushPose();
        pose.pushPose();
        pose.setIdentity();

        Matrix4f perspectiveMatrix = new Matrix4f();
        perspectiveMatrix.perspective(60f, (float) panel.getWidth() / panel.getHeight(), 0.01f, 4000f);
        pose.mulPose(perspectiveMatrix);
        RenderSystem.viewport((int) Math.round((leftPos + panel.getX()) * scale),
                (int) Math.round(getMinecraft().getWindow().getHeight() - (topPos + panel.getY() + panel.getHeight()) * scale),
                (int) Math.round(panel.getWidth() * scale),
                (int) Math.round(panel.getHeight() * scale));

        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, true);

        sc = (293 * sc) + zoom / zoomScale;
        pose.scale((float) sc, (float) sc, (float) sc);
        int moveX = startPos.getX() - endPos.getX();

        float radians = (float) Math.toRadians(30);
        Quaternionf rot = new Quaternionf().rotateAxis(radians, 0, 1, 0);
        pose.mulPose(rot);
        if (startPos.getX() >= endPos.getX())
            moveX--;

        pose.translate((moveX) / 1.75, -Math.abs(startPos.getY() - endPos.getY()) / 1.75, 0);
        pose.translate(panX, -panY, 0);
        pose.translate(((startPos.getX() - endPos.getX()) / 2f) * -1, ((startPos.getY() - endPos.getY()) / 2f) * -1, ((startPos.getZ() - endPos.getZ()) / 2f) * -1);
        pose.mulPose(new Quaternionf().rotateX((float)Math.toRadians(-rotX)));
        pose.mulPose(new Quaternionf().rotateY((float)Math.toRadians(rotY)));
        pose.translate(((startPos.getX() - endPos.getX()) / 2f), ((startPos.getY() - endPos.getY()) / 2f), ((startPos.getZ() - endPos.getZ()) / 2f));

        RenderSystem.disableDepthTest();

        IBuildView view = template.createViewInContext(
                BuildContext.builder()
                        .player(getMinecraft().player)
                        .stack(container.getSlot(0).getItem())
                        .build(new MockDelegationWorld(getMinecraft().level)));

        renderStructure(view, partialTicks);

        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

        pose.popPose();
        pose.popPose();
        RenderSystem.viewport(0, 0, getMinecraft().getWindow().getWidth(), getMinecraft().getWindow().getHeight());
    }

    private void resetViewport() {
        rotX = 0;
        rotY = 0;
        zoom = 1;
        momentumX = 0;
        momentumY = 0;
        panX = 0;
        panY = 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (panel.contains((int) mouseX - leftPos, (int) mouseY - topPos)) {
            clickButton = mouseButton;
            panelClicked = true;
            clickX = (int) getMinecraft().mouseHandler.xpos();
            clickY = (int) getMinecraft().mouseHandler.ypos();
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        panelClicked = false;
        initRotX = rotX;
        initRotY = rotY;
        initPanX = panX;
        initPanY = panY;
        initZoom = zoom;

        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        if (p_keyPressed_1_ == 256) {
            this.onClose();
            return true;
        }

        return this.nameField.isFocused() ? this.nameField.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_) : super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }

    protected void renderLabels(PoseStack matrices, int mouseX, int mouseY, GuiGraphics guiGraphics) {
        if (panelClicked) {
            if (clickButton == 0) {
                float prevRotX = rotX;
                float prevRotY = rotY;
                rotX = initRotX - ((int) getMinecraft().mouseHandler.ypos() - clickY);
                rotY = initRotY + ((int) getMinecraft().mouseHandler.xpos() - clickX);
                momentumX = rotX - prevRotX;
                momentumY = rotY - prevRotY;
            } else if (clickButton == 1) {
                panX = initPanX + ((int) getMinecraft().mouseHandler.xpos() - clickX) / 8f;
                panY = initPanY + ((int) getMinecraft().mouseHandler.ypos() - clickY) / 8f;
            }
        }

        rotX += momentumX;
        rotY += momentumY;
        float momentumDampening = 0.98f;
        momentumX *= momentumDampening;
        momentumY *= momentumDampening;

        if (!nameField.isFocused() && nameField.getValue().isEmpty()) {
            guiGraphics.drawString(
                    getMinecraft().font,
                    GuiTranslation.TEMPLATE_PLACEHOLDER.format(),
                    nameField.getX() - leftPos + 4,
                    nameField.getY() - topPos + 2,
                    0xFF666666
            );
        }

        // Draw slot overlays if buttons hovered
        if (buttonSave.isHoveredOrFocused() || buttonLoad.isHoveredOrFocused() || buttonPaste.isHoveredOrFocused()) {
            Slot slotToHighlight = buttonLoad.isHoveredOrFocused() ? container.getSlot(0) : container.getSlot(1);
            drawSlotOverlay(matrices, slotToHighlight, guiGraphics);
        }
    }

    private void drawSlotOverlay(PoseStack matrices, Slot slot, GuiGraphics guiGraphics) {
        matrices.pushPose();
        matrices.translate(0, 0, 1000);
        guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x9E000000);
        matrices.popPose();
    }


    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta, double scrollY) {
        zoom = initZoom + ((float) scrollDelta * 20);
        if (zoom < -200) zoom = -200;
        if (zoom > 5000) zoom = 5000;

        return super.mouseScrolled(mouseX, mouseY, scrollDelta, scrollY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (!panelClicked) {
            initRotX = rotX;
            initRotY = rotY;
            initZoom = zoom;
            initPanX = panX;
            initPanY = panY;
        }
    }

    private Level getWorld() {
        return getMinecraft().level;
    }

    public Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    // Events
    private void runAfterUpdate(int slot, Runnable runnable) {
        ITemplateKey key = TemplateKeyHelper.getTemplateKey(container.getSlot(slot).getItem());
        if (key != null) {
            templateProvider.ifPresent((ITemplateProvider provider) -> {
                provider.registerUpdateListener(new IUpdateListener() {
                    @Override
                    public void onTemplateUpdate(ITemplateProvider provider, ITemplateKey updateKey, Template template) {
                        if (provider.getId(updateKey).equals(provider.getId(key))) {
                            runnable.run();
                            provider.removeUpdateListener(this);
                        }
                    }
                });
                provider.requestUpdate(key);
            });
        }
    }

    private void onSave() {
        boolean replaced = replaceStack();
        ItemStack left = container.getSlot(0).getItem();
        ItemStack right = container.getSlot(1).getItem();
        if (left.isEmpty()) {
            rename(right);
            return;
        }

        runAfterUpdate(0, () -> {
            templateProvider.ifPresent((ITemplateProvider provider) -> {
                ITemplateKey key = TemplateKeyHelper.getTemplateKey(left);
                if (key != null) {
                    Template templateToSave = provider.getTemplateForKey(key).withName(nameField.getValue());
                    pasteTemplateToStack(provider, right, templateToSave, replaced);
                }
            });
        });
    }

    private void onLoad() {
        boolean replaced = replaceStack();
        ItemStack left = container.getSlot(0).getItem();
        ItemStack right = container.getSlot(1).getItem();
        if (left.isEmpty()) {
            rename(right);
            return;
        }

        runAfterUpdate(1, () -> {
            templateProvider.ifPresent((ITemplateProvider provider) -> {
                ITemplateKey key = TemplateKeyHelper.getTemplateKey(right);
                if (key != null) {
                    Template templateToSave = provider.getTemplateForKey(key);
                    pasteTemplateToStack(provider, left, templateToSave, replaced);
                }
            });
        });
    }

    private void onCopy() {
        runAfterUpdate(0, () -> {
            ItemStack stack = container.getSlot(0).getItem();
            templateProvider.ifPresent((ITemplateProvider provider) -> {
                ITemplateKey key = TemplateKeyHelper.getTemplateKey(stack);
                if (key != null) {
                    Player player = getMinecraft().player;
                    assert player != null;

                    BuildContext buildContext = BuildContext.builder()
                            .player(player)
                            .stack(stack)
                            .build(getWorld());
                    try {
                        Template template = provider.getTemplateForKey(key);
                        if (!nameField.getValue().isEmpty())
                            template = template.withName(nameField.getValue());
                        String json = TemplateIO.writeTemplateJson(template, buildContext);
                        getMinecraft().keyboardHandler.setClipboard(json);
                        player.displayClientMessage(MessageTranslation.CLIPBOARD_COPY_SUCCESS.componentTranslation().setStyle(Styles.DK_GREEN), false);
                    } catch (DataCannotBeWrittenException e) {
                        BuildingGadgets.LOG.error("Failed to write Template.", e);
                        player.displayClientMessage(MessageTranslation.CLIPBOARD_COPY_ERROR_TEMPLATE.componentTranslation().setStyle(Styles.RED), false);
                    } catch (Exception e) {
                        BuildingGadgets.LOG.error("Failed to copy Template to clipboard.", e);
                        player.displayClientMessage(MessageTranslation.CLIPBOARD_COPY_ERROR.componentTranslation().setStyle(Styles.RED), false);
                    }
                }
            });
        });
    }

    private void onPaste() {
        assert getMinecraft().player != null;

        String CBString = getMinecraft().keyboardHandler.getClipboard();
        if (GadgetUtils.mightBeLink(CBString)) {
            getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_FAILED_LINK_COPIED.componentTranslation().setStyle(Styles.RED), false);
            return;
        }

        try {
            CompoundTag tagFromJson = TagParser.parseTag(CBString);
            if (!tagFromJson.contains("header")) {
                BuildingGadgets.LOG.error("Attempted to use a 1.12 compound on a newer MC version");
                getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_FAILED_WRONG_MC_VERSION
                        .componentTranslation("(1.12.x)", TemplateHeader.LOWEST_MC_VERSION, TemplateHeader.HIGHEST_MC_VERSION).setStyle(Styles.RED), false);
                return;
            }
            if(!tagFromJson.contains("body")) {
                BuildingGadgets.LOG.error("Attempted to paste Material List as a template");
                getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_FAILED_MATERIAL_LIST
                        .componentTranslation().setStyle(Styles.RED), false);
                return;
            }
        } catch (CommandSyntaxException ignored) {
        }

        try {
            Template template = TemplateIO.readTemplateFromJson(CBString);
            Template readTemplate = template.clearMaterials();
            if (!nameField.getValue().isEmpty())
                readTemplate = readTemplate.withName(nameField.getValue());
            boolean replaced = replaceStack();
            ItemStack stack = container.getSlot(1).getItem();
            pasteTemplateToStack(getWorld(), stack, readTemplate, replaced);
            getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_SUCCESS.componentTranslation().setStyle(Styles.DK_GREEN), false);
        } catch (CorruptJsonException e) {
            BuildingGadgets.LOG.error("Failed to parse json syntax.", e);
            getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_FAILED_CORRUPT_JSON
                    .componentTranslation().setStyle(Styles.RED), false);
        } catch (IllegalMinecraftVersionException e) {
            BuildingGadgets.LOG.error("Attempted to parse Template for Minecraft version {} but expected between {} and {}.",
                    e.getMinecraftVersion(), TemplateHeader.LOWEST_MC_VERSION, TemplateHeader.HIGHEST_MC_VERSION, e);
            getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_FAILED_WRONG_MC_VERSION
                    .componentTranslation(e.getMinecraftVersion(), TemplateHeader.LOWEST_MC_VERSION, TemplateHeader.HIGHEST_MC_VERSION).setStyle(Styles.RED), false);
        } catch (UnknownTemplateVersionException e) {
            BuildingGadgets.LOG.error("Attempted to parse Template version {} but newest is {}.",
                    e.getTemplateVersion(), TemplateHeader.VERSION, e);
            getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_FAILED_TOO_RECENT_VERSION
                    .componentTranslation(e.getTemplateVersion(), TemplateHeader.VERSION).setStyle(Styles.RED), false);
        } catch (JsonParseException e) {
            BuildingGadgets.LOG.error("Failed to parse Template json.", e);
            getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_FAILED_INVALID_JSON
                    .componentTranslation().setStyle(Styles.RED), false);
        } catch (TemplateReadException e) {
            BuildingGadgets.LOG.error("Failed to read Template body.", e);
            getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_FAILED_CORRUPT_BODY
                    .componentTranslation().setStyle(Styles.RED), false);
        } catch (Exception e) {
            BuildingGadgets.LOG.error("Failed to paste Template.", e);
            getMinecraft().player.displayClientMessage(MessageTranslation.PASTE_FAILED
                    .componentTranslation().setStyle(Styles.RED), false);
        }
    }
}
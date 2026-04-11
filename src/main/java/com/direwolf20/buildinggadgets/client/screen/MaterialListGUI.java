package com.direwolf20.buildinggadgets.client.screen;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.template.*;
import com.direwolf20.buildinggadgets.common.util.TemplateKeyHelper;
import com.direwolf20.buildinggadgets.common.util.lang.MaterialListTranslation;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MaterialListGUI extends Screen implements ITemplateProvider.IUpdateListener {

    public static final int BUTTON_HEIGHT = 20;
    public static final int BUTTONS_PADDING = 4;

    public static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MODID, "textures/gui/material_list.png");
    public static final int BACKGROUND_WIDTH = 256;
    public static final int BACKGROUND_HEIGHT = 200;
    public static final int BORDER_SIZE = 4;

    public static final int WINDOW_WIDTH = BACKGROUND_WIDTH - BORDER_SIZE * 2;
    public static final int WINDOW_HEIGHT = BACKGROUND_HEIGHT - BORDER_SIZE * 2;

    private int backgroundX;
    private int backgroundY;
    private final ItemStack item;

    private String title;
    private int titleLeft;
    private int titleTop;

    private ScrollingMaterialList scrollingList;

    private Button buttonSortingModes;
    private Button buttonCopyList;

    private List<Component> hoveringText;
    private TemplateHeader header;

    public MaterialListGUI(ItemStack item) {
        super(MaterialListTranslation.TITLE.componentTranslation());
        Preconditions.checkArgument(TemplateKeyHelper.hasTemplateKey(item));
        this.item = item;
    }

    @Override
    public void init() {
        this.backgroundX = getXForAlignedCenter(0, width, BACKGROUND_WIDTH);
        this.backgroundY = getYForAlignedCenter(0, height, BACKGROUND_HEIGHT);

        header = evaluateTemplateHeader();
        evaluateTitle();

        this.scrollingList = new ScrollingMaterialList(this);
        // Make it receive mouse scroll events, so that the player can use his mouse wheel at the start
        this.setFocused(scrollingList);
        this.addRenderableWidget(scrollingList);

        int buttonY = getWindowBottomY() - (ScrollingMaterialList.BOTTOM / 2 + BUTTON_HEIGHT / 2);
        Button buttonClose = Button.builder(
                        MaterialListTranslation.BUTTON_CLOSE.componentTranslation(),
                        b -> Minecraft.getInstance().player.closeContainer()
                )
                .bounds(backgroundX + 10, buttonY, 60, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(buttonClose);
        this.buttonSortingModes = Button.builder(
                        scrollingList.getSortingMode().getTranslationProvider().componentTranslation(),
                        b -> {
                            scrollingList.setSortingMode(scrollingList.getSortingMode().next());
                            buttonSortingModes.setMessage(scrollingList.getSortingMode().getTranslationProvider().componentTranslation());
                        }
                )
                .bounds(backgroundX + 80, buttonY, 120, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.buttonSortingModes);

        this.buttonCopyList = Button.builder(
                        MaterialListTranslation.BUTTON_COPY.componentTranslation(),
                        b -> {
                            Minecraft.getInstance().keyboardHandler.setClipboard(getJson());
                            if (Minecraft.getInstance().player != null) {
                                Minecraft.getInstance().player.displayClientMessage(
                                        Component.translatable(MaterialListTranslation.MESSAGE_COPY_SUCCESS.getTranslationKey()),
                                        true
                                );
                            }
                        }
                )
                .bounds(backgroundX + 210, buttonY, 60, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.buttonCopyList);

        // Buttons will be placed left to right in this order
        this.addRenderableWidget(buttonSortingModes);
        this.addRenderableWidget(buttonCopyList);
        this.addRenderableWidget(buttonClose);

        this.calculateButtonsWidthAndX();
    }

    public TemplateHeader evaluateTemplateHeader() {
        Template template = getTemplateCapability();
        return template.getHeaderAndForceMaterials(getContext());
    }

    public String getJson() {
        return TemplateIO.GSON.toJson(new TemplateHeader.TemplateHeaderJsonRepresentation(getTemplateCapability(), getContext()));
    }

    private BuildContext getContext() {
        return BuildContext.builder()
                .player(Minecraft.getInstance().player)
                .stack(getTemplateItem())
                .build(Minecraft.getInstance().level);
    }

    public TemplateHeader getHeader() {
        return header;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float particleTicks) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, backgroundX, backgroundY, (float)0, (float)0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 256, 256);


        scrollingList.render(guiGraphics, mouseX, mouseY, particleTicks);
        guiGraphics.drawString(font, title, titleLeft, titleTop, Color.WHITE.getRGB());
        super.render(guiGraphics, mouseX, mouseY, particleTicks);

        if (buttonCopyList.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    this.font,
                    java.util.List.of(ClientTooltipComponent.create(MaterialListTranslation.HELP_COPY_LIST.componentTranslation().getVisualOrderText())),
                    mouseX,
                    mouseY,
                    DefaultTooltipPositioner.INSTANCE,
                    null
            );
        } else if (hoveringText != null) {
            guiGraphics.renderTooltip(
                    this.font,
                    hoveringText.stream().map(c -> ClientTooltipComponent.create(c.getVisualOrderText())).collect(java.util.stream.Collectors.toList()),
                    mouseX,
                    mouseY,
                    DefaultTooltipPositioner.INSTANCE,
                    null
            );
            hoveringText = null;
        }

    }

    private void calculateButtonsWidthAndX() {
        // This part would can create narrower buttons when there are too few of them, due to the vanilla button texture is 200 pixels wide
        int amountButtons = (int) children().stream().filter(e -> e instanceof Button).count();
        int amountMargins = amountButtons - 1;
        int totalMarginWidth = amountMargins * BUTTONS_PADDING;
        int usableWidth = getWindowWidth();
        int buttonWidth = (usableWidth - totalMarginWidth) / amountButtons;

        // Align the box of buttons in the center, and start from the left
        int nextX = getWindowLeftX();

        for (GuiEventListener widget : children()) {
            if (widget instanceof Button btn) {
                btn.setWidth(buttonWidth);
                btn.setX(nextX);
                nextX += buttonWidth + BUTTONS_PADDING;
            }
        }
    }

    public Template getTemplateCapability() {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null)
            return null;

        Optional<ITemplateProvider> providerCap = BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(minecraft.level);
        if (providerCap.isPresent()) {
            ITemplateKey key = TemplateKeyHelper.getTemplateKey(item);
            if (key != null) {
                ITemplateProvider provider = providerCap.get();
                provider.registerUpdateListener(this);
                return provider.getTemplateForKey(key);
            }
            BuildingGadgets.LOG.warn("Item used for material list does not have a template key!");
            minecraft.player.closeContainer();
            return null;
        }

        BuildingGadgets.LOG.warn("Client world used for material list does not have an ITemplateProvider component!");
        minecraft.player.closeContainer();
        return null;
    }

    public void setTaskHoveringText(int x, int y, List<Component> text) {
        hoveringText = text;
    }

    @Override
    public void onTemplateUpdate(ITemplateProvider provider, ITemplateKey key, Template template) {
        ITemplateKey itemKey = TemplateKeyHelper.getTemplateKey(item);
        if (itemKey != null) {
            UUID keyId = provider.getId(key);
            UUID itemId = provider.getId(itemKey);
            if (keyId.equals(itemId)) {
                header = evaluateTemplateHeader();
                evaluateTitle();
                scrollingList.reset();
            }
        }
    }

    private void evaluateTitle() {
        String name = getHeader().getName();
        String author = getHeader().getAuthor();

        this.title = name == null && author == null ? MaterialListTranslation.TITLE_EMPTY.format()
                : name == null ? MaterialListTranslation.TITLE_AUTHOR_ONLY.format(author)
                : author == null ? MaterialListTranslation.TITLE_NAME_ONLY.format(name)
                : MaterialListTranslation.TITLE.format(name, author);

        this.titleTop = getYForAlignedCenter(backgroundY, getWindowTopY() + ScrollingMaterialList.TOP, font.lineHeight);
        this.titleLeft = getXForAlignedCenter(backgroundX, getWindowRightX(), font.width(title));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public int getWindowLeftX() {
        return backgroundX + BORDER_SIZE;
    }

    public int getWindowRightX() {
        return backgroundX + BACKGROUND_WIDTH - BORDER_SIZE;
    }

    public int getWindowTopY() {
        return backgroundY + BORDER_SIZE;
    }

    public int getWindowBottomY() {
        return backgroundY + BACKGROUND_HEIGHT - BORDER_SIZE;
    }

    public int getWindowWidth() {
        return WINDOW_WIDTH;
    }

    public int getWindowHeight() {
        return WINDOW_HEIGHT;
    }

    public ItemStack getTemplateItem() {
        return item;
    }

    public static int getXForAlignedRight(int right, int width) {
        return right - width;
    }

    public static int getXForAlignedCenter(int left, int right, int width) {
        return left + (right - left) / 2 - width / 2;
    }

    public static int getYForAlignedCenter(int top, int bottom, int height) {
        return top + (bottom - top) / 2 - height / 2;
    }

    public static void renderTextVerticalCenter(GuiGraphics guiGraphics, String text, int leftX, int top, int bottom, int color) {
        Font font = Minecraft.getInstance().font;
        int y = getYForAlignedCenter(top, bottom, font.lineHeight);
        guiGraphics.drawString(font, text, leftX, y, color);
    }

    public static void renderTextHorizontalRight(GuiGraphics guiGraphics, String text, int right, int y, int color) {
        Font font = Minecraft.getInstance().font;
        int x = getXForAlignedRight(right, font.width(text));
        guiGraphics.drawString(font, text, x, y, color);
    }

    public static boolean isPointInBox(double x, double y, int bx, int by, int width, int height) {
        return x >= bx &&
               y >= by &&
               x < bx + width &&
               y < by + height;
    }
}

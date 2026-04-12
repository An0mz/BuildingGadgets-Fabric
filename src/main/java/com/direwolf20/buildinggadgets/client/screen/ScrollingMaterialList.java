package com.direwolf20.buildinggadgets.client.screen;

import com.direwolf20.buildinggadgets.client.screen.components.EntryList;
import com.direwolf20.buildinggadgets.common.tainted.inventory.IItemIndex;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.tainted.inventory.MatchResult;
import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.MaterialList;
import com.direwolf20.buildinggadgets.common.util.lang.ITranslationProvider;
import com.direwolf20.buildinggadgets.common.util.lang.MaterialListTranslation;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multiset;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.Comparator;
import java.util.Iterator;

import static com.direwolf20.buildinggadgets.client.screen.MaterialListGUI.*;
import static com.direwolf20.buildinggadgets.client.screen.ScrollingMaterialList.Entry;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

// Todo change to AbstractList as it's an easy fix compared to duping the class
class ScrollingMaterialList extends EntryList<Entry> {
    private static final int UPDATE_MILLIS = 1000;
    static final int TOP = 16;
    static final int BOTTOM = 32;

    private static final int SLOT_SIZE = 18;
    private static final int MARGIN = 2;
    private static final int ENTRY_HEIGHT = Math.max(SLOT_SIZE + MARGIN * 2, Minecraft.getInstance().font.lineHeight * 2 + MARGIN * 3);
    private static final int LINE_SIDE_MARGIN = 8;

    private final MaterialListGUI gui;

    private SortingModes sortingMode;
    private long lastUpdate;
    private Iterator<ImmutableMultiset<ItemVariant>> multisetIterator;

    public ScrollingMaterialList(MaterialListGUI gui) {
        super(gui.getWindowLeftX(), gui.getWindowTopY() + TOP, gui.getWindowWidth(), gui.getWindowHeight() - TOP - BOTTOM, ENTRY_HEIGHT);

        this.gui = gui;
        this.setSortingMode(SortingModes.NAME);

        updateEntries();
    }

    private void updateEntries() {
        this.lastUpdate = System.currentTimeMillis();
        this.clearEntries();

        if (multisetIterator == null || !multisetIterator.hasNext()) {
            MaterialList list = gui.getHeader().getRequiredItems();
            multisetIterator = list != null ? list.iterator() : Iterators.singletonIterator(ImmutableMultiset.of());
        }

        Player player = Minecraft.getInstance().player;

        // Could likely just assert
        if (player == null)
            return;

        IItemIndex index = InventoryHelper.index(gui.getTemplateItem(), player);
        MatchResult result;

        try (Transaction transaction = Transaction.openOuter()) {
            result = index.match(MaterialList.of(multisetIterator.next()), transaction);
        }

        for (Multiset.Entry<ItemVariant> entry : result.getChosenOption().entrySet()) {
            ItemVariant item = entry.getElement();
            addEntry(new Entry(this, item, entry.getCount(), result.getFoundItems().count(entry.getElement())));
        }

        sort();
    }

        protected int getScrollbarPosition() {
        return getX() - MARGIN - SCROLL_BAR_WIDTH;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_E) {
            assert Minecraft.getInstance().player != null;
            Minecraft.getInstance().player.closeContainer();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (lastUpdate + UPDATE_MILLIS < System.currentTimeMillis())
            updateEntries();

        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTicks);
    }

    public void reset() {
        multisetIterator = null;
    }

    static class Entry extends ObjectSelectionList.Entry<Entry> {

        private final ScrollingMaterialList parent;
        private final int required;
        private final int available;

        private final ItemStack stack;

        private final String itemName;
        private final String amount;

        private final int widthItemName;
        private final int widthAmount;

        public Entry(ScrollingMaterialList parent, ItemVariant item, int required, int available) {
            this.parent = parent;
            this.required = required;
            this.available = Mth.clamp(available, 0, required);

            this.stack = item.toStack();
            this.itemName = stack.getHoverName().getString();

            // Use this.available since the parameter is not clamped
            this.amount = this.available + "/" + required;
            this.widthItemName = Minecraft.getInstance().font.width(itemName);
            this.widthAmount = Minecraft.getInstance().font.width(amount);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int top, int left, boolean hovering, float delta) {
            renderContent(guiGraphics, top, left, hovering, delta);
        }

        public void renderContent(GuiGraphicsExtractor guiGraphics, int topY, int leftX, boolean hovered, float particleTicks) {
            int entryWidth = parent.getRowWidth();
            int entryHeight = ENTRY_HEIGHT;
            int right = leftX + entryWidth - MARGIN * 2;
            // Centralize entry vertically, for some reason this.getY() is not inclusive on the bottom
            int bottom = topY + entryHeight;

            int slotX = leftX + MARGIN;
            int slotY = topY + MARGIN;

            guiGraphics.item(stack, slotX, slotY);
            drawTextOverlay(guiGraphics, right, topY, bottom, slotX);
            int mouseX = (int)(Minecraft.getInstance().mouseHandler.xpos() / Minecraft.getInstance().getWindow().getGuiScale());
            int mouseY = (int)(Minecraft.getInstance().mouseHandler.ypos() / Minecraft.getInstance().getWindow().getGuiScale());
            drawHoveringText(stack, slotX, slotY, mouseX, mouseY);
        }

        private void drawTextOverlay(GuiGraphicsExtractor guiGraphics, int right, int top, int bottom, int slotX) {
            int itemNameX = slotX + SLOT_SIZE + MARGIN;
            renderTextVerticalCenter(guiGraphics, itemName, itemNameX, top, bottom, Color.WHITE.getRGB());
            int amountY = getYForAlignedCenter(top, bottom, Minecraft.getInstance().font.lineHeight);
            renderTextHorizontalRight(guiGraphics, amount, right, amountY, getTextColor());
            drawGuidingLine(guiGraphics, right, top, bottom, itemNameX, widthItemName, widthAmount);
        }


        private void drawGuidingLine(GuiGraphicsExtractor guiGraphics, int right, int top, int bottom, int itemNameX, int widthItemName, int widthAmount) {
            if (!isSelected()) {
                int lineXStart = itemNameX + widthItemName + LINE_SIDE_MARGIN;
                int lineXEnd = right - widthAmount - LINE_SIDE_MARGIN;
                int lineY = getYForAlignedCenter(top, bottom - 1, 1);

                int lineColor = 0x22FFFFFF;
                guiGraphics.fill(lineXStart, lineY, lineXEnd, lineY + 1, lineColor);
            }
        }


        private void drawHoveringText(ItemStack item, int slotX, int slotY, int mouseX, int mouseY) {
            if (isPointInBox(mouseX, mouseY, slotX, slotY, 18, 18)) {
                Minecraft mc = Minecraft.getInstance();

                Item.TooltipContext tooltipContext = Item.TooltipContext.of(mc.level);

                List tooltip = (List) item.getTooltipLines(
                        tooltipContext,
                        mc.player,
                        mc.options.advancedItemTooltips
                                ? TooltipFlag.ADVANCED
                                : TooltipFlag.NORMAL
                );

                parent.gui.setTaskHoveringText(mouseX, mouseY, (java.util.List<Component>) tooltip);
            }
        }




        private boolean hasEnoughItems() {
            return required == available;
        }

        private int getTextColor() {
            return hasEnoughItems() ? Color.GREEN.getRGB() : Color.RED.getRGB();
        }

        public int getRequired() {
            return required;
        }

        public int getAvailable() {
            return available;
        }

        public int getMissing() {
            return required - available;
        }

        public ItemStack getStack() {
            return stack;
        }

        public String getItemName() {
            return itemName;
        }

        public String getFormattedRequired() {
            int maxSize = stack.getMaxStackSize();
            int stacks = required / maxSize; // Integer division automatically floors
            int leftover = required % maxSize;
            if (stacks == 0)
                return String.valueOf(leftover);
            return stacks + "×" + maxSize + "+" + leftover;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean ingame) {
            return false;
        }

        public boolean isSelected() {
            return parent.getSelected() == this;
        }

        @Override
        public Component getNarration() {
            return null;
        }
    }

    public SortingModes getSortingMode() {
        return sortingMode;
    }

    public void setSortingMode(SortingModes sortingMode) {
        this.sortingMode = sortingMode;
        sort();
    }

    private void sort() {
        children().sort(sortingMode.getComparator());
    }

    enum SortingModes {

        NAME(Comparator.comparing(Entry::getItemName), MaterialListTranslation.BUTTON_SORTING_NAMEAZ),
        NAME_REVERSED(NAME.getComparator().reversed(), MaterialListTranslation.BUTTON_SORTING_NAMEZA),
        REQUIRED(Comparator.comparingInt(Entry::getRequired), MaterialListTranslation.BUTTON_SORTING_REQUIREDACSE),
        REQUIRED_REVERSED(REQUIRED.getComparator().reversed(), MaterialListTranslation.BUTTON_SORTING_MISSINGDESC),
        MISSING(Comparator.comparingInt(Entry::getMissing), MaterialListTranslation.BUTTON_SORTING_MISSINGACSE),
        MISSING_REVERSED(MISSING.getComparator().reversed(), MaterialListTranslation.BUTTON_SORTING_MISSINGDESC);

        private final Comparator<Entry> comparator;
        private final ITranslationProvider translationProvider;

        SortingModes(Comparator<Entry> comparator, ITranslationProvider provider) {
            this.comparator = comparator;
            this.translationProvider = provider;
        }

        public Comparator<Entry> getComparator() {
            return comparator;
        }

        public String getLocalizedName() {
            return translationProvider.format();
        }

        public ITranslationProvider getTranslationProvider() {
            return translationProvider;
        }

        public SortingModes next() {
            int nextIndex = ordinal() + 1;
            return VALUES[nextIndex >= VALUES.length ? 0 : nextIndex];
        }

        public static final SortingModes[] VALUES = SortingModes.values();

    }
}
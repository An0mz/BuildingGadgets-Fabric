package com.direwolf20.buildinggadgets.common.items;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.blocks.OurBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class OurItems {

    private static final ResourceKey<Item> BUILDING_GADGET_KEY = ResourceKey.create(Registries.ITEM, BuildingGadgets.id("gadget_building"));
    private static final ResourceKey<Item> EXCHANGING_GADGET_KEY = ResourceKey.create(Registries.ITEM, BuildingGadgets.id("gadget_exchanging"));
    private static final ResourceKey<Item> COPY_PASTE_GADGET_KEY = ResourceKey.create(Registries.ITEM, BuildingGadgets.id("gadget_copy_paste"));
    private static final ResourceKey<Item> DESTRUCTION_GADGET_KEY = ResourceKey.create(Registries.ITEM, BuildingGadgets.id("gadget_destruction"));
    private static final ResourceKey<Item> TEMPLATE_KEY = ResourceKey.create(Registries.ITEM, BuildingGadgets.id("template"));
    private static final ResourceKey<Item> TEMPLATE_MANAGER_ITEM_KEY = ResourceKey.create(Registries.ITEM, BuildingGadgets.id("template_manager"));

    // Gadgets
    public static final Item BUILDING_GADGET_ITEM = new GadgetBuilding(itemProperties(BUILDING_GADGET_KEY).stacksTo(1));
    public static final Item EXCHANGING_GADGET_ITEM = new GadgetExchanger(itemProperties(EXCHANGING_GADGET_KEY).stacksTo(1));
    public static final Item COPY_PASTE_GADGET_ITEM = new GadgetCopyPaste(itemProperties(COPY_PASTE_GADGET_KEY).stacksTo(1));
    public static final Item DESTRUCTION_GADGET_ITEM = new GadgetDestruction(itemProperties(DESTRUCTION_GADGET_KEY).stacksTo(1));

    // Template
    public static final Item TEMPLATE_ITEM = new TemplateItem(itemProperties(TEMPLATE_KEY).stacksTo(1));

    // Item Blocks
    public static final Item TEMPLATE_MANGER_ITEM = new BlockItem(OurBlocks.TEMPLATE_MANGER_BLOCK, itemProperties(TEMPLATE_MANAGER_ITEM_KEY));

    private static Item.Properties itemProperties(ResourceKey<Item> key) {
        return new Item.Properties().setId(key);
    }

    public static void registerItems() {
        Registry.register(BuiltInRegistries.ITEM, BUILDING_GADGET_KEY, BUILDING_GADGET_ITEM);
        Registry.register(BuiltInRegistries.ITEM, EXCHANGING_GADGET_KEY, EXCHANGING_GADGET_ITEM);
        Registry.register(BuiltInRegistries.ITEM, COPY_PASTE_GADGET_KEY, COPY_PASTE_GADGET_ITEM);
        Registry.register(BuiltInRegistries.ITEM, DESTRUCTION_GADGET_KEY, DESTRUCTION_GADGET_ITEM);
        Registry.register(BuiltInRegistries.ITEM, TEMPLATE_KEY, TEMPLATE_ITEM);
        Registry.register(BuiltInRegistries.ITEM, TEMPLATE_MANAGER_ITEM_KEY, TEMPLATE_MANGER_ITEM);
    }
}

package com.direwolf20.buildinggadgets.common.enchants;

import com.direwolf20.buildinggadgets.common.items.OurItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class ExchangeEnchantment extends Enchantment {

    public static final TagKey<Item> SUPPORTED = TagKey.create(
            BuiltInRegistries.ITEM.key(),                 // the registry key for items
            BuiltInRegistries.ITEM.getKey(OurItems.EXCHANGING_GADGET_ITEM) // the item's ResourceLocation
    );

    public ExchangeEnchantment() {
        super(definition(
                SUPPORTED,                   // supported items
                1,                           // weight
                1,                           // max level
                new Cost(1, 0),              // min cost
                new Cost(1, 0),              // max cost
                0,                           // anvil cost
                EquipmentSlot.MAINHAND
        ));
    }


    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.is(OurItems.EXCHANGING_GADGET_ITEM);
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canEnchant(stack);
    }

    public boolean isAllowedOnBooks() {
        return false;
    }
}

package com.direwolf20.buildinggadgets.common.enchants;

import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class GadgetSilkTouch {

    // ResourceKey to reference the enchantment
    public static final ResourceKey<Enchantment> GADGET_SILKTOUCH_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, new ResourceLocation(Reference.MODID, "gadget_silktouch"));

    /**
     * Check if an ItemStack has the Gadget Silk Touch enchantment
     */
    public static boolean hasSilkTouch(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return EnchantmentHelper.getItemEnchantmentLevel(getEnchantment(), stack) > 0;
    }

    /**
     * Get the level of Gadget Silk Touch on an ItemStack
     */
    public static int getSilkTouchLevel(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return EnchantmentHelper.getItemEnchantmentLevel(getEnchantment(), stack);
    }

    /**
     * Get the Enchantment instance (nullable - returns null if not registered)
     */
    public static Enchantment getEnchantment() {
        return BuiltInRegistries.ENCHANTMENT.get(GADGET_SILKTOUCH_KEY);
    }
}
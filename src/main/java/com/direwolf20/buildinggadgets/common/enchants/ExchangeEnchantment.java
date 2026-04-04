package com.direwolf20.buildinggadgets.common.enchants;

import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * In 1.21.1, enchantments are fully data-driven.
 * The actual enchantment definition is in data/buildinggadgets/enchantment/exchange.json.
 * This class just holds the ResourceKey reference for use in code.
 */
public class ExchangeEnchantment {

    public static final ResourceKey<Enchantment> EXCHANGE_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Reference.MODID, "exchange"));

    public static boolean hasExchange(ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        if (stack.isEmpty()) return false;
        return registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(EXCHANGE_KEY)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0)
                .orElse(false);
    }
}

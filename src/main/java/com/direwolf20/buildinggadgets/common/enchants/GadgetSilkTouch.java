package com.direwolf20.buildinggadgets.common.enchants;

import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Optional;

public class GadgetSilkTouch {

    public static final ResourceKey<Enchantment> GADGET_SILKTOUCH_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Reference.MODID, "gadget_silktouch"));

    public static boolean hasSilkTouch(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return false;
        Optional<Holder.Reference<Enchantment>> holder = registries
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(GADGET_SILKTOUCH_KEY);
        return holder.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack) > 0).orElse(false);
    }

    public static int getSilkTouchLevel(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return 0;
        Optional<Holder.Reference<Enchantment>> holder = registries
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(GADGET_SILKTOUCH_KEY);
        return holder.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
    }
}

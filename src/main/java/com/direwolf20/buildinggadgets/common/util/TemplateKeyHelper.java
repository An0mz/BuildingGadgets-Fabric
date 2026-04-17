package com.direwolf20.buildinggadgets.common.util;

import com.direwolf20.buildinggadgets.common.component.BGDataComponents;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.tainted.template.SimpleTemplateKey;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TemplateKeyHelper {

    @Nullable
    public static ITemplateKey getTemplateKey(ItemStack stack) {
        UUID keyId = stack.get(BGDataComponents.TEMPLATE_KEY);
        if (keyId == null) {
            return null;
        }
        return new SimpleTemplateKey(keyId);
    }

    public static boolean hasTemplateKey(ItemStack stack) {
        return stack.has(BGDataComponents.TEMPLATE_KEY);
    }

    public static void setTemplateKey(ItemStack stack, UUID keyId) {
        stack.set(BGDataComponents.TEMPLATE_KEY, keyId);
    }

    public static void initializeTemplateKey(ItemStack stack) {
        if (stack.get(BGDataComponents.TEMPLATE_KEY) == null) {
            stack.set(BGDataComponents.TEMPLATE_KEY, UUID.randomUUID());
        }
    }
}

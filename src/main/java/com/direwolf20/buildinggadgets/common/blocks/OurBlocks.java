package com.direwolf20.buildinggadgets.common.blocks;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public final class OurBlocks {
    private OurBlocks() {
    }

    public static void registerBlocks() {
        Registry.register(BuiltInRegistries.BLOCK, BuildingGadgets.id("effect_block"), EFFECT_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, BuildingGadgets.id("template_manager"), TEMPLATE_MANGER_BLOCK);
    }

    public static final Block EFFECT_BLOCK = new EffectBlock();
    public static final Block TEMPLATE_MANGER_BLOCK = new TemplateManager();
}

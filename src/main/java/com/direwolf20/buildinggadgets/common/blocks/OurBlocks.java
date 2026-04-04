package com.direwolf20.buildinggadgets.common.blocks;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public final class OurBlocks {
    private OurBlocks() {
    }

    private static final ResourceKey<Block> EFFECT_BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK, BuildingGadgets.id("effect_block"));
    private static final ResourceKey<Block> TEMPLATE_MANAGER_KEY = ResourceKey.create(
            Registries.BLOCK, BuildingGadgets.id("template_manager"));

    public static final Block EFFECT_BLOCK = new EffectBlock(
            Block.Properties.of()
                    .setId(EFFECT_BLOCK_KEY)
                    .strength(20f)
                    .noCollission()
                    .noLootTable()
    );
    public static final Block TEMPLATE_MANGER_BLOCK = new TemplateManager(
            Block.Properties.of()
                    .setId(TEMPLATE_MANAGER_KEY)
                    .strength(2f)
    );

    public static void registerBlocks() {
        Registry.register(BuiltInRegistries.BLOCK, EFFECT_BLOCK_KEY, EFFECT_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, TEMPLATE_MANAGER_KEY, TEMPLATE_MANGER_BLOCK);
    }
}

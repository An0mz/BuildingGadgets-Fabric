package com.direwolf20.buildinggadgets.common;

import com.direwolf20.buildinggadgets.client.OurSounds;
import com.direwolf20.buildinggadgets.common.blocks.OurBlocks;
import com.direwolf20.buildinggadgets.common.commands.ForceUnloadedCommand;
import com.direwolf20.buildinggadgets.common.commands.OverrideBuildSizeCommand;
import com.direwolf20.buildinggadgets.common.commands.OverrideCopySizeCommand;
import com.direwolf20.buildinggadgets.common.compat.FLANCompat;
import com.direwolf20.buildinggadgets.common.compat.FTBChunksCompat;
import com.direwolf20.buildinggadgets.common.component.BGDataComponents;
import com.direwolf20.buildinggadgets.common.config.Config;
import com.direwolf20.buildinggadgets.common.containers.OurContainers;
import com.direwolf20.buildinggadgets.common.containers.TemplateManagerContainer;
import com.direwolf20.buildinggadgets.common.items.OurItems;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.tainted.registry.Registries;
import com.direwolf20.buildinggadgets.common.tileentities.OurTileEntities;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.direwolf20.buildinggadgets.common.util.TemplateKeyHelper;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BuildingGadgets implements ModInitializer {

    public static final Logger LOG = LogManager.getLogger();

    public static final CreativeModeTab CREATIVE_TAB = FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Reference.MODID))
            .icon(() -> {
                ItemStack stack = new ItemStack(OurItems.BUILDING_GADGET_ITEM);
                // Set energy using our DataComponent directly
                stack.set(BGDataComponents.ENERGY, getConfig().gadgets.gadgetBuilding.maxEnergy);
                return stack;
            })
            .displayItems((parameters, output) -> {
                // Building Gadget - empty and charged
                output.accept(OurItems.BUILDING_GADGET_ITEM);
                ItemStack buildingGadgetCharged = new ItemStack(OurItems.BUILDING_GADGET_ITEM);
                buildingGadgetCharged.set(BGDataComponents.ENERGY, getConfig().gadgets.gadgetBuilding.maxEnergy);
                output.accept(buildingGadgetCharged);

                // Exchanging Gadget - empty and charged
                output.accept(OurItems.EXCHANGING_GADGET_ITEM);
                ItemStack exchangingGadgetCharged = new ItemStack(OurItems.EXCHANGING_GADGET_ITEM);
                exchangingGadgetCharged.set(BGDataComponents.ENERGY, getConfig().gadgets.gadgetExchanger.maxEnergy);
                output.accept(exchangingGadgetCharged);

                // Copy Paste Gadget - empty and charged
                output.accept(OurItems.COPY_PASTE_GADGET_ITEM);
                ItemStack copyPasteGadgetCharged = new ItemStack(OurItems.COPY_PASTE_GADGET_ITEM);
                copyPasteGadgetCharged.set(BGDataComponents.ENERGY, getConfig().gadgets.gadgetCopyPaste.maxEnergy);
                // Initialize template key for copy/paste gadget
                TemplateKeyHelper.initializeTemplateKey(copyPasteGadgetCharged);
                output.accept(copyPasteGadgetCharged);

                // Destruction Gadget - empty and charged
                output.accept(OurItems.DESTRUCTION_GADGET_ITEM);
                ItemStack destructionGadgetCharged = new ItemStack(OurItems.DESTRUCTION_GADGET_ITEM);
                destructionGadgetCharged.set(BGDataComponents.ENERGY, getConfig().gadgets.gadgetDestruction.maxEnergy);
                output.accept(destructionGadgetCharged);

                // Add non-energy items
                ItemStack templateItem = new ItemStack(OurItems.TEMPLATE_ITEM);
                // Initialize template key for template item
                TemplateKeyHelper.initializeTemplateKey(templateItem);
                output.accept(templateItem);

                output.accept(OurItems.TEMPLATE_MANGER_ITEM);
            })
            .build();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Reference.MODID, path);
    }

    public static Config getConfig() {
        return AutoConfig.getConfigHolder(Config.class).getConfig();
    }

    @Override
    public void onInitialize() {
        AutoConfig.register(Config.class, GsonConfigSerializer::new);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("tab"), CREATIVE_TAB);
        OurBlocks.registerBlocks();
        OurItems.registerItems();
        BGDataComponents.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> server.getCommands().getDispatcher()
                .register(Commands.literal(Reference.MODID)
                        .then(OverrideBuildSizeCommand.registerToggle())
                        .then(OverrideCopySizeCommand.registerToggle())
                        .then(ForceUnloadedCommand.registerToggle())
                        .then(OverrideBuildSizeCommand.registerList())
                        .then(OverrideCopySizeCommand.registerList())
                        .then(ForceUnloadedCommand.registerList())));

        Registries.registerTileDataSerializers();
        PacketHandler.registerMessages();
        OurSounds.initSounds();
        OurTileEntities.initBE();
        OurContainers.TEMPLATE_MANAGER_CONTAINER_TYPE = Registry.register(
                BuiltInRegistries.MENU,
                BuildingGadgets.id("template_manager_container"),
                new ExtendedMenuType<>(
                        TemplateManagerContainer::new,
                        BlockPos.STREAM_CODEC
                )
        );

        FLANCompat.MOD_LOADED = FabricLoader.getInstance().isModLoaded("flan");
        FTBChunksCompat.MOD_LOADED = FabricLoader.getInstance().isModLoaded("ftbchunks");
    }
}

package com.direwolf20.buildinggadgets.common;

import com.direwolf20.buildinggadgets.client.OurSounds;
import com.direwolf20.buildinggadgets.common.blocks.OurBlocks;
import com.direwolf20.buildinggadgets.common.commands.ForceUnloadedCommand;
import com.direwolf20.buildinggadgets.common.commands.OverrideBuildSizeCommand;
import com.direwolf20.buildinggadgets.common.commands.OverrideCopySizeCommand;
import com.direwolf20.buildinggadgets.common.compat.FLANCompat;
import com.direwolf20.buildinggadgets.common.compat.FTBChunksCompat;
//import com.direwolf20.buildinggadgets.common.compat.GOMLCompat;
import com.direwolf20.buildinggadgets.common.config.Config;
import com.direwolf20.buildinggadgets.common.containers.OurContainers;
import com.direwolf20.buildinggadgets.common.containers.TemplateManagerContainer;
import com.direwolf20.buildinggadgets.common.enchants.GadgetSilkTouch;
import com.direwolf20.buildinggadgets.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.items.OurItems;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.tainted.registry.Registries;
import com.direwolf20.buildinggadgets.common.tileentities.OurTileEntities;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyItem;

public final class BuildingGadgets implements ModInitializer {

    public static final Logger LOG = LogManager.getLogger();

    /**
     * Register our creative tab. Notice that we're also modifying the NBT data of the
     * building gadget to remove the damage / energy indicator from the creative
     * tabs icon.
     */
    public static final CreativeModeTab CREATIVE_TAB = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup." + Reference.MODID))
            .icon(() -> {
                ItemStack stack = new ItemStack(OurItems.BUILDING_GADGET_ITEM);
                SimpleEnergyItem.setStoredEnergyUnchecked(stack, getConfig().gadgets.gadgetBuilding.maxEnergy);
                return stack;
            })
            .build();

    public static ResourceLocation id(String path) {
        return new ResourceLocation(Reference.MODID, path);
    }

    public static Config getConfig() {
        return AutoConfig.getConfigHolder(Config.class).getConfig();
    }

    @Override
    public void onInitialize() {
        AutoConfig.register(Config.class, GsonConfigSerializer::new);
        net.minecraft.core.Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("tab"), CREATIVE_TAB);
        OurBlocks.registerBlocks();
        OurItems.registerItems();

        ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), id("tab"))).register(entries -> {
            // Building Gadget - empty and charged
            entries.accept(OurItems.BUILDING_GADGET_ITEM);
            ItemStack buildingGadgetCharged = new ItemStack(OurItems.BUILDING_GADGET_ITEM);
            SimpleEnergyItem.setStoredEnergyUnchecked(buildingGadgetCharged, getConfig().gadgets.gadgetBuilding.maxEnergy);
            entries.accept(buildingGadgetCharged);

            // Exchanging Gadget - empty and charged
            entries.accept(OurItems.EXCHANGING_GADGET_ITEM);
            ItemStack exchangingGadgetCharged = new ItemStack(OurItems.EXCHANGING_GADGET_ITEM);
            SimpleEnergyItem.setStoredEnergyUnchecked(exchangingGadgetCharged, getConfig().gadgets.gadgetExchanger.maxEnergy);
            entries.accept(exchangingGadgetCharged);

            // Copy Paste Gadget - empty and charged
            entries.accept(OurItems.COPY_PASTE_GADGET_ITEM);
            ItemStack copyPasteGadgetCharged = new ItemStack(OurItems.COPY_PASTE_GADGET_ITEM);
            SimpleEnergyItem.setStoredEnergyUnchecked(copyPasteGadgetCharged, getConfig().gadgets.gadgetCopyPaste.maxEnergy);
            entries.accept(copyPasteGadgetCharged);

            // Destruction Gadget - empty and charged
            entries.accept(OurItems.DESTRUCTION_GADGET_ITEM);
            ItemStack destructionGadgetCharged = new ItemStack(OurItems.DESTRUCTION_GADGET_ITEM);
            SimpleEnergyItem.setStoredEnergyUnchecked(destructionGadgetCharged, getConfig().gadgets.gadgetDestruction.maxEnergy);
            entries.accept(destructionGadgetCharged);

            // Add non-energy items
            entries.accept(OurItems.TEMPLATE_ITEM);
            entries.accept(OurItems.TEMPLATE_MANGER_ITEM);
        });

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
        OurContainers.TEMPLATE_MANAGER_CONTAINER_TYPE = Registry.register(BuiltInRegistries.MENU, BuildingGadgets.id("template_manager_container"), new ExtendedScreenHandlerType<>(TemplateManagerContainer::new));

        Registry.register(BuiltInRegistries.ENCHANTMENT, id("silk_touch"), GadgetSilkTouch.GADGET_SILKTOUCH);

        //GOMLCompat.MOD_LOADED = FabricLoader.getInstance().isModLoaded("goml");
        FLANCompat.MOD_LOADED = FabricLoader.getInstance().isModLoaded("flan");
        FTBChunksCompat.MOD_LOADED = FabricLoader.getInstance().isModLoaded("ftbchunks");
    }
}

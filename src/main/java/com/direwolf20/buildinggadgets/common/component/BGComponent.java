package com.direwolf20.buildinggadgets.common.component;

import com.direwolf20.buildinggadgets.client.BuildingGadgetsClient;
import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.compat.NoWorldCompat;
import com.direwolf20.buildinggadgets.common.items.OurItems;
import com.direwolf20.buildinggadgets.common.tainted.save.SaveTemplateProvider;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.item.ItemComponentInitializer;
import org.ladysnake.cca.api.v3.item.ItemComponentMigrationRegistry;
import org.ladysnake.cca.api.v3.level.LevelComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.level.LevelComponentInitializer;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

public class BGComponent implements ItemComponentInitializer, WorldComponentInitializer, LevelComponentInitializer {

    public static final ComponentKey<ITemplateProvider> TEMPLATE_PROVIDER_COMPONENT = ComponentRegistryV3.INSTANCE.getOrCreate(BuildingGadgets.id("template_provider"), ITemplateProvider.class);
    public static final ComponentKey<ITemplateKey> TEMPLATE_KEY_COMPONENT = ComponentRegistryV3.INSTANCE.getOrCreate(BuildingGadgets.id("template_key"), ITemplateKey.class);
    public static final ComponentKey<UndoService> UNDO_COMPONENT = ComponentRegistryV3.INSTANCE.getOrCreate(BuildingGadgets.id("undo"), UndoService.class);

    @Override
    public void registerItemComponentMigrations(ItemComponentMigrationRegistry registry) {
        // no migrations yet, leave empty
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
        registry.register(TEMPLATE_PROVIDER_COMPONENT, world -> {
            if (world instanceof ServerLevel) {
                return new SaveTemplateProvider();
            }
            else if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT && world instanceof ClientLevel){
                return BuildingGadgetsClient.CACHE_TEMPLATE_PROVIDER;
            }
            else return new NoWorldCompat();
        });
    }

    @Override
    public void registerLevelComponentFactories(LevelComponentFactoryRegistry registry) {
        registry.register(UNDO_COMPONENT, levelData -> new UndoService());
    }
}
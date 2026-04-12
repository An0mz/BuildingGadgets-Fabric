package com.direwolf20.buildinggadgets.common.component;

import com.direwolf20.buildinggadgets.client.BuildingGadgetsClient;
import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.tainted.save.SaveTemplateProvider;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v8.level.LevelComponentFactoryRegistry;
import org.ladysnake.cca.api.v8.level.LevelComponentInitializer;

public class BGComponent implements LevelComponentInitializer {

    public static final ComponentKey<ITemplateProvider> TEMPLATE_PROVIDER_COMPONENT =
            ComponentRegistry.getOrCreate(BuildingGadgets.id("template_provider"), ITemplateProvider.class);
    public static final ComponentKey<UndoService> UNDO_COMPONENT =
            ComponentRegistry.getOrCreate(BuildingGadgets.id("undo"), UndoService.class);

    @Override
    public void registerLevelComponentFactories(LevelComponentFactoryRegistry registry) {
        registry.register(TEMPLATE_PROVIDER_COMPONENT, level -> {
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                return BuildingGadgetsClient.CACHE_TEMPLATE_PROVIDER;
            } else {
                return new SaveTemplateProvider();
            }
        });
        registry.register(UNDO_COMPONENT, level -> new UndoService());
    }
}

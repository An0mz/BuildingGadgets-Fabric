package com.direwolf20.buildinggadgets.common.compat;

import com.direwolf20.buildinggadgets.common.config.Config;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;

public class ModMenuCompat implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfigClient.getConfigScreen(Config.class, parent).get();
    }
}

package com.mtxCore.mod_menu_filter.client;

import com.mtxCore.mod_menu_filter.client.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;

public class ModMenuFilterClient implements ClientModInitializer {

    public static final String MOD_ID = "mod_menu_filter";

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        if (ConfigManager.syncWithModMenu && ConfigManager.filterEnabled) {
            ConfigManager.syncToModMenu();
        }
    }
}
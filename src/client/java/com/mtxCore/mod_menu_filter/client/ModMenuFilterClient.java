package com.mtxCore.mod_menu_filter.client;

import net.fabricmc.api.ClientModInitializer;
import com.mtxCore.mod_menu_filter.client.config.ConfigManager;

public class ModMenuFilterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        // Call config initialization
        ConfigManager.load();

    }
}
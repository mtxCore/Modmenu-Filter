package com.mtxCore.mod_menu_filter.client;

import com.mtxCore.mod_menu_filter.client.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;

public class ModMenuFilterClient implements ClientModInitializer {

    public static final String MOD_ID = "mod_menu_filter";

    @Override
    public void onInitializeClient() {
        // Load config — this also initialises the tag database,
        // applies user overrides, and sets up colours.
        ConfigManager.load();

        // Push the computed hidden-mods list into Mod Menu's own config
        // so filtering is active immediately on startup.
        if (ConfigManager.syncWithModMenu && ConfigManager.filterEnabled) {
            ConfigManager.syncToModMenu();
        }
    }
}
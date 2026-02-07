package com.mtxCore.mod_menu_filter.client;

import com.mtxCore.mod_menu_filter.client.config.ConfigManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("title.mod_menu_filter.config"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Component.translatable("category.mod_menu_filter.general"));

            // Boolean Toggle for "Filter Enabled"
            general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.mod_menu_filter.enabled"), ConfigManager.isFilterEnabled())
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> ConfigManager.filterEnabled = newValue)
                    .build());

            // String List for "Excluded Mods"
            general.addEntry(entryBuilder.startStrList(Component.translatable("option.mod_menu_filter.excluded"), ConfigManager.excludedMods)
                    .setDefaultValue(new ArrayList<>())
                    .setSaveConsumer(newList -> ConfigManager.excludedMods = newList)
                    .build());


            builder.setSavingRunnable(ConfigManager::save);

            return builder.build();
        };
    }
}
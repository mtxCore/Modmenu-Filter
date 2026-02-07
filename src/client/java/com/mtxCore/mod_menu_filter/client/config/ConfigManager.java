package com.mtxCore.mod_menu_filter.client.config;


// Imports
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigManager {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("mod_menu_filter.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static List<String> excludedMods = new ArrayList<>();
    public static Boolean filterEnabled;


    public static void load() {
        File file = PATH.toFile();
        if (!file.exists()) {
            saveDefault();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null && data.excludedMods != null) {
                excludedMods = new ArrayList<>(Arrays.asList(data.excludedMods));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveDefault() {
        try (FileWriter writer = new FileWriter(PATH.toFile())) {
            GSON.toJson(new ConfigData(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(PATH.toFile())) {
            ConfigData data = new ConfigData();
            data.excludedMods = excludedMods.toArray(new String[0]);
            data.isFilterEnabled = filterEnabled;
            GSON.toJson(data, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static Boolean isFilterEnabled() {
        File file = PATH.toFile();
        if (!file.exists()) {
            saveDefault();
            return true; // Default to true if config doesn't exist
        }

        try (FileReader reader = new FileReader(file)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            return data != null && data.isFilterEnabled != null ? data.isFilterEnabled : true;
        } catch (IOException e) {
            e.printStackTrace();
            return true; // Default to true on error
        }
    }

    // Config structure
    private static class ConfigData {
        String[] excludedMods = {"example-mod-id"};
        Boolean isFilterEnabled = true;
    }
}
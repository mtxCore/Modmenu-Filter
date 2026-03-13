package com.mtxCore.mod_menu_filter.client.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Reads and writes ModMenu's own config file ({@code config/modmenu.json}).
 * <p>
 * Only the {@code hidden_mods} key is modified; every other setting is
 * preserved verbatim so we stay fully compatible with Mod Menu.
 */
public class ModMenuConfigWriter {

    private static final Path MODMENU_CONFIG =
            FabricLoader.getInstance().getConfigDir().resolve("modmenu.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static List<String> readHiddenMods() {
        File file = MODMENU_CONFIG.toFile();
        if (!file.exists()) return new ArrayList<>();

        try (FileReader reader = new FileReader(file)) {
            JsonObject config = GSON.fromJson(reader, JsonObject.class);
            if (config == null || !config.has("hidden_mods")) return new ArrayList<>();

            JsonArray arr = config.getAsJsonArray("hidden_mods");
            List<String> result = new ArrayList<>();
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) result.add(el.getAsString());
            }
            return result;
        } catch (Exception e) {
            System.err.println("[ModMenuFilter] Failed to parse ModMenu config: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Updates the 'hidden_mods' field in ModMenu's config.
     * We manipulate the JsonObject directly to ensure we don't accidentally 
     * strip other ModMenu settings when saving our filtered list.
     */
    public static void writeHiddenMods(Collection<String> hiddenMods) {
        File file = MODMENU_CONFIG.toFile();
        JsonObject config;

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = GSON.fromJson(reader, JsonObject.class);
                if (config == null) config = new JsonObject();
            } catch (Exception e) {
                config = new JsonObject();
            }
        } else {
            config = new JsonObject();
        }

        JsonArray arr = new JsonArray();
        new TreeSet<>(hiddenMods).forEach(arr::add);
        config.add("hidden_mods", arr);

        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("[ModMenuFilter] Failed to write ModMenu config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void addHiddenMods(Collection<String> modIds) {
        Set<String> combined = new LinkedHashSet<>(readHiddenMods());
        combined.addAll(modIds);
        writeHiddenMods(combined);
    }

    public static void removeHiddenMods(Collection<String> modIds) {
        List<String> current = readHiddenMods();
        current.removeAll(new HashSet<>(modIds));
        writeHiddenMods(current);
    }
}

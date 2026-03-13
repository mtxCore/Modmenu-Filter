package com.mtxCore.mod_menu_filter.client.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads the bundled tag database (default_tags.json) and manages
 * tag ↔ mod associations at runtime.  Supports user overrides and
 * custom tags applied on top of the defaults.
 */
public class TagDatabase {

    private static final Map<String, Set<String>> MOD_TO_TAGS = new HashMap<>();
    private static final Map<String, Set<String>> TAG_TO_MODS = new HashMap<>();
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        loaded = true;
        loadDefaults();
    }

    private static void loadDefaults() {
        try (InputStream is = TagDatabase.class.getResourceAsStream("/assets/mod_menu_filter/default_tags.json")) {
            if (is == null) {
                // Not using a logger to avoid additional dependencies for a client mod.
                System.err.println("[ModMenuFilter] Missing default_tags.json resource.");
                return;
            }

            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> data = gson.fromJson(new InputStreamReader(is), type);

            if (data == null) return;

            for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                String tagId = entry.getKey();
                ModTag.getOrCreate(tagId);

                Set<String> mods = new LinkedHashSet<>(entry.getValue());
                TAG_TO_MODS.put(tagId, mods);

                for (String modId : mods) {
                    MOD_TO_TAGS.computeIfAbsent(modId, k -> new LinkedHashSet<>()).add(tagId);
                }
            }
        } catch (Exception e) {
            System.err.println("[ModMenuFilter] Failed to load tag database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Overwrites tag assignments for specific mods.
     * We clear old associations first to give users total control over 
     * how a mod is categorized in their UI.
     */
    public static void applyOverrides(Map<String, List<String>> overrides) {
        if (overrides == null) return;

        for (Map.Entry<String, List<String>> entry : overrides.entrySet()) {
            String modId = entry.getKey();
            List<String> tags = entry.getValue();

            Set<String> oldTags = MOD_TO_TAGS.remove(modId);
            if (oldTags != null) {
                for (String oldTag : oldTags) {
                    Set<String> mods = TAG_TO_MODS.get(oldTag);
                    if (mods != null) mods.remove(modId);
                }
            }

            Set<String> newTags = new LinkedHashSet<>(tags);
            MOD_TO_TAGS.put(modId, newTags);
            for (String tag : newTags) {
                ModTag.getOrCreate(tag);
                TAG_TO_MODS.computeIfAbsent(tag, k -> new LinkedHashSet<>()).add(modId);
            }
        }
    }

    /**
     * Integrates user-created tags. Custom tags are merged with 
     * defaults to allow for additive categorization.
     */
    public static void applyCustomTags(Map<String, ConfigManager.CustomTagData> customTags) {
        if (customTags == null) return;

        for (Map.Entry<String, ConfigManager.CustomTagData> entry : customTags.entrySet()) {
            String tagId = entry.getKey();
            ConfigManager.CustomTagData data = entry.getValue();

            ModTag tag = ModTag.getOrCreate(tagId);
            tag.setColor(data.color);

            if (data.mods != null) {
                Set<String> modSet = new LinkedHashSet<>(data.mods);
                TAG_TO_MODS.computeIfAbsent(tagId, k -> new LinkedHashSet<>()).addAll(modSet);
                for (String modId : modSet) {
                    MOD_TO_TAGS.computeIfAbsent(modId, k -> new LinkedHashSet<>()).add(tagId);
                }
            }
        }
    }

    public static void applyTagColors(Map<String, Integer> colors) {
        if (colors == null) return;
        for (Map.Entry<String, Integer> entry : colors.entrySet()) {
            ModTag tag = ModTag.get(entry.getKey());
            if (tag != null) tag.setColor(entry.getValue());
        }
    }

    public static List<ModTag> getTagsForMod(String modId) {
        Set<String> tagIds = MOD_TO_TAGS.get(modId);
        if (tagIds == null || tagIds.isEmpty()) return Collections.emptyList();
        return tagIds.stream()
                .map(ModTag::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public static Set<String> getModsForTag(String tagId) {
        return TAG_TO_MODS.getOrDefault(tagId, Collections.emptySet());
    }

    public static Set<String> getAllTagIds() {
        return TAG_TO_MODS.keySet();
    }

    public static Set<String> getAllCategorizedModIds() {
        return MOD_TO_TAGS.keySet();
    }

    public static boolean hasTag(String modId, String tagId) {
        Set<String> tags = MOD_TO_TAGS.get(modId);
        return tags != null && tags.contains(tagId);
    }

    public static void reload() {
        MOD_TO_TAGS.clear();
        TAG_TO_MODS.clear();
        loaded = false;
        load();
    }
}

package com.mtxCore.mod_menu_filter.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central config for Mod Menu Filter.
 * <p>
 * Our own settings live in {@code config/mod_menu_filter.json}.
 * Hidden-mod data is synced into Mod Menu's native {@code hidden_mods}
 * via {@link ModMenuConfigWriter} so the two never conflict.
 */
public class ConfigManager {

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("mod_menu_filter.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Cloth Config captures these fields directly via reference.
    // Making them public allows for cleaner integration without boilerplate getters/setters.

    public static boolean filterEnabled       = true;
    public static boolean syncWithModMenu     = true;
    public static boolean showTagBadges       = true;
    public static boolean autoHideLibraries   = false;
    public static boolean dependencyProtection = true;
    public static boolean compactBadges       = false;

    /** Transient – active tag filter toggles (not persisted to disk). */
    public static final java.util.Set<String> activeTagFilters = new java.util.HashSet<>();

    public static List<String> hiddenMods   = new ArrayList<>();
    public static List<String> favoriteMods = new ArrayList<>();
    public static List<String> hiddenTags   = new ArrayList<>();
    public static List<String> hidePatterns = new ArrayList<>();

    public static Map<String, List<String>>   tagOverrides = new HashMap<>();
    public static Map<String, CustomTagData>  customTags   = new HashMap<>();
    public static Map<String, Integer>        tagColors    = new HashMap<>();

    public static String                       activeProfile = "default";
    public static Map<String, ProfileData>     profiles      = new HashMap<>();

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            initDefaults();
            save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            @org.jetbrains.annotations.Nullable ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                applyFromData(data);
            } else {
                initDefaults();
            }
        } catch (Exception e) {
            System.err.println("[ModMenuFilter] Error loading config: " + e.getMessage());
            e.printStackTrace();
            initDefaults();
        }

        // TagDatabase must be refreshed post-load to ensure 
        // user overrides take precedence over hardcoded defaults.
        TagDatabase.load();
        TagDatabase.applyOverrides(tagOverrides);
        TagDatabase.applyCustomTags(customTags);
        TagDatabase.applyTagColors(tagColors);
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.filterEnabled       = filterEnabled;
        data.syncWithModMenu     = syncWithModMenu;
        data.showTagBadges       = showTagBadges;
        data.autoHideLibraries   = autoHideLibraries;
        data.dependencyProtection = dependencyProtection;
        data.compactBadges       = compactBadges;
        data.hiddenMods   = hiddenMods.toArray(new String[0]);
        data.favoriteMods = favoriteMods.toArray(new String[0]);
        data.hiddenTags   = hiddenTags.toArray(new String[0]);
        data.hidePatterns = hidePatterns.toArray(new String[0]);
        data.tagOverrides = tagOverrides;
        data.customTags   = customTags;
        data.tagColors    = tagColors;
        data.activeProfile = activeProfile;
        data.profiles      = profiles;

        try {
            CONFIG_PATH.getParent().toFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            System.err.println("[ModMenuFilter] Error saving config: " + e.getMessage());
            e.printStackTrace();
        }

        // We push to ModMenu's config on every save for immediate UX feedback.
        if (syncWithModMenu) {
            syncToModMenu();
        }

        TagDatabase.reload();
        TagDatabase.applyOverrides(tagOverrides);
        TagDatabase.applyCustomTags(customTags);
        TagDatabase.applyTagColors(tagColors);
    }

    public static void syncToModMenu() {
        ModMenuConfigWriter.writeHiddenMods(computeAllHiddenMods());
    }

    /**
     * Aggregates all hiding criteria into a final ID set for ModMenu.
     * Hierarchy: Essentials > Favorites > Protection > Patterns/Tags/Explicit
     */
    public static Set<String> computeAllHiddenMods() {
        if (!filterEnabled) return Collections.emptySet();

        Set<String> hidden = new LinkedHashSet<>(hiddenMods);

        for (String tagId : hiddenTags) {
            hidden.addAll(TagDatabase.getModsForTag(tagId));
        }

        if (autoHideLibraries) {
            hidden.addAll(TagDatabase.getModsForTag("library"));
        }

        if (!hidePatterns.isEmpty()) {
            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                String modId   = mod.getMetadata().getId();
                String modName = mod.getMetadata().getName();
                for (String pattern : hidePatterns) {
                    try {
                        // Pattern matching against both ID and Name ensures broad filter coverage
                        // for mods with non-obvious IDs.
                        if (modId.matches(pattern) || modName.toLowerCase(Locale.ROOT).matches(pattern.toLowerCase(Locale.ROOT))) {
                            hidden.add(modId);
                        }
                    } catch (Exception ignored) { }
                }
            }
        }

        hidden.removeAll(favoriteMods);

        // Required to prevent breaking mod lists that assume certain dependencies
        // are always available or displayed.
        if (dependencyProtection) {
            hidden.removeAll(getRequiredDependencies(hidden));
        }

        hidden.remove("mod_menu_filter");
        hidden.remove("minecraft");
        hidden.remove("java");
        hidden.remove("fabricloader");

        return hidden;
    }

    /**
     * To prevent "ghost" mod issues, we reveal any hidden mod that is 
     * a direct dependency of a currently visible mod.
     */
    private static Set<String> getRequiredDependencies(Set<String> candidates) {
        Set<String> keepVisible = new HashSet<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (candidates.contains(mod.getMetadata().getId())) continue;
            mod.getMetadata().getDependencies().forEach(dep -> {
                if (candidates.contains(dep.getModId())) {
                    keepVisible.add(dep.getModId());
                }
            });
        }
        return keepVisible;
    }

    // ════════════════════════════════════════════════════════════════
    //  Profile management
    // ════════════════════════════════════════════════════════════════

    public static void saveCurrentAsProfile(String name) {
        ProfileData p = new ProfileData();
        p.hiddenMods       = new ArrayList<>(hiddenMods);
        p.hiddenTags       = new ArrayList<>(hiddenTags);
        p.favoriteMods     = new ArrayList<>(favoriteMods);
        p.autoHideLibraries = autoHideLibraries;
        profiles.put(name, p);
        save();
    }

    public static void loadProfile(String name) {
        ProfileData p = profiles.get(name);
        if (p == null) return;
        hiddenMods       = new ArrayList<>(p.hiddenMods  != null ? p.hiddenMods  : Collections.emptyList());
        hiddenTags       = new ArrayList<>(p.hiddenTags  != null ? p.hiddenTags  : Collections.emptyList());
        favoriteMods     = new ArrayList<>(p.favoriteMods != null ? p.favoriteMods : Collections.emptyList());
        autoHideLibraries = p.autoHideLibraries;
        activeProfile     = name;
        save();
    }

    public static void deleteProfile(String name) {
        profiles.remove(name);
        if (activeProfile.equals(name)) activeProfile = "default";
        save();
    }

    // ════════════════════════════════════════════════════════════════
    //  Convenience helpers
    // ════════════════════════════════════════════════════════════════

    public static boolean isFilterEnabled() { return filterEnabled; }

    public static boolean isModHidden(String modId) {
        return computeAllHiddenMods().contains(modId);
    }

    public static boolean isModFavorite(String modId) {
        return favoriteMods.contains(modId);
    }

    public static void toggleFavorite(String modId) {
        if (!favoriteMods.remove(modId)) favoriteMods.add(modId);
    }

    public static void toggleHidden(String modId) {
        if (!hiddenMods.remove(modId)) hiddenMods.add(modId);
    }

    /** Sorted list of every installed mod ID. */
    public static List<String> getInstalledModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(m -> m.getMetadata().getId())
                .sorted()
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    //  Defaults
    // ════════════════════════════════════════════════════════════════

    private static void initDefaults() {
        filterEnabled       = true;
        syncWithModMenu     = true;
        showTagBadges       = true;
        autoHideLibraries   = false;
        dependencyProtection = true;
        compactBadges       = false;
        hiddenMods   = new ArrayList<>();
        favoriteMods = new ArrayList<>();
        hiddenTags   = new ArrayList<>();
        hidePatterns = new ArrayList<>();
        tagOverrides = new HashMap<>();
        customTags   = new HashMap<>();
        tagColors    = new HashMap<>();
        activeProfile = "default";
        profiles      = new HashMap<>();
    }

    private static void applyFromData(ConfigData d) {
        filterEnabled       = d.filterEnabled;
        syncWithModMenu     = d.syncWithModMenu;
        showTagBadges       = d.showTagBadges;
        autoHideLibraries   = d.autoHideLibraries;
        dependencyProtection = d.dependencyProtection;
        compactBadges       = d.compactBadges;
        hiddenMods   = d.hiddenMods   != null ? new ArrayList<>(Arrays.asList(d.hiddenMods))   : new ArrayList<>();
        favoriteMods = d.favoriteMods != null ? new ArrayList<>(Arrays.asList(d.favoriteMods)) : new ArrayList<>();
        hiddenTags   = d.hiddenTags   != null ? new ArrayList<>(Arrays.asList(d.hiddenTags))   : new ArrayList<>();
        hidePatterns = d.hidePatterns != null ? new ArrayList<>(Arrays.asList(d.hidePatterns)) : new ArrayList<>();
        tagOverrides = d.tagOverrides != null ? new HashMap<>(d.tagOverrides) : new HashMap<>();
        customTags   = d.customTags   != null ? new HashMap<>(d.customTags)   : new HashMap<>();
        tagColors    = d.tagColors    != null ? new HashMap<>(d.tagColors)    : new HashMap<>();
        activeProfile = d.activeProfile != null ? d.activeProfile : "default";
        profiles      = d.profiles      != null ? new HashMap<>(d.profiles)   : new HashMap<>();
    }

    // ════════════════════════════════════════════════════════════════
    //  Serialisation POJOs
    // ════════════════════════════════════════════════════════════════

    static class ConfigData {
        boolean filterEnabled        = true;
        boolean syncWithModMenu      = true;
        boolean showTagBadges        = true;
        boolean autoHideLibraries    = false;
        boolean dependencyProtection = true;
        boolean compactBadges        = false;

        String[] hiddenMods;
        String[] favoriteMods;
        String[] hiddenTags;
        String[] hidePatterns;

        Map<String, List<String>>  tagOverrides;
        Map<String, CustomTagData> customTags;
        Map<String, Integer>       tagColors;

        String                     activeProfile = "default";
        Map<String, ProfileData>   profiles;
    }

    /** User-created tag with a colour and an explicit mod list. */
    public static class CustomTagData {
        public int color = 0xFFFFFF;
        public List<String> mods = new ArrayList<>();

        public CustomTagData() {}
        public CustomTagData(int color, List<String> mods) {
            this.color = color;
            this.mods  = mods;
        }
    }

    /** A named snapshot of filter settings. */
    public static class ProfileData {
        public List<String> hiddenMods       = new ArrayList<>();
        public List<String> hiddenTags       = new ArrayList<>();
        public List<String> favoriteMods     = new ArrayList<>();
        public boolean      autoHideLibraries = false;
    }
}
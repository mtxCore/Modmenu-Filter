package com.mtxCore.mod_menu_filter.client;

import com.mtxCore.mod_menu_filter.client.config.*;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.*;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("title.mod_menu_filter.config"))
                    .setDoesConfirmSave(true);

            ConfigEntryBuilder eb = builder.entryBuilder();

            buildGeneralCategory(builder, eb);
            buildHiddenModsCategory(builder, eb);
            buildTagFiltersCategory(builder, eb);
            buildFavoritesCategory(builder, eb);
            buildProfilesCategory(builder, eb);
            buildAdvancedCategory(builder, eb);

            builder.setSavingRunnable(ConfigManager::save);
            return builder.build();
        };
    }

    private void buildGeneralCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory cat = builder.getOrCreateCategory(
                Component.translatable("category.mod_menu_filter.general"));

        cat.addEntry(eb
                .startBooleanToggle(
                        Component.translatable("option.mod_menu_filter.enabled"),
                        ConfigManager.filterEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.enabled"))
                .setSaveConsumer(v -> ConfigManager.filterEnabled = v)
                .build());

        cat.addEntry(eb
                .startBooleanToggle(
                        Component.translatable("option.mod_menu_filter.sync"),
                        ConfigManager.syncWithModMenu)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.sync"))
                .setSaveConsumer(v -> ConfigManager.syncWithModMenu = v)
                .build());

        cat.addEntry(eb
                .startBooleanToggle(
                        Component.translatable("option.mod_menu_filter.badges"),
                        ConfigManager.showTagBadges)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.badges"))
                .setSaveConsumer(v -> ConfigManager.showTagBadges = v)
                .build());

        cat.addEntry(eb
                .startBooleanToggle(
                        Component.translatable("option.mod_menu_filter.auto_hide_libs"),
                        ConfigManager.autoHideLibraries)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.auto_hide_libs"))
                .setSaveConsumer(v -> ConfigManager.autoHideLibraries = v)
                .build());

        cat.addEntry(eb
                .startBooleanToggle(
                        Component.translatable("option.mod_menu_filter.dep_protection"),
                        ConfigManager.dependencyProtection)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.dep_protection"))
                .setSaveConsumer(v -> ConfigManager.dependencyProtection = v)
                .build());

        cat.addEntry(eb
                .startBooleanToggle(
                        Component.translatable("option.mod_menu_filter.compact_badges"),
                        ConfigManager.compactBadges)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.compact_badges"))
                .setSaveConsumer(v -> ConfigManager.compactBadges = v)
                .build());
    }

    private void buildHiddenModsCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory cat = builder.getOrCreateCategory(
                Component.translatable("category.mod_menu_filter.hidden"));

        cat.addEntry(eb
                .startTextDescription(
                        Component.translatable("text.mod_menu_filter.hidden_info")
                                .withStyle(ChatFormatting.GRAY))
                .build());

        cat.addEntry(eb
                .startStrList(
                        Component.translatable("option.mod_menu_filter.hidden_mods"),
                        ConfigManager.hiddenMods)
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.hidden_mods"))
                .setSaveConsumer(v -> ConfigManager.hiddenMods = v)
                .build());

        cat.addEntry(eb
                .startStrList(
                        Component.translatable("option.mod_menu_filter.hide_patterns"),
                        ConfigManager.hidePatterns)
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.hide_patterns"))
                .setSaveConsumer(v -> ConfigManager.hidePatterns = v)
                .build());
    }

    private void buildTagFiltersCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory cat = builder.getOrCreateCategory(
                Component.translatable("category.mod_menu_filter.tags"));

        cat.addEntry(eb
                .startTextDescription(
                        Component.translatable("text.mod_menu_filter.tags_info")
                                .withStyle(ChatFormatting.GRAY))
                .build());

        for (Map.Entry<String, ModTag> entry : ModTag.getAll().entrySet()) {
            String tagId = entry.getKey();
            ModTag tag   = entry.getValue();
            int modCount = TagDatabase.getModsForTag(tagId).size();

            SubCategoryBuilder sub = eb.startSubCategory(
                    coloredTagLabel(tag, modCount));

            sub.add(eb
                    .startBooleanToggle(
                            Component.translatable("option.mod_menu_filter.hide_tag",
                                    tag.getDisplayName(), String.valueOf(modCount)),
                            ConfigManager.hiddenTags.contains(tagId))
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("tooltip.mod_menu_filter.hide_tag",
                            tag.getDisplayName()))
                    .setSaveConsumer(v -> {
                        if (v && !ConfigManager.hiddenTags.contains(tagId))
                            ConfigManager.hiddenTags.add(tagId);
                        else if (!v)
                            ConfigManager.hiddenTags.remove(tagId);
                    })
                    .build());

            int currentColor = ConfigManager.tagColors.getOrDefault(tagId, tag.getColor());
            sub.add(eb
                    .startColorField(
                            Component.translatable("option.mod_menu_filter.tag_color",
                                    tag.getDisplayName()),
                            currentColor)
                    .setDefaultValue(tag.getColor())
                    .setTooltip(Component.translatable("tooltip.mod_menu_filter.tag_color",
                            tag.getDisplayName()))
                    .setSaveConsumer(v -> ConfigManager.tagColors.put(tagId, v))
                    .build());

            Set<String> modsInTag = TagDatabase.getModsForTag(tagId);
            List<String> installedInTag = modsInTag.stream()
                    .filter(id -> FabricLoader.getInstance().isModLoaded(id))
                    .sorted()
                    .limit(25)
                    .toList();

            if (!installedInTag.isEmpty()) {
                StringBuilder sb = new StringBuilder("\u00A77Installed: ");
                sb.append("\u00A7f");
                sb.append(String.join(", ", installedInTag));
                long total = modsInTag.stream()
                        .filter(id -> FabricLoader.getInstance().isModLoaded(id))
                        .count();
                if (total > 25) sb.append(" \u00A77... and ").append(total - 25).append(" more");
                sub.add(eb
                        .startTextDescription(Component.literal(String.valueOf(sb.toString())))
                        .build());
            }

            cat.addEntry(sub.build());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Favorites
    // ═══════════════════════════════════════════════════════════

    private void buildFavoritesCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory cat = builder.getOrCreateCategory(
                Component.translatable("category.mod_menu_filter.favorites"));

        cat.addEntry(eb
                .startTextDescription(
                        Component.translatable("text.mod_menu_filter.favorites_info")
                                .withStyle(ChatFormatting.GRAY))
                .build());

        cat.addEntry(eb
                .startStrList(
                        Component.translatable("option.mod_menu_filter.favorites"),
                        ConfigManager.favoriteMods)
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.favorites"))
                .setSaveConsumer(v -> ConfigManager.favoriteMods = v)
                .build());
    }

    // ═══════════════════════════════════════════════════════════
    //  Profiles
    // ═══════════════════════════════════════════════════════════

    private void buildProfilesCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory cat = builder.getOrCreateCategory(
                Component.translatable("category.mod_menu_filter.profiles"));

        cat.addEntry(eb
                .startTextDescription(
                        Component.translatable("text.mod_menu_filter.profiles_info")
                                .withStyle(ChatFormatting.GRAY))
                .build());

        cat.addEntry(eb
                .startStrField(
                        Component.translatable("option.mod_menu_filter.active_profile"),
                        ConfigManager.activeProfile)
                .setDefaultValue("default")
                .setTooltip(Component.translatable("tooltip.mod_menu_filter.active_profile"))
                .setSaveConsumer(v -> {
                    if (!v.equals(ConfigManager.activeProfile)) {
                        ConfigManager.saveCurrentAsProfile(ConfigManager.activeProfile);
                        ConfigManager.activeProfile = v;
                        if (ConfigManager.profiles.containsKey(v)) {
                            ConfigManager.loadProfile(v);
                        }
                    }
                })
                .build());

        // List existing profiles
        for (Map.Entry<String, ConfigManager.ProfileData> entry : ConfigManager.profiles.entrySet()) {
            String name = entry.getKey();
            ConfigManager.ProfileData p = entry.getValue();
            int hMods = p.hiddenMods  != null ? p.hiddenMods.size()  : 0;
            int hTags = p.hiddenTags  != null ? p.hiddenTags.size()  : 0;
            int faves = p.favoriteMods != null ? p.favoriteMods.size() : 0;

            boolean isActive = name.equals(ConfigManager.activeProfile);
            String prefix = isActive ? "\u00A7a\u25B6 " : "\u00A7e";
            cat.addEntry(eb
                    .startTextDescription(Component.literal(
                            prefix + name + " \u00A77\u2014 "
                                    + hMods + " hidden, "
                                    + hTags + " tag filters, "
                                    + faves + " favourites"))
                    .build());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Advanced & Statistics
    // ═══════════════════════════════════════════════════════════

    private void buildAdvancedCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory cat = builder.getOrCreateCategory(
                Component.translatable("category.mod_menu_filter.advanced"));

        // Overview
        int totalMods  = ConfigManager.getInstalledModIds().size();
        int hiddenCount = ConfigManager.computeAllHiddenMods().size();
        int visible     = totalMods - hiddenCount;

        cat.addEntry(eb
                .startTextDescription(Component.literal(
                        "\u00A7lMod Statistics\n"
                                + "\u00A77Installed: \u00A7a" + totalMods
                                + " \u00A77| Hidden: \u00A7c" + hiddenCount
                                + " \u00A77| Visible: \u00A7e" + visible))
                .build());

        // Per-tag stats
        SubCategoryBuilder tagStats = eb.startSubCategory(
                Component.literal("\u00A76Tag Breakdown"));

        for (Map.Entry<String, ModTag> entry : ModTag.getAll().entrySet()) {
            ModTag tag = entry.getValue();
            Set<String> modsInTag = TagDatabase.getModsForTag(entry.getKey());
            long installed = modsInTag.stream()
                    .filter(id -> FabricLoader.getInstance().isModLoaded(id))
                    .count();
            boolean isHidden = ConfigManager.hiddenTags.contains(entry.getKey());
            String status = isHidden ? " \u00A7c[HIDDEN]" : "";

            tagStats.add(eb.startTextDescription(
                    Component.literal(
                            "\u00A7f" + tag.getDisplayName()
                                    + ": \u00A7a" + installed + " installed"
                                    + " \u00A77(of " + modsInTag.size() + " known)"
                                    + status))
                    .build());
        }
        cat.addEntry(tagStats.build());

        // Config tips
        cat.addEntry(eb
                .startTextDescription(
                        Component.translatable("text.mod_menu_filter.tag_override_info")
                                .withStyle(ChatFormatting.GRAY))
                .build());

        cat.addEntry(eb
                .startTextDescription(
                        Component.translatable("text.mod_menu_filter.regex_info")
                                .withStyle(ChatFormatting.GRAY))
                .build());
    }

    // ═══════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════

    private static MutableComponent coloredTagLabel(ModTag tag, int count) {
        return Component.literal("")
                .append(Component.literal(String.valueOf(tag.getDisplayName()))
                        .withStyle(Style.EMPTY.withColor(tag.getColorWithAlpha())))
                .append(Component.literal(" (" + count + " mods)")
                        .withStyle(ChatFormatting.GRAY));
    }
}
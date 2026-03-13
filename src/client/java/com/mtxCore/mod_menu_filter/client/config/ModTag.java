package com.mtxCore.mod_menu_filter.client.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a colored tag that can be assigned to mods.
 * Predefined tags are registered on class load; custom tags are created on demand.
 */
public class ModTag {

    private static final Map<String, ModTag> REGISTRY = new HashMap<>();

    public static final ModTag PERFORMANCE = register("performance", "Performance", 0x55FF55);
    public static final ModTag GAMEPLAY    = register("gameplay",    "Gameplay",    0x5599FF);
    public static final ModTag UTILITY     = register("utility",     "Utility",     0xFFAA00);
    public static final ModTag LIBRARY     = register("library",     "Library",     0xAAAAAA);

    private final String id;
    private final String displayName;
    private int color;

    public ModTag(String id, String displayName, int color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color & 0xFFFFFF;
    }

    private static ModTag register(String id, String displayName, int color) {
        ModTag tag = new ModTag(id, displayName, color);
        REGISTRY.put(id, tag);
        return tag;
    }

    /**
     * Retrieves an existing tag by ID or generates a new one.
     * New tags receive a deterministic color based on their ID hash to ensure 
     * visual consistency across sessions without requiring explicit config.
     */
    public static ModTag getOrCreate(String id) {
        return REGISTRY.computeIfAbsent(id, k -> new ModTag(k, capitalize(k), generateColor(k)));
    }

    public static ModTag get(String id) {
        return REGISTRY.get(id);
    }

    public static Map<String, ModTag> getAll() {
        return REGISTRY;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    /** Helper for MC Rendering — provides the full AARRGGBB integer. */
    public int getColorWithAlpha() {
        return 0xFF000000 | color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String spaced = s.replace('_', ' ').replace('-', ' ');
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : spaced.toCharArray()) {
            if (c == ' ') {
                sb.append(' ');
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    /**
     * Generates a deterministic, visually distinct color from a string hash.
     * Uses manual HSB to RGB calculation to maintain compatibility across 
     * OS environments where Java AWT might be restricted or absent.
     */
    private static int generateColor(String id) {
        int hash = id.hashCode();
        // HSB range adjustments ensure the color is always readable with white text
        float hue = ((hash & 0xFFFF) % 360) / 360.0f;
        float sat = 0.55f + ((hash >> 16) & 0x1F) / 100.0f; 
        float bri = 0.75f + ((hash >> 21) & 0x1F) / 120.0f; 
        return hsbToRgb(hue, sat, bri);
    }

    private static int hsbToRgb(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - saturation * (1.0f - f));
            switch ((int) h) {
                case 0 -> { r = (int) (brightness * 255 + 0.5f); g = (int) (t * 255 + 0.5f); b = (int) (p * 255 + 0.5f); }
                case 1 -> { r = (int) (q * 255 + 0.5f); g = (int) (brightness * 255 + 0.5f); b = (int) (p * 255 + 0.5f); }
                case 2 -> { r = (int) (p * 255 + 0.5f); g = (int) (brightness * 255 + 0.5f); b = (int) (t * 255 + 0.5f); }
                case 3 -> { r = (int) (p * 255 + 0.5f); g = (int) (q * 255 + 0.5f); b = (int) (brightness * 255 + 0.5f); }
                case 4 -> { r = (int) (t * 255 + 0.5f); g = (int) (p * 255 + 0.5f); b = (int) (brightness * 255 + 0.5f); }
                case 5 -> { r = (int) (brightness * 255 + 0.5f); g = (int) (p * 255 + 0.5f); b = (int) (q * 255 + 0.5f); }
            }
        }
        return (r << 16) | (g << 8) | b;
    }
}
}

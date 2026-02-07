package com.mtxCore.mod_menu_filter.client.config;

// Mixin Imports
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.fabricmc.loader.api.FabricLoader;

// Java Imports
import java.util.Set;
import java.util.List;

// Just to check whether to load the filter based off of config
public class ConfigCheckMixinPlugin implements IMixinConfigPlugin {


    @Override
    public void onLoad(String mixinPackage) {
        // Load config on mixin load
        ConfigManager.load();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean isLegacyInstalled = FabricLoader.getInstance().isModLoaded("legacy");
        boolean filterMixinName = mixinClassName.endsWith("ModMenuFilterMixin");
        boolean filterEnabled = ConfigManager.isFilterEnabled();

        if (mixinClassName.endsWith("ModMenuFilterMixin")) {
            // If Legacy4J is there, ModMenu's screen is usually disabled/replaced,
            // so we skip this mixin to avoid conflicts.
            if (isLegacyInstalled) return false;
            return filterEnabled;
        }

        // For Legacy4J's mod menu
        if (mixinClassName.endsWith("ModMenuFilterLegacyMixin")) {
            // Only even attempt this if Legacy4J is actually present
            if (!isLegacyInstalled) return false;
            return filterEnabled;
        }

        return true; // Default to true for other mixins
    }

    // Other method shit or something
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets){}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}

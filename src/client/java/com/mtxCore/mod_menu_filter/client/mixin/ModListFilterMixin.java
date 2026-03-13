package com.mtxCore.mod_menu_filter.client.mixin;

import com.mtxCore.mod_menu_filter.client.config.ConfigManager;
import com.mtxCore.mod_menu_filter.client.config.TagDatabase;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.ModSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Filters the mod list by the active tag selection.
 *
 * Injecting at RETURN (rather than HEAD) means Mod Menu's own search/sort
 * logic runs first and produces a fully ordered list — we just trim it down
 * before it reaches the UI, so sorting and alphabetisation stay correct.
 */
@Mixin(ModSearch.class)
public class ModListFilterMixin {

    @Inject(method = "search", at = @At("RETURN"), cancellable = true, require = 0)
    private static void modMenuFilter$postSearch(
            ModsScreen screen, String query, List<Mod> candidates,
            CallbackInfoReturnable<List<Mod>> cir) {
        try {
            Set<String> active = ConfigManager.activeTagFilters;
            if (active == null || active.isEmpty()) return;

            List<Mod> filtered = new ArrayList<>();
            for (Mod mod : cir.getReturnValue()) {
                String modId = mod.getId();
                for (String tagId : active) {
                    boolean matches;
                    if ("favorites".equals(tagId)) {
                        matches = ConfigManager.isModFavorite(modId);
                    } else {
                        matches = TagDatabase.hasTag(modId, tagId);
                    }
                    if (matches) {
                        filtered.add(mod);
                        break;
                    }
                }
            }
            cir.setReturnValue(filtered);
        } catch (Exception e) {
            // If tag filtering blows up for any reason, return the unfiltered result.
            // Better a noisy mod list than a completely broken screen.
        }
    }
}

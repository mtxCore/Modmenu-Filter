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
 * Intercepts {@link ModSearch#search} to additionally filter by active
 * tag buttons BEFORE Mod Menu adds entries to the list.
 * <p>
 * This approach is reliable because we replace the return value of the
 * static search method rather than mutating the children list afterwards.
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
            // Silently ignore to avoid breaking the mod list
        }
    }
}

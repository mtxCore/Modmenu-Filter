package com.mtxCore.mod_menu_filter.client.mixin;

import com.mtxCore.mod_menu_filter.client.config.TagDatabase;
import com.terraformersmc.modmenu.util.mod.Mod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/**
 * Intercepts getBadges() to dynamically add or remove the LIBRARY badge
 * based on our library tags, so Mod Menu's native "Hide Libraries" toggle
 * respects our tag database.
 */
@Mixin(targets = {
    "com.terraformersmc.modmenu.util.mod.fabric.FabricMod",
    "com.terraformersmc.modmenu.util.mod.quilt.QuiltMod"
})
public abstract class ModBadgeLibraryMixin {

    @Inject(method = "getBadges", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void modMenuFilter$overrideLibraryBadge(CallbackInfoReturnable<Set<Mod.Badge>> cir) {
        try {
            Mod self = (Mod) (Object) this;
            String modId = self.getId();
            Set<Mod.Badge> badges = cir.getReturnValue();

            boolean isOurLibrary = TagDatabase.hasTag(modId, "library");
            boolean hasLibraryBadge = badges.contains(Mod.Badge.LIBRARY);

            if (isOurLibrary != hasLibraryBadge) {
                Set<Mod.Badge> newBadges = new HashSet<>(badges);
                if (isOurLibrary) {
                    newBadges.add(Mod.Badge.LIBRARY);
                } else {
                    newBadges.remove(Mod.Badge.LIBRARY);
                }
                cir.setReturnValue(newBadges);
            }
        } catch (Exception e) {
            // Silently ignore
        }
    }
}

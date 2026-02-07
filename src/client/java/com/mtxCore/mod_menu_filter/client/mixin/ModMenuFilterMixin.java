package com.mtxCore.mod_menu_filter.client.mixin;

import com.mtxCore.mod_menu_filter.client.config.ConfigManager;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(ModListWidget.class)
public class ModMenuFilterMixin {

    @Redirect(
            method = "filter(Ljava/lang/String;ZZ)V", // The method we are inside
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/terraformersmc/modmenu/gui/widget/ModListWidget;addEntry(Lcom/terraformersmc/modmenu/gui/widget/entries/ModListEntry;)I"
            )
    )
    private int onAddEntry(ModListWidget instance, ModListEntry entry) {
        // Generic Entry Check
        if (entry instanceof ModListEntry modEntry) {
            String modId = modEntry.getMod().getId();

            if (ConfigManager.excludedMods.contains(modId)) {
                return 0; // Return nothing, skip entry
            }
        }

        return instance.addEntry(entry); // Else add entry as normal
    }
}
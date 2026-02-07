// For Legacy4j's mod menu

package com.mtxCore.mod_menu_filter.client.mixin;

// Config Manager
import com.mtxCore.mod_menu_filter.client.config.ConfigManager;

// Mixin
import net.minecraft.client.gui.components.Renderable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Legacy4j
import wily.legacy.client.screen.ModsScreen;
import wily.legacy.client.screen.RenderableVList;
import wily.factoryapi.util.ModInfo;


@Mixin(ModsScreen.class)
public abstract class ModMenuFilterLegacyMixin {
    @Redirect(
            method="lambda$fillMods$4",
            at=@At(
                    value="INVOKE",
                    target="Lwily/legacy/client/screen/RenderableVList;addRenderable(Lnet/minecraft/client/gui/components/Renderable;)Lwily/legacy/client/screen/RenderableVList;"
            )
    )
    private RenderableVList onAddModEntry(RenderableVList instance, Renderable renderable, ModInfo modInfo) {
        if(ConfigManager.excludedMods.contains(modInfo.getId())){
            return instance; // Skip adding this mod entry
        } else {
            return instance.addRenderable(renderable); // Add as normal
        }
    }
}
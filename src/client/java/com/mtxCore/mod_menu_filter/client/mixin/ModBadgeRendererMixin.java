package com.mtxCore.mod_menu_filter.client.mixin;

import com.mtxCore.mod_menu_filter.client.config.ConfigManager;
import com.mtxCore.mod_menu_filter.client.config.ModTag;
import com.mtxCore.mod_menu_filter.client.config.TagDatabase;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.ModBadgeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Replaces Mod Menu's native badge rendering (Client, Library, etc.) with our
 * own tag badges.
 *
 * We inject into ModBadgeRenderer.draw() because ModListEntry already
 * calculates the correct startX/startY (right after the truncated mod name)
 * and passes them to ModBadgeRenderer's constructor — so we inherit that
 * coordinate math for free, staying version-agnostic across all modmenu
 * releases regardless of how ModListEntry.render() signature changes.
 */
@Mixin(ModBadgeRenderer.class)
public class ModBadgeRendererMixin {

    @Shadow(remap = false) protected int startX;
    @Shadow(remap = false) protected int startY;
    @Shadow(remap = false) protected int badgeMax;
    @Shadow(remap = false) protected Mod mod;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, require = 0)
    private void modMenuFilter$replaceBadges(GuiGraphics drawContext, int mouseX, int mouseY, CallbackInfo ci) {
        if (!ConfigManager.showTagBadges) return;

        // Suppress native badges regardless of whether we have custom tags to show
        ci.cancel();

        try {
            String modId = mod.getId();
            List<ModTag> tags = TagDatabase.getTagsForMod(modId);
            boolean isFavorite = ConfigManager.isModFavorite(modId);
            if (tags.isEmpty() && !isFavorite) return;

            Font font = Minecraft.getInstance().font;
            boolean compact = ConfigManager.compactBadges;

            int badgeX = startX;
            int badgeY = startY;

            // Favourite star
            if (isFavorite) {
                String star = "\u2605";
                int starWidth = font.width(star);
                if (badgeX + starWidth < badgeMax) {
                    drawContext.drawString(font, star, badgeX, badgeY, 0xFFFFD700, true);
                    badgeX += starWidth + 3;
                }
            }

            // Tag badges
            for (ModTag tag : tags) {
                String fullLabel = String.valueOf(tag.getDisplayName());
                String label = fullLabel;
                int textW = font.width(label);
                int totalW = textW + 6;

                // Shorten to 3-char abbreviation if compact mode or not enough space
                if (compact || badgeX + totalW >= badgeMax) {
                    label = String.valueOf(fullLabel.substring(0, Math.min(3, fullLabel.length())).toUpperCase());
                    textW = font.width(label);
                    totalW = textW + 6;
                }

                if (badgeX + totalW >= badgeMax) break;

                int outlineColor = 0xFF000000 | modMenuFilter$darken(tag.getColor(), 0.55f);
                int fillColor    = 0xFF000000 | modMenuFilter$darken(tag.getColor(), 0.35f);

                drawContext.fill(badgeX + 1,      badgeY - 1,              badgeX + totalW,     badgeY,                       outlineColor);
                drawContext.fill(badgeX,           badgeY,                  badgeX + 1,          badgeY + font.lineHeight,      outlineColor);
                drawContext.fill(badgeX + 1,      badgeY + font.lineHeight, badgeX + totalW,     badgeY + font.lineHeight + 1,  outlineColor);
                drawContext.fill(badgeX + totalW, badgeY,                  badgeX + totalW + 1, badgeY + font.lineHeight,      outlineColor);
                drawContext.fill(badgeX + 1,      badgeY,                  badgeX + totalW,     badgeY + font.lineHeight,      fillColor);

                int textX = (int) (badgeX + 1 + (totalW - textW) / 2.0f);
                drawContext.drawString(font, label, textX, badgeY + 1, 0xFFCACACA, false);

                badgeX += totalW + 3;
            }
        } catch (Exception e) {
            // Silently ignore rendering errors
        }
    }

    @Unique
    private static int modMenuFilter$darken(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >>  8) & 0xFF) * factor);
        int b = (int) (( rgb        & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }
}

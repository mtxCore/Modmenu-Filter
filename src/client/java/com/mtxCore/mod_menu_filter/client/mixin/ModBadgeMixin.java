package com.mtxCore.mod_menu_filter.client.mixin;

import com.mtxCore.mod_menu_filter.client.config.ConfigManager;
import com.mtxCore.mod_menu_filter.client.config.ModTag;
import com.mtxCore.mod_menu_filter.client.config.TagDatabase;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Renders coloured tag badges and a favourite star on each mod entry
 * in Mod Menu's mod list.  This is purely additive — it does not alter
 * filtering, which is handled natively by Mod Menu's {@code hidden_mods}.
 */
@Mixin(ModListEntry.class)
public abstract class ModBadgeMixin {

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void modMenuFilter$renderTagBadges(
            GuiGraphics graphics, int index, int y, int x,
            int entryWidth, int entryHeight,
            int mouseX, int mouseY, boolean hovered,
            float tickDelta, CallbackInfo ci) {
        try {
            if (!ConfigManager.showTagBadges) return;

            ModListEntry self = (ModListEntry) (Object) this;
            String modId = self.getMod().getId();

            List<ModTag> tags = TagDatabase.getTagsForMod(modId);
            boolean isFavorite = ConfigManager.isModFavorite(modId);
            if (tags.isEmpty() && !isFavorite) return;

            Font font = Minecraft.getInstance().font;
            boolean compact = ConfigManager.compactBadges;

            int badgeH   = compact ? 9 : 11;
            int pad      = compact ? 2 : 3;
            int spacing  = 2;
            int textYOff = 1;

            int renderX = x + entryWidth - 4;
            int renderY = y + entryHeight - badgeH - 2;

            for (int i = tags.size() - 1; i >= 0; i--) {
                ModTag tag = tags.get(i);
                String label = compact
                        ? tag.getDisplayName().substring(0, Math.min(3, tag.getDisplayName().length())).toUpperCase()
                        : tag.getDisplayName();
                int textW  = font.width(label);
                int totalW = textW + pad * 2;

                renderX -= totalW;

                int bg = (0xDD << 24) | tag.getColor();
                graphics.fill(renderX, renderY, renderX + totalW, renderY + badgeH, bg);

                int border = (0xFF << 24) | modMenuFilter$darken(tag.getColor(), 0.55f);
                graphics.fill(renderX,             renderY,             renderX + totalW, renderY + 1,      border);
                graphics.fill(renderX,             renderY + badgeH - 1, renderX + totalW, renderY + badgeH, border);
                graphics.fill(renderX,             renderY,             renderX + 1,      renderY + badgeH, border);
                graphics.fill(renderX + totalW - 1, renderY,             renderX + totalW, renderY + badgeH, border);

                graphics.drawString(font, label, renderX + pad, renderY + textYOff, 0xFFFFFFFF, true);

                renderX -= spacing;
            }

            if (isFavorite) {
                String star = "\u2605";
                renderX -= font.width(star) + 2;
                graphics.drawString(font, star, renderX, renderY, 0xFFFFD700, true);
            }
        } catch (Exception e) {
            // Silently failing rendering to maintain compatibility with other mods 
            // that might aggressively transform ModListEntry.
        }
    }

    /**
     * Darkens a color by a scaling factor.
     * Used for badge borders to create visual depth without needing secondary 
     * color definitions in the tag config.
     */
    @Unique
    private static int modMenuFilter$darken(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >>  8) & 0xFF) * factor);
        int b = (int) (( rgb        & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }
}

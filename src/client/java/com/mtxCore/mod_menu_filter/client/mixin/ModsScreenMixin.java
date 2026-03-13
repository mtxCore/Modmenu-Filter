package com.mtxCore.mod_menu_filter.client.mixin;

import com.mtxCore.mod_menu_filter.client.config.ConfigManager;
import com.mtxCore.mod_menu_filter.client.config.ModTag;
import com.mtxCore.mod_menu_filter.client.config.TagDatabase;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * Adds tag-filter toggle buttons to Mod Menu's filter panel.
 * <p>
 * Buttons appear in the row(s) below Sort / Libraries when the funnel
 * icon is clicked.  The mod list is pushed down to make room, and the
 * list is restored when the panel is dismissed.  Clicking a tag button
 * toggles that tag; mods not matching ANY active tag are hidden.
 */
@Mixin(ModsScreen.class)
public abstract class ModsScreenMixin extends Screen {

    @Shadow private boolean filterOptionsShown;
    @Shadow private int paneWidth;
    @Shadow private int filtersX;
    @Shadow private ModListWidget modList;

    @Unique private final List<Button> modMenuFilter$tagButtons = new ArrayList<>();
    /** Top Y of the first tag-button row (below the Sort/Libraries row at y=45+20). */
    @Unique private static final int TAG_ROW_Y     = 68;
    @Unique private static final int TAG_BTN_H     = 16;
    @Unique private static final int MOD_LIST_Y_DEFAULT = 67;
    /** Populated during init; used by pushListDown(). */
    @Unique private int modMenuFilter$expandedListY = 92;

    protected ModsScreenMixin(Component title) { super(title); }

    // -----------------------------------------------------------------------
    // init
    // -----------------------------------------------------------------------

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void modMenuFilter$addTagButtons(CallbackInfo ci) {
        modMenuFilter$tagButtons.clear();
        ConfigManager.activeTagFilters.clear();

        // Build sorted tag list — Favorites first, rest alphabetical
        List<String> tagIds = new ArrayList<>(TagDatabase.getAllTagIds());
        Collections.sort(tagIds);
        tagIds.add(0, "favorites");

        // Pre-compute labels and widths so we can do look-ahead packing
        List<String>  labels = new ArrayList<>(tagIds.size());
        List<Integer> widths = new ArrayList<>(tagIds.size());
        for (String tagId : tagIds) {
            String lbl = String.valueOf(modMenuFilter$label(tagId, false));
            labels.add(lbl);
            widths.add(this.font.width(lbl) + 10);
        }

        final int maxX = paneWidth - 2;

        // ---- Row-filling pass ----
        // For each row, repeatedly scan the remaining unplaced buttons from
        // left to right and pack every button that still fits — this fills
        // gaps before starting a new row.
        boolean[] placed = new boolean[tagIds.size()];
        int placedCount  = 0;
        int buttonY = TAG_ROW_Y;
        int rowEnd  = TAG_ROW_Y; // will be updated below

        // We need to emit buttons in a stable visual order (top-left → bottom-right),
        // so collect (index, x, y) triples first, then create widgets.
        int[] posX  = new int[tagIds.size()];
        int[] posY  = new int[tagIds.size()];

        while (placedCount < tagIds.size()) {
            int currentX = filtersX;
            boolean anyOnRow = false;

            for (int i = 0; i < tagIds.size(); i++) {
                if (placed[i]) continue;
                int w = widths.get(i);
                if (currentX + w <= maxX) {
                    posX[i]  = currentX;
                    posY[i]  = buttonY;
                    placed[i] = true;
                    placedCount++;
                    anyOnRow = true;
                    currentX += w + 2;
                }
            }
            rowEnd = buttonY + TAG_BTN_H;
            if (!anyOnRow) break; // safety valve — shouldn't happen
            buttonY = rowEnd + 2;
        }

        // Now create + register widgets in original order so Tab-order is sane
        for (int i = 0; i < tagIds.size(); i++) {
            final String fTag = tagIds.get(i);
            final String lbl  = String.valueOf(labels.get(i));
            final int    btnW = widths.get(i);
            Button btn = Button.builder(
                Component.literal(lbl),
                b -> {
                    boolean wasActive = ConfigManager.activeTagFilters.contains(fTag);
                    if (wasActive) ConfigManager.activeTagFilters.remove(fTag);
                    else          ConfigManager.activeTagFilters.add(fTag);
                    b.setMessage(Component.literal(String.valueOf(modMenuFilter$label(fTag, !wasActive))));
                    modList.reloadFilters();
                }
            ).pos(posX[i], posY[i]).size(btnW, TAG_BTN_H).build();

            btn.visible = filterOptionsShown;
            this.addRenderableWidget(btn);
            modMenuFilter$tagButtons.add(btn);
        }

        // Push mod list below all tag-button rows (4 px breathing room)
        modMenuFilter$expandedListY = rowEnd + 4;

        if (filterOptionsShown) modMenuFilter$pushListDown();
    }

    // -----------------------------------------------------------------------
    // setFilterOptionsShown hook
    // -----------------------------------------------------------------------

    @Inject(method = "setFilterOptionsShown", at = @At("TAIL"), require = 0)
    private void modMenuFilter$onFilterToggle(boolean shown, CallbackInfo ci) {
        for (Button btn : modMenuFilter$tagButtons) {
            btn.visible = shown;
        }
        if (shown) modMenuFilter$pushListDown();
        else       modMenuFilter$restoreList();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Unique
    private void modMenuFilter$pushListDown() {
        if (modList == null) return;
        int newTopY  = modMenuFilter$expandedListY;
        int newHeight = this.height - newTopY - 36;
        // updateSizeAndPosition(width, height, topY)
        modList.updateSizeAndPosition(paneWidth, newHeight, newTopY);
    }

    @Unique
    private void modMenuFilter$restoreList() {
        if (modList == null) return;
        int newHeight = this.height - MOD_LIST_Y_DEFAULT - 36;
        modList.updateSizeAndPosition(paneWidth, newHeight, MOD_LIST_Y_DEFAULT);
    }

    @Unique
    private static String modMenuFilter$label(String tagId, boolean active) {
        String name;
        if ("favorites".equals(tagId)) {
            name = "\u2605 Fav";
        } else {
            ModTag tag = ModTag.get(tagId);
            name = (tag != null) ? tag.getDisplayName() : tagId;
        }
        return active ? "\u2713 " + name : name;
    }
}

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
 * Inserts tag-filter buttons between the sort row and the mod list.
 *
 * Buttons are packed into rows greedily (widest-that-fits first) to avoid
 * wasting horizontal space. The mod list's top Y is then pushed down by the
 * total height of the button panel so nothing overlaps.
 *
 * When the filter panel is toggled off, the button rows collapse and the list
 * snaps back to its default Y, so the layout stays coherent whether the panel
 * is open or not.
 */
@Mixin(ModsScreen.class)
public abstract class ModsScreenMixin extends Screen {

    @Shadow private boolean filterOptionsShown;
    @Shadow private int paneWidth;
    @Shadow private int filtersX;
    @Shadow private ModListWidget modList;

    @Unique private final List<Button> modMenuFilter$tagButtons = new ArrayList<>();
    @Unique private static final int TAG_ROW_Y          = 68;
    @Unique private static final int TAG_BTN_H          = 16;
    @Unique private static final int MOD_LIST_Y_DEFAULT = 67;
    // Y that the mod list was pushed to after laying out tag rows; cached so
    // we can restore the right value when the screen resizes while expanded.
    @Unique private int modMenuFilter$expandedListY = 92;

    protected ModsScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void modMenuFilter$addTagButtons(CallbackInfo ci) {
        modMenuFilter$tagButtons.clear();
        ConfigManager.activeTagFilters.clear();

        List<String> tagIds = new ArrayList<>(TagDatabase.getAllTagIds());
        Collections.sort(tagIds);
        tagIds.add(0, "favorites");

        // Measure labels up front so the row-packing loop can do look-ahead
        // without hitting the text renderer on every candidate.
        List<String>  labels = new ArrayList<>(tagIds.size());
        List<Integer> widths = new ArrayList<>(tagIds.size());
        for (String tagId : tagIds) {
            String lbl = String.valueOf(modMenuFilter$label(tagId, false));
            labels.add(lbl);
            widths.add(this.font.width(lbl) + 10);
        }

        final int maxX = paneWidth - 2;

        // Greedy row-packing: scan all unplaced buttons left-to-right each pass
        // and take every one that fits. Repeat until all are placed.
        boolean[] placed = new boolean[tagIds.size()];
        int placedCount  = 0;
        int buttonY = TAG_ROW_Y;
        int rowEnd  = TAG_ROW_Y; // will be updated below

        // Collect (index, x, y) positions first, then create widgets in sorted order
        // so Tab-stop ordering matches left-to-right, top-to-bottom visual layout.
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
            if (!anyOnRow) break; // shouldn't happen, but guard against infinite loop
            buttonY = rowEnd + 2;
        }

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

        modMenuFilter$expandedListY = rowEnd + 4;

        if (filterOptionsShown) modMenuFilter$pushListDown();
    }

    @Inject(method = "setFilterOptionsShown", at = @At("TAIL"), require = 0)
    private void modMenuFilter$onFilterToggle(boolean shown, CallbackInfo ci) {
        for (Button btn : modMenuFilter$tagButtons) {
            btn.visible = shown;
        }
        if (shown) modMenuFilter$pushListDown();
        else       modMenuFilter$restoreList();
    }

    @Unique
    private void modMenuFilter$pushListDown() {
        if (modList == null) return;
        int newTopY  = modMenuFilter$expandedListY;
        int newHeight = this.height - newTopY - 36;
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

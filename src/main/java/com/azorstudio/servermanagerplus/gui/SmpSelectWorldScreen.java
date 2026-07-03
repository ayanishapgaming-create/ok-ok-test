package com.azorstudio.servermanagerplus.gui;

import com.azorstudio.servermanagerplus.data.ServerDataManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelSummary;

import java.util.List;

/**
 * Enhanced SelectWorldScreen with:
 * ─ Search bar (by world name / folder)
 * ─ Pinned toggle
 * ─ Right-click → Pin/Unpin context menu
 * ─ Pinned indicator (📌) on world entries
 */
@Environment(EnvType.CLIENT)
public class SmpSelectWorldScreen extends SelectWorldScreen {

    private String searchQuery = "";
    private boolean showPinnedOnly = false;

    private TextFieldWidget searchField;
    private ButtonWidget pinnedToggleBtn;

    public SmpSelectWorldScreen(Screen parent) {
        super(parent);
    }

    @Override
    protected void init() {
        super.init();

        int sw = this.width;
        int barY = 22;
        int fieldW = sw / 2 - 10;

        // Search field
        searchField = new TextFieldWidget(
            textRenderer,
            8, barY,
            fieldW, 18,
            Text.translatable("servermanagerplus.search.worlds")
        );
        searchField.setPlaceholderText(Text.literal("🔍  Search worlds by name..."));
        searchField.setMaxLength(128);
        searchField.setText(searchQuery);
        searchField.setChangedListener(q -> {
            searchQuery = q.toLowerCase().trim();
        });
        addDrawableChild(searchField);

        // Pinned toggle button
        pinnedToggleBtn = ButtonWidget.builder(
            pinnedBtnText(),
            btn -> {
                showPinnedOnly = !showPinnedOnly;
                btn.setMessage(pinnedBtnText());
            }
        ).dimensions(sw / 2 + 2, barY, fieldW, 18).build();
        addDrawableChild(pinnedToggleBtn);
    }

    private Text pinnedBtnText() {
        return showPinnedOnly
            ? Text.literal("★  Pinned Worlds")
            : Text.literal("☆  All Worlds");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            // Right-click → try to get selected world
            LevelSummary summary = getSelectedWorld();
            if (summary != null) {
                MinecraftClient.getInstance().setScreen(
                    new PinContextMenu(this,
                        summary.getName(),
                        summary.getDisplayName(),
                        false,
                        (int) mouseX, (int) mouseY)
                );
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Returns true if the given world should be shown under current filters.
     */
    public boolean shouldIncludeWorld(LevelSummary summary) {
        if (summary == null) return false;
        ServerDataManager dm = ServerDataManager.getInstance();
        String folder = summary.getName();
        String display = summary.getDisplayName() != null
            ? summary.getDisplayName().toLowerCase() : folder.toLowerCase();

        if (showPinnedOnly && !dm.isWorldPinned(folder)) {
            return false;
        }
        if (!searchQuery.isEmpty()) {
            return display.contains(searchQuery) || folder.toLowerCase().contains(searchQuery);
        }
        return true;
    }

    private LevelSummary getSelectedWorld() {
        try {
            var field = SelectWorldScreen.class.getDeclaredField("levelList");
            field.setAccessible(true);
            Object list = field.get(this);
            if (list == null) return null;
            var getSelected = list.getClass().getMethod("getSelectedOrNull");
            getSelected.setAccessible(true);
            Object entry = getSelected.invoke(list);
            if (entry == null) return null;
            var summaryField = entry.getClass().getDeclaredField("level");
            summaryField.setAccessible(true);
            return (LevelSummary) summaryField.get(entry);
        } catch (Exception e) {
            return null;
        }
    }
}

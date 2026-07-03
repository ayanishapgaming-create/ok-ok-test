package com.azorstudio.servermanagerplus.gui;

import com.azorstudio.servermanagerplus.data.ServerDataManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Utility class that injects a search bar + pin controls into the
 * vanilla SelectWorldScreen. Called from SelectWorldScreenMixin.
 */
@Environment(EnvType.CLIENT)
public class EnhancedWorldScreen {

    public static String worldSearchQuery = "";
    public static boolean showPinnedWorldsOnly = false;

    /**
     * Called from the mixin after vanilla init() finishes.
     */
    public static void injectIntoWorldScreen(SelectWorldScreen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        int screenWidth = screen.width;
        int searchWidth = screenWidth / 2 - 10;
        int searchX = 8;
        int searchY = 18;

        // ── Search field ──────────────────────────────────────────────────────
        TextFieldWidget searchField = new TextFieldWidget(
            client.textRenderer,
            searchX, searchY,
            searchWidth, 16,
            Text.translatable("servermanagerplus.search.worlds")
        );
        searchField.setPlaceholderText(Text.literal("🔍 Search worlds by name..."));
        searchField.setMaxLength(128);
        searchField.setText(worldSearchQuery);
        searchField.setChangedListener(query -> {
            worldSearchQuery = query.toLowerCase();
        });

        screen.addDrawableChild(searchField);

        // ── Pinned toggle button ───────────────────────────────────────────────
        ButtonWidget pinnedButton = ButtonWidget.builder(
            getPinnedButtonText(),
            btn -> {
                showPinnedWorldsOnly = !showPinnedWorldsOnly;
                btn.setMessage(getPinnedButtonText());
            }
        ).dimensions(screenWidth / 2 + 4, searchY, searchWidth, 16).build();

        screen.addDrawableChild(pinnedButton);
    }

    private static Text getPinnedButtonText() {
        return showPinnedWorldsOnly
            ? Text.literal("★ Pinned Worlds")
            : Text.literal("☆ All Worlds");
    }

    /**
     * Called by the world list widget to determine if a world should be visible.
     */
    public static boolean shouldShowWorld(String worldName, String displayName) {
        ServerDataManager dm = ServerDataManager.getInstance();

        if (showPinnedWorldsOnly && !dm.isWorldPinned(worldName)) {
            return false;
        }

        if (!worldSearchQuery.isEmpty()) {
            String name = displayName != null ? displayName.toLowerCase() : worldName.toLowerCase();
            String folder = worldName != null ? worldName.toLowerCase() : "";
            return name.contains(worldSearchQuery) || folder.contains(worldSearchQuery);
        }

        return true;
    }
}

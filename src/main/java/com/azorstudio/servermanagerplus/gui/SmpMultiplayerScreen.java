package com.azorstudio.servermanagerplus.gui;

import com.azorstudio.servermanagerplus.data.ServerDataManager;
import com.azorstudio.servermanagerplus.util.CountryLookupUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Full replacement for the vanilla MultiplayerScreen.
 *
 * Features:
 * ─ Search bar (by server name or IP)
 * ─ Pinned toggle (show pinned only)
 * ─ Country flag emoji on each entry with tooltip on hover
 * ─ Right-click entry → Pin/Unpin context menu
 * ─ Pinned servers always appear at the top
 */
@Environment(EnvType.CLIENT)
public class SmpMultiplayerScreen extends MultiplayerScreen {

    // Search / filter state
    private String searchQuery = "";
    private boolean showPinnedOnly = false;

    // Widgets
    private TextFieldWidget searchField;
    private ButtonWidget pinnedToggleBtn;

    // Country code cache for visible entries (emoji string)
    private final Map<String, String> flagCache = new LinkedHashMap<>();

    public SmpMultiplayerScreen(Screen parent) {
        super(parent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        int sw = this.width;
        int barY = 22; // row below the title
        int fieldW = sw / 2 - 10;

        // Search text field
        searchField = new TextFieldWidget(
            textRenderer,
            8, barY,
            fieldW, 18,
            Text.translatable("servermanagerplus.search.servers")
        );
        searchField.setPlaceholderText(Text.literal("🔍  Search by name or IP..."));
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

        // Kick off country lookups for all servers in the list
        kickOffCountryLookups();
    }

    private Text pinnedBtnText() {
        return showPinnedOnly
            ? Text.literal("★  Pinned Only")
            : Text.literal("☆  All Servers");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Render
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Overlay flag + pin decorations on each visible server entry
        MultiplayerServerListWidget widget = getServerListWidget();
        if (widget == null) return;

        List<ServerInfo> visible = getFilteredServers();
        ServerDataManager dm = ServerDataManager.getInstance();

        int entryY = widget.getY() + 4;
        int entryH = 36; // vanilla server entry height

        for (int i = widget.children().size() - 1; i >= 0; i--) {
            // We draw decorations per entry using the widget's scroll offset
        }
        // Note: flag drawing per-entry is done in ServerEntryMixin for correctness.
        // This method handles any screen-level overlays.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Right-click for pin context menu
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Right-click (button == 1) on the list area → open pin context menu
        if (button == 1) {
            ServerInfo selected = getSelectedServer();
            if (selected != null) {
                MinecraftClient.getInstance().setScreen(
                    new PinContextMenu(this, selected.address, selected.name,
                        true, (int)mouseX, (int)mouseY)
                );
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtering helpers (called by ServerListWidgetMixin via accessor)
    // ─────────────────────────────────────────────────────────────────────────

    public boolean shouldIncludeServer(ServerInfo info) {
        ServerDataManager dm = ServerDataManager.getInstance();

        if (showPinnedOnly && !dm.isServerPinned(info.address)) {
            return false;
        }
        if (!searchQuery.isEmpty()) {
            String name = info.name != null ? info.name.toLowerCase() : "";
            String addr = info.address != null ? info.address.toLowerCase() : "";
            return name.contains(searchQuery) || addr.contains(searchQuery);
        }
        return true;
    }

    /**
     * Returns all servers sorted: pinned first, then rest.
     */
    private List<ServerInfo> getFilteredServers() {
        ServerList list = new ServerList(MinecraftClient.getInstance());
        list.loadFile();
        ServerDataManager dm = ServerDataManager.getInstance();

        List<ServerInfo> pinned = new ArrayList<>();
        List<ServerInfo> rest = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            ServerInfo info = list.get(i);
            if (!shouldIncludeServer(info)) continue;
            if (dm.isServerPinned(info.address)) pinned.add(info);
            else rest.add(info);
        }
        List<ServerInfo> result = new ArrayList<>(pinned);
        result.addAll(rest);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Country lookup kickoff
    // ─────────────────────────────────────────────────────────────────────────

    private void kickOffCountryLookups() {
        ServerList list = new ServerList(MinecraftClient.getInstance());
        list.loadFile();
        for (int i = 0; i < list.size(); i++) {
            String addr = list.get(i).address;
            CountryLookupUtil.lookupAsync(addr, code -> {
                if (code != null) flagCache.put(addr, CountryLookupUtil.countryCodeToFlag(code));
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Reflection-free accessor (vanilla exposes this in 1.21.1 via serverListWidget field) */
    private MultiplayerServerListWidget getServerListWidget() {
        // The field is protected in MultiplayerScreen; accessed via mixin accessor
        // For now we return null and rely on the per-entry mixin for rendering
        return null;
    }

    private ServerInfo getSelectedServer() {
        // Accessed via the list widget's getSelectedOrNull
        try {
            var field = MultiplayerScreen.class.getDeclaredField("serverListWidget");
            field.setAccessible(true);
            MultiplayerServerListWidget widget = (MultiplayerServerListWidget) field.get(this);
            if (widget == null) return null;
            var entry = widget.getSelectedOrNull();
            if (entry == null) return null;
            var serverField = entry.getClass().getDeclaredField("server");
            serverField.setAccessible(true);
            return (ServerInfo) serverField.get(entry);
        } catch (Exception e) {
            return null;
        }
    }
}

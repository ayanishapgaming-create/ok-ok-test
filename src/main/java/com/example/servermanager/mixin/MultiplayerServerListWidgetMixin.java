package com.example.servermanager.mixin;

import com.example.servermanager.client.ServerSearchManager;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes the original mixin which shadowed clearEntries() and addEntry() --
 * both inherited from EntryListWidget, not defined in MultiplayerServerListWidget.
 * Without a refmap those shadows fail at runtime ("No refMap loaded").
 *
 * New approach: manipulate the `servers` list in-place BEFORE updateEntries()
 * renders it, then restore the original list AFTER so the master list stays intact.
 * Features preserved: search filtering, pinned-server sorting, country flags.
 */
@Mixin(MultiplayerServerListWidget.class)
public abstract class MultiplayerServerListWidgetMixin {

    // These fields are declared DIRECTLY in MultiplayerServerListWidget, so they
    // are safe to shadow without a refmap.
    @Shadow @Final
    private List<MultiplayerServerListWidget.ServerEntry> servers;

    // Per-instance snapshot of the server list before we filter/sort it.
    @Unique
    private List<MultiplayerServerListWidget.ServerEntry> servermanager$savedServers = null;

    /**
     * HEAD inject: filter and sort the servers list in-place before
     * the vanilla updateEntries() iterates over it to build the display.
     * We do NOT cancel -- we let the vanilla method run on our filtered list.
     */
    @Inject(method = "updateEntries", at = @At("HEAD"))
    private void servermanager$onUpdateEntriesHead(CallbackInfo ci) {
        // Snapshot the original full list so we can restore it at TAIL.
        this.servermanager$savedServers = new ArrayList<>(this.servers);

        String query = ServerSearchManager.searchQuery.trim().toLowerCase();

        // 1. Filter: remove servers that don't match the search query.
        if (!query.isEmpty()) {
            this.servers.removeIf(entry -> {
                ServerInfo info = entry.getServer();
                return !info.name.toLowerCase().contains(query) &&
                       !info.address.toLowerCase().contains(query);
            });
        }

        // 2. Sort: pinned servers bubble to the top, unpinned keep relative order.
        this.servers.sort((a, b) -> {
            boolean aPinned = ServerSearchManager.pinnedServers.contains(a.getServer().address);
            boolean bPinned = ServerSearchManager.pinnedServers.contains(b.getServer().address);
            if (aPinned && !bPinned) return -1;   // a goes first
            if (!aPinned && bPinned) return  1;   // b goes first
            return 0;                              // preserve existing order
        });
    }

    /**
     * TAIL inject: restore the original unfiltered/unsorted server list.
     * This ensures subsequent calls (e.g. after typing another character
     * in the search box) still have the full list to filter from.
     */
    @Inject(method = "updateEntries", at = @At("TAIL"))
    private void servermanager$onUpdateEntriesTail(CallbackInfo ci) {
        if (this.servermanager$savedServers != null) {
            this.servers.clear();
            this.servers.addAll(this.servermanager$savedServers);
            this.servermanager$savedServers = null;
        }
    }
}

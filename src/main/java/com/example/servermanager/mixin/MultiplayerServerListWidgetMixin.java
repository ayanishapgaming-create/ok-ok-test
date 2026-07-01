package com.example.servermanager.mixin;

import com.example.servermanager.client.ServerSearchManager;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MultiplayerServerListWidget.class)
public abstract class MultiplayerServerListWidgetMixin {

    @Shadow @Final
    private List<MultiplayerServerListWidget.ServerEntry> servers;

    @Shadow @Final
    private List<MultiplayerServerListWidget.LanServerEntry> lanServers;

    @Shadow @Final
    private MultiplayerServerListWidget.Entry scanningEntry;

    @Shadow
    protected abstract void clearEntries();

    @Shadow
    protected abstract int addEntry(MultiplayerServerListWidget.Entry entry);

    /**
     * Overrides and intercepts updateEntries to support dynamic pinning,
     * custom search filtering (by Name or IP/Address), and custom sorting.
     */
    @Inject(method = "updateEntries", at = @At("HEAD"), cancellable = true)
    private void onUpdateEntries(CallbackInfo ci) {
        // Clear all entries currently displayed
        this.clearEntries();

        String query = ServerSearchManager.searchQuery.trim().toLowerCase();

        // 1. Filter servers matching search bar (checks both Server Name and Server IP/Address)
        List<MultiplayerServerListWidget.ServerEntry> filteredServers = new ArrayList<>();
        for (MultiplayerServerListWidget.ServerEntry entry : this.servers) {
            ServerInfo serverInfo = entry.getServer();
            if (query.isEmpty() || 
                serverInfo.name.toLowerCase().contains(query) || 
                serverInfo.address.toLowerCase().contains(query)) {
                filteredServers.add(entry);
            }
        }

        // 2. Sort servers so pinned servers are positioned at the very top
        filteredServers.sort((a, b) -> {
            boolean aPinned = ServerSearchManager.pinnedServers.contains(a.getServer().address);
            boolean bPinned = ServerSearchManager.pinnedServers.contains(b.getServer().address);

            if (aPinned && !bPinned) return -1;  // 'a' goes to top
            if (!aPinned && bPinned) return 1;   // 'b' goes to top
            
            // Otherwise maintain original alphabetical or list order
            return 0;
        });

        // 3. Populate filtered and sorted servers into the server list widget
        filteredServers.forEach(this::addEntry);

        // 4. Add standard local LAN network scanning indicator and discovered LAN servers
        this.addEntry(this.scanningEntry);
        this.lanServers.forEach(this::addEntry);

        // Cancel default method execution
        ci.cancel();
    }
}

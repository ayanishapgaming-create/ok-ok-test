package com.azorstudio.servermanagerplus.mixin;

import com.azorstudio.servermanagerplus.data.ServerDataManager;
import com.azorstudio.servermanagerplus.util.CountryLookupUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Injects into each ServerEntry render call to:
 * 1. Draw a pin icon (📌) for pinned servers
 * 2. Draw a country flag emoji with tooltip on hover
 */
@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class ServerEntryMixin {

    @Shadow
    @Final
    private ServerInfo server;

    /**
     * After vanilla renders the server entry, we draw our overlays.
     */
    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private void onRender(DrawContext context, int index, int y, int x,
                          int entryWidth, int entryHeight,
                          int mouseX, int mouseY,
                          boolean hovered, float tickDelta,
                          CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        ServerDataManager dm = ServerDataManager.getInstance();
        String address = server.address;

        // ── Pin indicator ───────────────────────────────────────────────────────
        if (dm.isServerPinned(address)) {
            // Draw pin emoji in top-left corner of entry
            context.drawText(client.textRenderer,
                Text.literal("📌"),
                x + 1, y + 1,
                0xFFD700, // gold color
                true);
        }

        // ── Country flag ────────────────────────────────────────────────────────
        Optional<String> countryCode = dm.getCountryCode(address);

        if (countryCode.isEmpty()) {
            // Trigger async lookup; will be available on next render
            CountryLookupUtil.lookupAsync(address, code -> {
                // Cached automatically by setCountryCode; next render will display it
            });
        } else {
            String code = countryCode.get();
            String flagEmoji = CountryLookupUtil.countryCodeToFlag(code);

            // Draw flag at right side of entry (before ping indicator)
            int flagX = x + entryWidth - 85;
            int flagY = y + (entryHeight / 2) - 4;

            context.drawText(client.textRenderer,
                Text.literal(flagEmoji),
                flagX, flagY,
                0xFFFFFF,
                true);

            // ── Tooltip on hover over flag area ─────────────────────────────────
            boolean overFlag = mouseX >= flagX && mouseX <= flagX + 14
                    && mouseY >= flagY && mouseY <= flagY + 10;

            if (overFlag && hovered) {
                String countryName = CountryLookupUtil.getCountryName(code);
                context.drawTooltip(
                    client.textRenderer,
                    Text.literal(flagEmoji + " " + countryName),
                    mouseX, mouseY
                );
            }
        }
    }
}

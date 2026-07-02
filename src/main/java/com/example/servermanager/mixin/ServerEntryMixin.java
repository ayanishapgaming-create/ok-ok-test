package com.example.servermanager.mixin;

import com.example.servermanager.client.ServerSearchManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class ServerEntryMixin {

    @Shadow @Final
    private ServerInfo server;

    @Shadow @Final
    private MultiplayerScreen screen;

    @Unique
    private int lastX;

    @Unique
    private int lastY;

    /**
     * Shifts the rendering position of the server name to the right by 31 pixels,
     * making room on the left of the name for the Star/Pin icon and Country Flag.
     */
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V"
        )
    )
    private void redirectServerNameDraw(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        if (text != null && text.equals(this.server.name)) {
            // Shift the server name 31 pixels to the right
            context.drawText(textRenderer, text, x + 31, y, color, shadow);
        } else {
            context.drawText(textRenderer, text, x, y, color, shadow);
        }
    }

    /**
     * Injects custom rendering at the end of the ServerEntry render process.
     * This draws the Pin Star, Country Flag, and handles hover tooltip displays.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Save last positions to calculate clicks dynamically in mouseClicked
        this.lastX = x;
        this.lastY = y;

        // 1. Trigger asynchronous GeoIP and Flag downloading
        ServerSearchManager.fetchCountry(this.server.address);

        // 2. Draw Pin Star Icon (★ for pinned, ☆ for unpinned)
        boolean isPinned = ServerSearchManager.pinnedServers.contains(this.server.address);
        String starChar = isPinned ? "★" : "☆";
        int starColor = isPinned ? 0xFFFFD700 : 0x88888888; // Gold if pinned, gray if unpinned
        
        int pinX = x + 35;
        int pinY = y + 1;
        context.drawText(client.textRenderer, starChar, pinX, pinY, starColor, true);

        // 3. Draw Country Flag
        int flagX = x + 47;
        int flagY = y + 1;
        String countryCode = ServerSearchManager.getCountryCode(this.server.address);

        if (countryCode != null) {
            if (countryCode.equals("local")) {
                // For LAN or localhost servers, render yellow brackets "[LAN]" as flag
                context.drawText(client.textRenderer, "[LAN]", flagX - 1, flagY, 0xFFFFAA00, false);
            } else if (countryCode.equals("un") || countryCode.isEmpty()) {
                // Fallback for unknown location flag
                context.drawText(client.textRenderer, "[??]", flagX - 1, flagY, 0x88888888, false);
            } else {
                // Render the dynamically downloaded flag texture
                Identifier flagId = Identifier.of("servermanager", "flags/" + countryCode.toLowerCase());
                // Flags from flagcdn are 20x15, we draw them resized to 16x11
                // Use drawTexture with all required parameters including render pipeline
                context.drawTexture(context.getRenderPipeline(), flagId, flagX, flagY, 0.0f, 0.0f, 16, 11, 16, 11, 16, 11);
            }
        } else {
            // Loading placeholder
            context.drawText(client.textRenderer, "[..]", flagX - 1, flagY, 0x55555555, false);
        }
    }

    /**
     * Intercepts mouse clicking inside the ServerEntry bounds.
     * Toggles server pinned state if the Star icon was clicked.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0) { // Left-click only
            int pinX = this.lastX + 35;
            int pinY = this.lastY + 1;

            // Star hitbox is 10x10 pixels
            if (mouseX >= pinX && mouseX <= pinX + 10 && mouseY >= pinY && mouseY <= pinY + 10) {
                // Toggle pin state
                ServerSearchManager.togglePin(this.server.address);

                // Play standard GUI click sound
                MinecraftClient.getInstance().getSoundManager().play(
                    new PositionedSoundInstance(SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0F, 1.0F, Random.create())
                );

                // Force-reloads the server list widget to apply new pinned order sorting immediately
                ((MultiplayerScreenAccessor) this.screen).getServerListWidget().setServers(this.screen.getServerList());

                cir.setReturnValue(true);
            }
        }
    }
}

package com.azorstudio.servermanagerplus.gui;

import com.azorstudio.servermanagerplus.data.ServerDataManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A small floating context menu that pops up near the mouse cursor
 * offering Pin/Unpin for a server or world entry.
 *
 * Usage:
 *   MinecraftClient.getInstance().setScreen(
 *       new PinContextMenu(parentScreen, serverAddress, serverName));
 */
@Environment(EnvType.CLIENT)
public class PinContextMenu extends Screen {

    private final Screen parent;
    private final String identifier;   // server address or world folder name
    private final String displayName;
    private final boolean isServer;    // true=server, false=world
    private final int menuX, menuY;

    private static final int MENU_W = 140;
    private static final int MENU_H = 50;
    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int BORDER_COLOR = 0xFF5B6EE1;

    public PinContextMenu(Screen parent, String identifier, String displayName,
                          boolean isServer, int mouseX, int mouseY) {
        super(Text.literal("Context Menu"));
        this.parent = parent;
        this.identifier = identifier;
        this.displayName = displayName;
        this.isServer = isServer;

        // Clamp menu so it doesn't go off-screen
        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc != null ? mc.getWindow().getScaledWidth() : 320;
        int sh = mc != null ? mc.getWindow().getScaledHeight() : 240;
        this.menuX = Math.min(mouseX, sw - MENU_W - 4);
        this.menuY = Math.min(mouseY, sh - MENU_H - 4);
    }

    @Override
    protected void init() {
        ServerDataManager dm = ServerDataManager.getInstance();
        boolean pinned = isServer
            ? dm.isServerPinned(identifier)
            : dm.isWorldPinned(identifier);

        String pinLabel = pinned ? "📌 Unpin" : "📌 Pin";

        // Pin / Unpin button
        addDrawableChild(ButtonWidget.builder(
            Text.literal(pinLabel),
            btn -> {
                if (isServer) dm.toggleServerPin(identifier);
                else dm.toggleWorldPin(identifier);
                close();
            }
        ).dimensions(menuX + 4, menuY + 4, MENU_W - 8, 18).build());

        // Cancel button
        addDrawableChild(ButtonWidget.builder(
            Text.literal("✕ Cancel"),
            btn -> close()
        ).dimensions(menuX + 4, menuY + 26, MENU_W - 8, 18).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw background panel
        context.fill(menuX, menuY, menuX + MENU_W, menuY + MENU_H, BG_COLOR);
        // Border
        context.drawBorder(menuX, menuY, MENU_W, MENU_H, BORDER_COLOR);

        // Title
        context.drawText(textRenderer,
            Text.literal(displayName).withColor(0xFFE0C060),
            menuX + 4, menuY - 10, 0xFFFFFF, true);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If click is outside the menu, close
        if (mouseX < menuX || mouseX > menuX + MENU_W
                || mouseY < menuY || mouseY > menuY + MENU_H) {
            close();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void close() {
        if (client != null) client.setScreen(parent);
    }
}

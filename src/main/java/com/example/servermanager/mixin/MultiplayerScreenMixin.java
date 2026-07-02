package com.example.servermanager.mixin;

import com.example.servermanager.client.ServerSearchManager;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private TextFieldWidget searchField;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ServerSearchManager.searchQuery = "";

        int x = this.width / 2 - 100;
        int y = 8;

        this.searchField = new TextFieldWidget(this.textRenderer, x, y, 200, 16, Text.literal("Search Server Name/IP..."));
        this.searchField.setMaxLength(64);
        this.searchField.setPlaceholder(Text.literal("Search..."));

        MultiplayerScreen mScreen = (MultiplayerScreen) (Object) this;
        this.searchField.setChangedListener(query -> {
            ServerSearchManager.searchQuery = query;
            MultiplayerServerListWidget listWidget = ((MultiplayerScreenAccessor) mScreen).getServerListWidget();
            if (listWidget != null) {
                listWidget.setServers(mScreen.getServerList());
            }
        });

        this.addSelectableChild(this.searchField);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.searchField != null) {
            this.searchField.render(context, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void onRemoved(CallbackInfo ci) {
        ServerSearchManager.searchQuery = "";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchField != null) {
            if (mouseX >= this.searchField.getX() && mouseX <= this.searchField.getX() + this.searchField.getWidth() &&
                mouseY >= this.searchField.getY() && mouseY <= this.searchField.getY() + this.searchField.getHeight()) {
                this.searchField.setFocused(true);
                this.setFocused(this.searchField);
                return true;
            } else {
                this.searchField.setFocused(false);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchField != null && this.searchField.isFocused()) {
            if (keyCode == 256) { // ESC
                this.searchField.setFocused(false);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchField != null && this.searchField.isFocused()) {
            return this.searchField.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }
}

package com.example.servermanager.mixin;

import com.example.servermanager.client.ServerSearchManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected TextRenderer textRenderer;

    @Shadow
    protected abstract <T extends Element & Selectable> T addSelectableChild(T child);

    @Unique
    private TextFieldWidget searchField;

    @Unique
    private boolean isMultiplayerScreen() {
        return (Object) this instanceof MultiplayerScreen;
    }

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (isMultiplayerScreen()) {
            MultiplayerScreen mScreen = (MultiplayerScreen) (Object) this;
            ServerSearchManager.searchQuery = "";

            // Centered search bar right below the screen header
            int x = this.width / 2 - 100;
            int y = 8;
            
            this.searchField = new TextFieldWidget(this.textRenderer, x, y, 200, 16, Text.literal("Search Server Name/IP..."));
            this.searchField.setMaxLength(64);
            this.searchField.setPlaceholder(Text.literal("Search..."));

            this.searchField.setChangedListener(query -> {
                ServerSearchManager.searchQuery = query;
                MultiplayerServerListWidget listWidget = ((MultiplayerScreenAccessor) mScreen).getServerListWidget();
                if (listWidget != null) {
                    listWidget.setServers(mScreen.getServerList());
                }
            });

            this.addSelectableChild(this.searchField);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (isMultiplayerScreen() && this.searchField != null) {
            this.searchField.render(context, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void onRemoved(CallbackInfo ci) {
        if (isMultiplayerScreen()) {
            ServerSearchManager.searchQuery = "";
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (isMultiplayerScreen() && this.searchField != null) {
            if (mouseX >= this.searchField.getX() && mouseX <= this.searchField.getX() + this.searchField.getWidth() &&
                mouseY >= this.searchField.getY() && mouseY <= this.searchField.getY() + this.searchField.getHeight()) {
                this.searchField.setFocused(true);
                ((Screen) (Object) this).setFocused(this.searchField);
                cir.setReturnValue(true);
            } else {
                this.searchField.setFocused(false);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (isMultiplayerScreen() && this.searchField != null && this.searchField.isFocused()) {
            if (keyCode == 256) { // ESC key
                this.searchField.setFocused(false);
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (isMultiplayerScreen() && this.searchField != null && this.searchField.isFocused()) {
            cir.setReturnValue(true);
        }
    }
}

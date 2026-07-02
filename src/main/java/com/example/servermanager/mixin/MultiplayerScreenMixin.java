package com.example.servermanager.mixin;

import com.example.servermanager.client.SearchFieldRegistry;
import com.example.servermanager.client.ServerSearchManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ServerSearchManager.searchQuery = "";
        MultiplayerScreen mScreen = (MultiplayerScreen) (Object) this;

        TextFieldWidget field = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 8, 200, 16, Text.literal("Search..."));
        field.setMaxLength(64);
        field.setPlaceholder(Text.literal("Search..."));
        field.setChangedListener(query -> {
            ServerSearchManager.searchQuery = query;
            MultiplayerServerListWidget listWidget = ((MultiplayerScreenAccessor) mScreen).getServerListWidget();
            if (listWidget != null) {
                listWidget.setServers(mScreen.getServerList());
            }
        });

        SearchFieldRegistry.put(mScreen, field);
        this.addSelectableChild(field);
        this.setFocused(field);
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void onRemoved(CallbackInfo ci) {
        ServerSearchManager.searchQuery = "";
        SearchFieldRegistry.remove((MultiplayerScreen) (Object) this);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        TextFieldWidget field = SearchFieldRegistry.get((MultiplayerScreen) (Object) this);
        if (field == null) return;
        boolean inBounds = mouseX >= field.getX() && mouseX <= field.getX() + field.getWidth()
                        && mouseY >= field.getY() && mouseY <= field.getY() + field.getHeight();
        if (inBounds) {
            field.setFocused(true);
            this.setFocused(field);
            cir.setReturnValue(true);
        } else {
            field.setFocused(false);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        TextFieldWidget field = SearchFieldRegistry.get((MultiplayerScreen) (Object) this);
        if (field != null && field.isFocused() && keyCode == 256) {
            field.setFocused(false);
            this.setFocused(null);
        }
    }
}

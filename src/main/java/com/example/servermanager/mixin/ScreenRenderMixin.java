package com.example.servermanager.mixin;

import com.example.servermanager.client.SearchFieldRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenRenderMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof MultiplayerScreen)) return;
        TextFieldWidget field = SearchFieldRegistry.get((MultiplayerScreen) (Object) this);
        if (field != null) {
            field.render(context, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof MultiplayerScreen)) return;
        TextFieldWidget field = SearchFieldRegistry.get((MultiplayerScreen) (Object) this);
        if (field == null) return;
        boolean inBounds = mouseX >= field.getX() && mouseX <= field.getX() + field.getWidth()
                        && mouseY >= field.getY() && mouseY <= field.getY() + field.getHeight();
        if (inBounds) {
            field.setFocused(true);
            ((Screen) (Object) this).setFocused(field);
            cir.setReturnValue(true);
        } else {
            field.setFocused(false);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof MultiplayerScreen)) return;
        TextFieldWidget field = SearchFieldRegistry.get((MultiplayerScreen) (Object) this);
        if (field != null && field.isFocused() && keyCode == 256) {
            field.setFocused(false);
            ((Screen) (Object) this).setFocused(null);
        }
    }
}

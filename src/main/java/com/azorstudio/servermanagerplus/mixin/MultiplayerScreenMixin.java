package com.azorstudio.servermanagerplus.mixin;

import com.azorstudio.servermanagerplus.data.ServerDataManager;
import com.azorstudio.servermanagerplus.gui.EnhancedMultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla MultiplayerScreen with our enhanced version
 * by redirecting the constructor init call.
 */
@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    /**
     * After the screen initializes, we inject our UI additions.
     * We can't fully replace the screen here, so we delegate via the EnhancedMultiplayerScreen
     * companion class which is opened by our mixin on the parent screen's button press.
     */
    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // Mark that mod is active; actual UI injection is done via ServerListWidgetMixin
        // and the search/pin overlays added by EnhancedMultiplayerScreen
        EnhancedMultiplayerScreen.injectIntoMultiplayerScreen((MultiplayerScreen)(Object)this);
    }
}

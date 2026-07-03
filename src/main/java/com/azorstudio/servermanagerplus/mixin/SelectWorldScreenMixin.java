package com.azorstudio.servermanagerplus.mixin;

import com.azorstudio.servermanagerplus.gui.EnhancedWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts the world selection screen init to add our search + pin UI.
 */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        EnhancedWorldScreen.injectIntoWorldScreen((SelectWorldScreen)(Object)this);
    }
}

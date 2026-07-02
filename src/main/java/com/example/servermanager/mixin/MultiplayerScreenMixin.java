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
}

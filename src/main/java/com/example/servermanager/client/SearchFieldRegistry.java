package com.example.servermanager.client;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.WeakHashMap;

public class SearchFieldRegistry {
    private static final WeakHashMap<MultiplayerScreen, TextFieldWidget> FIELDS = new WeakHashMap<>();

    public static void put(MultiplayerScreen screen, TextFieldWidget field) {
        FIELDS.put(screen, field);
    }

    public static TextFieldWidget get(MultiplayerScreen screen) {
        return FIELDS.get(screen);
    }

    public static void remove(MultiplayerScreen screen) {
        FIELDS.remove(screen);
    }
}

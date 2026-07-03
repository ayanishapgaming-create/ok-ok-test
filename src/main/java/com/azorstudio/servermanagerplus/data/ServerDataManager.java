package com.azorstudio.servermanagerplus.data;

import com.azorstudio.servermanagerplus.ServerManagerPlusClient;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages persistent data for Server Manager+:
 * - Pinned server addresses
 * - Pinned world names
 * - Cached country codes for server IPs
 */
public class ServerDataManager {

    private static final ServerDataManager INSTANCE = new ServerDataManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("servermanagerplus/data.json");

    // Pinned server addresses (set for O(1) lookup)
    private final Set<String> pinnedServers = new HashSet<>();
    // Pinned world folder names
    private final Set<String> pinnedWorlds = new HashSet<>();
    // Cached country codes: ip/host -> ISO 2-letter code
    private final Map<String, String> serverCountryCodes = new HashMap<>();

    private ServerDataManager() {}

    public static ServerDataManager getInstance() {
        return INSTANCE;
    }

    // ─── Pin API ────────────────────────────────────────────────────────────────

    public boolean isServerPinned(String address) {
        return pinnedServers.contains(normalizeAddress(address));
    }

    public void toggleServerPin(String address) {
        String key = normalizeAddress(address);
        if (pinnedServers.contains(key)) {
            pinnedServers.remove(key);
        } else {
            pinnedServers.add(key);
        }
        save();
    }

    public boolean isWorldPinned(String worldName) {
        return pinnedWorlds.contains(worldName);
    }

    public void toggleWorldPin(String worldName) {
        if (pinnedWorlds.contains(worldName)) {
            pinnedWorlds.remove(worldName);
        } else {
            pinnedWorlds.add(worldName);
        }
        save();
    }

    // ─── Country Code API ────────────────────────────────────────────────────────

    public Optional<String> getCountryCode(String address) {
        return Optional.ofNullable(serverCountryCodes.get(normalizeAddress(address)));
    }

    public void setCountryCode(String address, String countryCode) {
        serverCountryCodes.put(normalizeAddress(address), countryCode.toUpperCase());
        save();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private String normalizeAddress(String address) {
        if (address == null) return "";
        return address.toLowerCase().trim();
    }

    // ─── Persistence ─────────────────────────────────────────────────────────────

    public void load() {
        if (!Files.exists(DATA_FILE)) return;
        try (Reader reader = Files.newBufferedReader(DATA_FILE)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) return;

            pinnedServers.clear();
            if (root.has("pinnedServers")) {
                root.getAsJsonArray("pinnedServers")
                    .forEach(e -> pinnedServers.add(e.getAsString()));
            }

            pinnedWorlds.clear();
            if (root.has("pinnedWorlds")) {
                root.getAsJsonArray("pinnedWorlds")
                    .forEach(e -> pinnedWorlds.add(e.getAsString()));
            }

            serverCountryCodes.clear();
            if (root.has("countryCodes")) {
                JsonObject codes = root.getAsJsonObject("countryCodes");
                codes.entrySet().forEach(e ->
                    serverCountryCodes.put(e.getKey(), e.getValue().getAsString()));
            }
        } catch (IOException e) {
            ServerManagerPlusClient.LOGGER.error("Failed to load Server Manager+ data", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            JsonObject root = new JsonObject();

            JsonArray servers = new JsonArray();
            pinnedServers.forEach(servers::add);
            root.add("pinnedServers", servers);

            JsonArray worlds = new JsonArray();
            pinnedWorlds.forEach(worlds::add);
            root.add("pinnedWorlds", worlds);

            JsonObject codes = new JsonObject();
            serverCountryCodes.forEach(codes::addProperty);
            root.add("countryCodes", codes);

            try (Writer writer = Files.newBufferedWriter(DATA_FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            ServerManagerPlusClient.LOGGER.error("Failed to save Server Manager+ data", e);
        }
    }
}

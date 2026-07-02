package com.example.servermanager.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSearchManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ServerManager-Search");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File PINS_FILE = FabricLoader.getInstance().getConfigDir().resolve("servermanager_pins.json").toFile();

    // Search query typed by the user on the client screen
    public static String searchQuery = "";

    // Set of pinned server IP/address strings
    public static final Set<String> pinnedServers = Collections.synchronizedSet(new HashSet<>());

    // Cache: Server IP/Address -> Country Name
    public static final Map<String, String> ipCountryCache = new ConcurrentHashMap<>();

    // Cache: Server IP/Address -> Country Code (e.g. "US", "CA")
    public static final Map<String, String> ipFlagCache = new ConcurrentHashMap<>();

    // Tracks currently fetching IPs to avoid duplicate requests
    private static final Set<String> fetchingIPs = Collections.synchronizedSet(new HashSet<>());

    // Tracks registered country code flags to avoid re-registering textures
    private static final Set<String> registeredFlags = Collections.synchronizedSet(new HashSet<>());

    static {
        loadPins();
    }

    /**
     * Loads the pinned servers list from config file.
     */
    public static void loadPins() {
        if (PINS_FILE.exists()) {
            try (FileReader reader = new FileReader(PINS_FILE)) {
                Set<String> pins = GSON.fromJson(reader, new TypeToken<HashSet<String>>() {}.getType());
                if (pins != null) {
                    pinnedServers.clear();
                    pinnedServers.addAll(pins);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load pinned servers.", e);
            }
        }
    }

    /**
     * Saves the pinned servers list to config file.
     */
    public static void savePins() {
        try {
            File parent = PINS_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(PINS_FILE)) {
                GSON.toJson(pinnedServers, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save pinned servers.", e);
        }
    }

    /**
     * Toggles the pin state for a server IP.
     */
    public static void togglePin(String ip) {
        if (ip == null || ip.isEmpty()) return;
        if (pinnedServers.contains(ip)) {
            pinnedServers.remove(ip);
        } else {
            pinnedServers.add(ip);
        }
        savePins();
    }

    /**
     * Resolves the hostname and fetches country/flag asynchronously using freeipapi.com
     */
    public static void fetchCountry(String ipOrHostname) {
        if (ipOrHostname == null || ipOrHostname.isEmpty()) return;
        if (ipCountryCache.containsKey(ipOrHostname) || fetchingIPs.contains(ipOrHostname)) return;

        fetchingIPs.add(ipOrHostname);

        CompletableFuture.runAsync(() -> {
            try {
                // Strip port from address if present
                String ip = ipOrHostname;
                int colonIndex = ip.indexOf(':');
                if (colonIndex != -1) {
                    ip = ip.substring(0, colonIndex);
                }

                // Skip local addresses
                if (ip.equalsIgnoreCase("localhost") || ip.startsWith("127.") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
                    ipCountryCache.put(ipOrHostname, "Local Network");
                    ipFlagCache.put(ipOrHostname, "local");
                    return;
                }

                // Resolve DNS to IP
                InetAddress address = InetAddress.getByName(ip);
                String resolvedIp = address.getHostAddress();

                // Call FreeIPAPI (unlimited and completely free)
                URL url = new URL("https://freeipapi.com/api/json/" + resolvedIp);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                if (conn.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        if (json != null && json.has("countryCode") && json.has("countryName")) {
                            String countryCode = json.get("countryCode").getAsString().toUpperCase();
                            String countryName = json.get("countryName").getAsString();

                            ipCountryCache.put(ipOrHostname, countryName);
                            ipFlagCache.put(ipOrHostname, countryCode);

                            // Download and register flag icon
                            downloadFlagTexture(countryCode);
                        }
                    }
                } else {
                    LOGGER.warn("GeoIP API returned HTTP " + conn.getResponseCode() + " for " + resolvedIp);
                    ipCountryCache.put(ipOrHostname, "Unknown Location");
                    ipFlagCache.put(ipOrHostname, "un");
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to lookup location for: " + ipOrHostname, e);
                ipCountryCache.put(ipOrHostname, "Unknown Location");
                ipFlagCache.put(ipOrHostname, "un");
            } finally {
                fetchingIPs.remove(ipOrHostname);
            }
        });
    }

    /**
     * Downloads flag png from CDN and registers it in Minecraft's TextureManager.
     */
    private static void downloadFlagTexture(String countryCode) {
        String codeLower = countryCode.toLowerCase();
        if (registeredFlags.contains(codeLower) || codeLower.equals("local") || codeLower.equals("un")) return;

        try {
            // High-quality flag CDN
            URL url = new URL("https://flagcdn.com/w20/" + codeLower + ".png");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream()) {
                    NativeImage nativeImage = NativeImage.read(in);
                    
                    // Texture registration must happen on client render thread
                    MinecraftClient.getInstance().execute(() -> {
                        try {
                            NativeImageBackedTexture texture = new NativeImageBackedTexture("servermanager_flags_" + codeLower, nativeImage);
                            Identifier id = Identifier.of("servermanager", "flags/" + codeLower);
                            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
                            registeredFlags.add(codeLower);
                        } catch (Exception e) {
                            LOGGER.error("Failed to register flag texture for: " + codeLower, e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to download flag image for code: " + countryCode, e);
        }
    }

    public static String getCountryName(String ip) {
        return ipCountryCache.getOrDefault(ip, "Locating Server...");
    }

    public static String getCountryCode(String ip) {
        return ipFlagCache.get(ip);
    }
}

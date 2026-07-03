package com.azorstudio.servermanagerplus.util;

import com.azorstudio.servermanagerplus.ServerManagerPlusClient;
import com.azorstudio.servermanagerplus.data.ServerDataManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Async IP → country code lookup using the free ip-api.com service.
 * Results are cached in ServerDataManager to avoid repeated requests.
 */
public class CountryLookupUtil {

    private static final ExecutorService EXECUTOR =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "smp-geo-lookup");
                t.setDaemon(true);
                return t;
            });

    // Emoji flag from ISO country code: each letter maps to a regional indicator symbol
    public static String countryCodeToFlag(String iso2) {
        if (iso2 == null || iso2.length() != 2) return "🌐";
        int[] codePoints = iso2.toUpperCase().chars()
                .map(c -> 0x1F1E0 + (c - 'A'))
                .toArray();
        return new String(codePoints, 0, codePoints.length);
    }

    /**
     * Get country code for a server address, using cache first.
     * Calls {@code callback} on the main thread with the ISO-2 code (or null if unknown).
     */
    public static void lookupAsync(String serverAddress, Consumer<String> callback) {
        // Strip port
        String host = serverAddress.split(":")[0].trim();

        // Skip localhost / LAN addresses
        if (host.equalsIgnoreCase("localhost")
                || host.startsWith("127.")
                || host.startsWith("192.168.")
                || host.startsWith("10.")
                || host.startsWith("172.")) {
            callback.accept("LAN");
            return;
        }

        // Use cached value immediately if present
        Optional<String> cached = ServerDataManager.getInstance().getCountryCode(serverAddress);
        if (cached.isPresent()) {
            callback.accept(cached.get());
            return;
        }

        EXECUTOR.submit(() -> {
            try {
                String apiUrl = "http://ip-api.com/json/" + host + "?fields=countryCode,status";
                URL url = URI.create(apiUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestProperty("User-Agent", "ServerManagerPlus/1.0");

                if (conn.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        if ("success".equals(json.get("status").getAsString())) {
                            String code = json.get("countryCode").getAsString();
                            ServerDataManager.getInstance().setCountryCode(serverAddress, code);
                            callback.accept(code);
                            return;
                        }
                    }
                }
            } catch (IOException e) {
                ServerManagerPlusClient.LOGGER.warn(
                    "Country lookup failed for {}: {}", serverAddress, e.getMessage());
            }
            callback.accept(null);
        });
    }

    /**
     * Returns full country name for tooltip use.
     */
    public static String getCountryName(String iso2) {
        if (iso2 == null) return "Unknown";
        try {
            java.util.Locale locale = new java.util.Locale("", iso2);
            String name = locale.getDisplayCountry(java.util.Locale.ENGLISH);
            return name.isEmpty() ? iso2 : name;
        } catch (Exception e) {
            return iso2;
        }
    }
}

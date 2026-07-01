package com.example.servermanager.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ServerManager-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("servermanager.json").toFile();

    // Configurable Settings
    public String welcomeMessage = "&aWelcome back, &e%player%&a! Enjoy your stay on the server! \u2764";
    public boolean maintenanceMode = false;
    public String maintenanceKickMessage = "&cThis server is currently undergoing scheduled maintenance.\n&7Please check back later or contact administrators!";
    public boolean enableCustomJoinMessages = true;
    public String customJoinMessage = "&7[&a+&7] &e%player% &7has entered the realm.";
    public boolean enableCustomLeaveMessages = true;
    public String customLeaveMessage = "&7[&c-&7] &e%player% &7has departed.";
    public String serverMotd = "&bServer Manager &7| &aOnline and Running smoothly!";

    /**
     * Loads the config file from disk. If the file doesn't exist, a default config is saved and loaded.
     */
    public static ModConfig load() {
        ModConfig config;
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, ModConfig.class);
                if (config == null) {
                    config = new ModConfig();
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load Server Manager config, generating a default one.", e);
                config = new ModConfig();
            }
        } else {
            config = new ModConfig();
            config.save();
        }
        return config;
    }

    /**
     * Saves the current configuration state to disk.
     */
    public void save() {
        try {
            // Ensure parent directories exist
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save Server Manager config.", e);
        }
    }
}

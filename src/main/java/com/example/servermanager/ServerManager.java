package com.example.servermanager;

import com.example.servermanager.command.ServerManagerCommand;
import com.example.servermanager.config.ModConfig;
import com.example.servermanager.util.TextUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerManager implements ModInitializer {
    public static final String MOD_ID = "servermanager";
    public static final Logger LOGGER = LoggerFactory.getLogger("ServerManager");

    private static ModConfig config;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Server Manager Mod (1.21.1)...");

        // Load configuration
        config = ModConfig.load();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ServerManagerCommand.register(dispatcher);
        });

        // Register Connection (Join/Leave) Events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // 1. Check Maintenance Mode
            if (config.maintenanceMode && !server.getPlayerManager().isOperator(player.getGameProfile())) {
                LOGGER.info("Player {} tried to join during maintenance mode. Disconnecting.", player.getName().getString());
                handler.disconnect(TextUtil.format(config.maintenanceKickMessage));
                return;
            }

            // 2. Custom Join Message (Broadcasted to all players)
            if (config.enableCustomJoinMessages) {
                String joinMsg = config.customJoinMessage.replace("%player%", player.getName().getString());
                Text formattedJoin = TextUtil.format(joinMsg);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.sendMessage(formattedJoin, false);
                }
                server.sendMessage(formattedJoin); // Log to server console
            }

            // 3. Custom Welcome Message (Sent to joining player)
            if (config.welcomeMessage != null && !config.welcomeMessage.isEmpty()) {
                String welcomeMsg = config.welcomeMessage.replace("%player%", player.getName().getString());
                player.sendMessage(TextUtil.format(welcomeMsg), false);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Custom Leave Message (Broadcasted to all players)
            if (config.enableCustomLeaveMessages) {
                String leaveMsg = config.customLeaveMessage.replace("%player%", player.getName().getString());
                Text formattedLeave = TextUtil.format(leaveMsg);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.sendMessage(formattedLeave, false);
                }
                server.sendMessage(formattedLeave); // Log to server console
            }
        });

        LOGGER.info("Server Manager Mod initialized successfully!");
    }

    public static ModConfig getConfig() {
        return config;
    }

    public static void setConfig(ModConfig newConfig) {
        config = newConfig;
    }
}

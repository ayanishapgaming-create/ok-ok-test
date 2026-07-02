package com.example.servermanager.command;

import com.example.servermanager.ServerManager;
import com.example.servermanager.config.ModConfig;
import com.example.servermanager.util.TextUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ServerManagerCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Main command: /servermanager (and alias /sm)
        registerCommand(dispatcher, "servermanager");
        registerCommand(dispatcher, "sm");
    }

    private static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, String commandName) {
        dispatcher.register(literal(commandName)
            .requires(source -> source.hasPermissionLevel(2)) // Requires OP Level 2
            .then(literal("reload")
                .executes(context -> executeReload(context.getSource()))
            )
            .then(literal("status")
                .executes(context -> executeStatus(context.getSource()))
            )
            .then(literal("maintenance")
                .then(literal("on")
                    .executes(context -> executeMaintenance(context.getSource(), true))
                )
                .then(literal("off")
                    .executes(context -> executeMaintenance(context.getSource(), false))
                )
            )
            .then(literal("broadcast")
                .then(argument("message", StringArgumentType.greedyString())
                    .executes(context -> executeBroadcast(context.getSource(), StringArgumentType.getString(context, "message")))
                )
            )
        );
    }

    private static int executeReload(ServerCommandSource source) {
        // Load new config from file
        ModConfig config = ModConfig.load();
        ServerManager.setConfig(config);

        source.sendFeedback(() -> TextUtil.format("&7[&bServerManager&7] &aConfiguration successfully reloaded!"), true);
        return 1;
    }

    private static int executeStatus(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        ModConfig config = ServerManager.getConfig();

        // Calculate performance and memory statistics
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        int playersOnline = server.getCurrentPlayerCount();
        int maxPlayers = server.getMaxPlayerCount();

        source.sendFeedback(() -> TextUtil.format("&7============== &b&lServer Manager Status &r&7=============="), false);
        source.sendFeedback(() -> TextUtil.format("&eMaintenance Mode: " + (config.maintenanceMode ? "&c&lENABLED" : "&a&lDISABLED")), false);
        source.sendFeedback(() -> TextUtil.format("&ePlayers Online: &a" + playersOnline + "&7 / &a" + maxPlayers), false);
        source.sendFeedback(() -> TextUtil.format("&eMemory Usage: &a" + usedMemory + "MB&7 / &a" + totalMemory + "MB &7(Max: &a" + maxMemory + "MB)"), false);
        source.sendFeedback(() -> TextUtil.format("&eServer Version: &aMinecraft " + server.getVersion()), false);
        source.sendFeedback(() -> TextUtil.format("&7=================================================="), false);

        return 1;
    }

    private static int executeMaintenance(ServerCommandSource source, boolean enable) {
        ModConfig config = ServerManager.getConfig();
        config.maintenanceMode = enable;
        config.save(); // Save changes to file

        String statusString = enable ? "&cENABLED" : "&aDISABLED";
        source.sendFeedback(() -> TextUtil.format("&7[&bServerManager&7] &eMaintenance Mode has been " + statusString), true);

        if (enable) {
            // Kick all non-admin players immediately
            MinecraftServer server = source.getServer();
            Text kickReason = TextUtil.format(config.maintenanceKickMessage);

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!server.getPlayerManager().isOperator(player.getPlayerConfigEntry())) {
                    player.networkHandler.disconnect(kickReason);
                }
            }
            source.sendFeedback(() -> TextUtil.format("&7[&bServerManager&7] &6All non-operators have been kicked for maintenance."), true);
        }

        return 1;
    }

    private static int executeBroadcast(ServerCommandSource source, String message) {
        MinecraftServer server = source.getServer();

        // Build a beautiful broadcast message format
        String broadcastFormat = "&7[&4&lBROADCAST&r&7] &d" + message;
        Text formattedBroadcast = TextUtil.format(broadcastFormat);

        // Send to all online players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(formattedBroadcast, false);
        }

        // Also send to server console
        server.sendMessage(formattedBroadcast);

        return 1;
    }
}

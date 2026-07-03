package com.azorstudio.servermanagerplus;

import com.azorstudio.servermanagerplus.data.ServerDataManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class ServerManagerPlusClient implements ClientModInitializer {

    public static final String MOD_ID = "servermanagerplus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Server Manager + initialized by Azor Studio");
        // Load persisted pin/data on startup
        ServerDataManager.getInstance().load();
    }
}

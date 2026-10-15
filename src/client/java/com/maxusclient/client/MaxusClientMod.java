package com.maxusclient.client;

import com.maxusclient.client.controller.InputController;
import com.maxusclient.client.controller.ModuleController;
import com.maxusclient.client.module.ModuleManager;
import com.maxusclient.client.render.WorldRenderer;
import com.maxusclient.client.service.BlockRegistryService;
import com.maxusclient.client.service.ConfigService;
import net.fabricmc.api.ClientModInitializer;

public class MaxusClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1. Initialize generic module framework
        ModuleManager.init();

        // 2. Load configurations for modules and settings
        ConfigService.init();

        // 3. Cache block registry entries
        BlockRegistryService.ensureCache();

        // 4. Register world rendering dispatcher
        WorldRenderer.register();

        // 5. Register module ticker & chunk scanner
        ModuleController.register();

        // 6. Register input keybinds (. key)
        InputController.register();
    }
}

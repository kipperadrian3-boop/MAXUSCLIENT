package com.maxusclient.client.render;

import com.maxusclient.client.module.Module;
import com.maxusclient.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public class WorldRenderer {
    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            for (Module m : ModuleManager.getActiveModules()) {
                m.onRender(context);
            }
        });
    }
}

package com.maxusclient.client.service;

import com.google.gson.*;
import com.maxusclient.client.module.Module;
import com.maxusclient.client.module.ModuleManager;
import com.maxusclient.client.module.esp.BlockEsp;
import com.maxusclient.client.setting.BlockListSetting;
import com.maxusclient.client.setting.BooleanSetting;
import com.maxusclient.client.setting.ColorSetting;
import com.maxusclient.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ConfigService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "maxusclient.json");

    public static void init() {
        load();
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Module module : ModuleManager.getModules()) {
                if (!root.has(module.getName())) continue;
                JsonObject modObj = root.getAsJsonObject(module.getName());

                if (modObj.has("enabled")) {
                    module.setEnabled(modObj.get("enabled").getAsBoolean());
                }

                if (modObj.has("settings")) {
                    JsonObject setObj = modObj.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        if (!setObj.has(setting.getName())) continue;

                        if (setting instanceof BooleanSetting bs) {
                            bs.set(setObj.get(bs.getName()).getAsBoolean());
                        } else if (setting instanceof ColorSetting cs) {
                            cs.set(setObj.get(cs.getName()).getAsInt());
                        } else if (setting instanceof BlockListSetting bls) {
                            JsonObject blocksObj = setObj.getAsJsonObject(bls.getName());
                            bls.get().clear();
                            for (Map.Entry<String, JsonElement> entry : blocksObj.entrySet()) {
                                bls.get().put(entry.getKey(), entry.getValue().getAsInt());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Update block caches
        BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);
        if (blockEsp != null) {
            blockEsp.updateBlockCache();
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();

            for (Module module : ModuleManager.getModules()) {
                JsonObject modObj = new JsonObject();
                modObj.addProperty("enabled", module.isEnabled());

                JsonObject setObj = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting bs) {
                        setObj.addProperty(bs.getName(), bs.get());
                    } else if (setting instanceof ColorSetting cs) {
                        setObj.addProperty(cs.getName(), cs.get());
                    } else if (setting instanceof BlockListSetting bls) {
                        JsonObject blocksObj = new JsonObject();
                        for (Map.Entry<String, Integer> entry : bls.get().entrySet()) {
                            blocksObj.addProperty(entry.getKey(), entry.getValue());
                        }
                        setObj.add(bls.getName(), blocksObj);
                    }
                }
                modObj.add("settings", setObj);
                root.add(module.getName(), modObj);
            }

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Update caches
        BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);
        if (blockEsp != null) {
            blockEsp.updateBlockCache();
        }
    }
}

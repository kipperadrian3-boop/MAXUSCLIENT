package com.maxusclient.client.setting;

import java.util.LinkedHashMap;
import java.util.Map;

public class BlockListSetting extends Setting<Map<String, Integer>> {
    public BlockListSetting(String name, String description) {
        // Completely empty by default - nothing hardcoded!
        super(name, description, new LinkedHashMap<>());
    }

    public void addBlock(String id, int color) {
        this.value.put(id, color);
    }

    public void removeBlock(String id) {
        this.value.remove(id);
    }

    public boolean contains(String id) {
        return this.value.containsKey(id);
    }

    public int getColor(String id) {
        return this.value.getOrDefault(id, 0xFFFF69B4);
    }

    public void setColor(String id, int color) {
        if (this.value.containsKey(id)) {
            this.value.put(id, color);
        }
    }
}

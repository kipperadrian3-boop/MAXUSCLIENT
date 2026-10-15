package com.maxusclient.client.module;

import com.maxusclient.client.module.esp.BlockEsp;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.clear();
        // Register modules
        register(new BlockEsp());
    }

    public static void register(Module module) {
        modules.add(module);
    }

    public static List<Module> getModules() {
        return modules;
    }

    public static List<Module> getModulesByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) {
                list.add(m);
            }
        }
        return list;
    }

    public static List<Module> getActiveModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.isEnabled()) {
                list.add(m);
            }
        }
        return list;
    }

    public static List<Module> getDisabledModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (!m.isEnabled()) {
                list.add(m);
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) {
            if (m.getClass() == clazz) {
                return (T) m;
            }
        }
        return null;
    }
}

package com.maxusclient.client.gui;

import com.maxusclient.client.module.Category;
import com.maxusclient.client.module.Module;
import com.maxusclient.client.module.ModuleManager;
import com.maxusclient.client.module.esp.BlockEsp;
import com.maxusclient.client.service.ConfigService;
import com.maxusclient.client.util.RenderUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaxusMenuScreen extends Screen {

    // Track collapsed state per Category (default unfolded)
    private final Map<Category, Boolean> categoryCollapsed = new HashMap<>();

    public MaxusMenuScreen() {
        super(Component.literal("Maxus Client"));
        for (Category cat : Category.values()) {
            categoryCollapsed.put(cat, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // NOTE: No background dimming at all - Minecraft stays 100% bright and clear!

        Category[] categories = Category.values();
        int catCount = categories.length;
        // 25% smaller: width 66px per category column
        int colW = Math.min(66, Math.max(52, (this.width - 20) / catCount));
        int spacing = 3;
        int totalW = catCount * colW + (catCount - 1) * spacing;
        int startX = Math.max(4, (this.width - totalW) / 2);
        int startY = 10;

        int curX = startX;
        for (Category cat : categories) {
            boolean isCollapsed = categoryCollapsed.getOrDefault(cat, false);
            List<Module> modules = ModuleManager.getModulesByCategory(cat);

            int headerH = 12; // 25% smaller
            int itemH = 12;   // 25% smaller
            int bodyH = (isCollapsed || modules.isEmpty()) ? 0 : (modules.size() * itemH);
            int totalH = headerH + bodyH;

            // Header background & border
            RenderUtil.renderBox(graphics, curX, startY, colW, headerH, 0xFF9333EA, 0xFFA855F7);

            // Header title
            String title = this.font.plainSubstrByWidth(cat.getDisplayName(), colW - 10);
            graphics.text(this.font, title, curX + 2, startY + 2, 0xFFFFFFFF, true);

            // Collapse arrow
            String arrow = isCollapsed ? "+" : "-";
            graphics.text(this.font, arrow, curX + colW - 7, startY + 2, 0xFFFFFFFF, false);

            // Modules listed vertically directly underneath ("untereinander")
            if (!isCollapsed && !modules.isEmpty()) {
                RenderUtil.renderBox(graphics, curX, startY + headerH, colW, bodyH, 0xFF14151F, 0xFF242636);

                int itemY = startY + headerH;
                for (Module mod : modules) {
                    boolean enabled = mod.isEnabled();
                    boolean hover = mouseX >= curX && mouseX <= curX + colW && mouseY >= itemY && mouseY <= itemY + itemH;

                    int modBg;
                    int modText;
                    if (enabled) {
                        modBg = hover ? 0xFFA855F7 : 0xFF9333EA;
                        modText = 0xFFFFFFFF;
                    } else {
                        modBg = hover ? 0xFF202230 : 0xFF14151F;
                        modText = 0xFFC0C2D0;
                    }

                    graphics.fill(curX + 1, itemY, curX + colW - 1, itemY + itemH, modBg);
                    String modName = this.font.plainSubstrByWidth(mod.getName(), colW - 15);
                    graphics.text(this.font, modName, curX + 3, itemY + 2, modText, enabled);

                    // Mini status icon (green check when enabled, red cross when disabled)
                    int iconSize = 8;
                    int iconX = curX + colW - iconSize - 2;
                    int iconY = itemY + 2;
                    RenderUtil.renderStatusIcon(graphics, iconX, iconY, iconSize, enabled);

                    itemY += itemH;
                }
            }

            curX += colW + spacing;
        }

        // Helper text at the bottom-left corner
        graphics.text(this.font, "Left click: Toggle | Right click: Settings", 4, this.height - 10, 0xFF888B9E, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        Category[] categories = Category.values();
        int catCount = categories.length;
        int colW = Math.min(66, Math.max(52, (this.width - 20) / catCount));
        int spacing = 3;
        int totalW = catCount * colW + (catCount - 1) * spacing;
        int startX = Math.max(4, (this.width - totalW) / 2);
        int startY = 10;

        int curX = startX;
        for (Category cat : categories) {
            boolean isCollapsed = categoryCollapsed.getOrDefault(cat, false);
            int headerH = 12;

            // Click on Header (toggle collapse)
            if (mouseX >= curX && mouseX <= curX + colW && mouseY >= startY && mouseY <= startY + headerH) {
                categoryCollapsed.put(cat, !isCollapsed);
                return true;
            }

            // Click on items underneath
            if (!isCollapsed) {
                List<Module> modules = ModuleManager.getModulesByCategory(cat);
                int itemH = 12;
                int itemY = startY + headerH;

                for (Module mod : modules) {
                    if (mouseX >= curX && mouseX <= curX + colW && mouseY >= itemY && mouseY <= itemY + itemH) {
                        if (button == 0) {
                            // Left-click: toggle module on/off
                            mod.toggle();
                            ConfigService.save();
                            return true;
                        } else if (button == 1) {
                            // Right-click: close this menu and open dedicated centered settings window!
                            if (mod instanceof BlockEsp) {
                                Minecraft.getInstance().setScreenAndShow(new BlockEspSettingsScreen(this));
                                return true;
                            }
                        }
                    }
                    itemY += itemH;
                }
            }

            curX += colW + spacing;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}

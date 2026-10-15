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

    // Top Bar Tabs (from Meteor Client)
    private static final String[] TABS = {
        "Modules", "Config", "GUI", "HUD", "Friends", "Macros", "Profiles"
    };
    private String currentTab = "Modules";

    // Track collapsed state per Category
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
        // Semi-transparent background dim
        graphics.fill(0, 0, this.width, this.height, 0x60000000);

        // 1. Render Top Navigation Bar (Modules, Config, GUI, HUD, etc.)
        renderTopBar(graphics, mouseX, mouseY);

        // 2. Render Tab Content
        if ("Modules".equals(currentTab)) {
            renderModulesCategories(graphics, mouseX, mouseY);
        } else {
            renderPlaceholderTab(graphics, mouseX, mouseY);
        }

        // 3. Render bottom-left helper hints (like Meteor Client)
        int hintY = this.height - 26;
        graphics.text(this.font, "Left click - Toggle", 12, hintY, 0xFF888B9E, false);
        graphics.text(this.font, "Right click - Open module settings", 12, hintY + 11, 0xFF888B9E, false);
    }

    private void renderTopBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int tabCount = TABS.length;
        int tabW = 68;
        int tabH = 20;
        int barW = tabCount * tabW;
        int startX = (this.width - barW) / 2;
        int startY = 12;

        // Background container for top tabs
        RenderUtil.renderBox(graphics, startX - 2, startY - 2, barW + 4, tabH + 4, 0xFF12131C, 0xFF252737);

        int curX = startX;
        for (String tab : TABS) {
            boolean isActive = tab.equals(currentTab);
            boolean isHover = mouseX >= curX && mouseX <= curX + tabW && mouseY >= startY && mouseY <= startY + tabH;

            int bg;
            int textCol;
            if (isActive) {
                bg = 0xFF2C2F42;
                textCol = 0xFFFFFFFF;
            } else if (isHover) {
                bg = 0xFF1E202E;
                textCol = 0xFFD8D9E4;
            } else {
                bg = 0xFF12131C;
                textCol = 0xFF8F91A2;
            }

            graphics.fill(curX, startY, curX + tabW, startY + tabH, bg);
            if (isActive) {
                // Purple active indicator line at the bottom of the tab
                graphics.fill(curX, startY + tabH - 2, curX + tabW, startY + tabH, 0xFF9333EA);
            }

            int textW = this.font.width(tab);
            graphics.text(this.font, tab, curX + (tabW - textW) / 2, startY + 6, textCol, false);
            curX += tabW;
        }
    }

    private void renderModulesCategories(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Category[] categories = Category.values();
        int colW = 120;
        int spacing = 10;
        int totalColsW = categories.length * colW + (categories.length - 1) * spacing;
        int startX = Math.max(12, (this.width - totalColsW) / 2);
        int startY = 46;

        int curX = startX;
        for (Category cat : categories) {
            boolean isCollapsed = categoryCollapsed.getOrDefault(cat, false);
            List<Module> modules = ModuleManager.getModulesByCategory(cat);

            int headerH = 22;
            int itemH = 20;
            int bodyH = isCollapsed ? 0 : Math.max(modules.size() * itemH, 36);
            int totalH = headerH + bodyH;

            // Panel outline & background
            RenderUtil.renderBox(graphics, curX, startY, colW, totalH, 0xFF14151F, 0xFF242636);

            // Header (Meteor Purple)
            graphics.fill(curX, startY, curX + colW, startY + headerH, 0xFF9333EA);
            // Header text
            graphics.text(this.font, cat.getDisplayName(), curX + 8, startY + 7, 0xFFFFFFFF, true);
            // Triangle collapse arrow
            String arrow = isCollapsed ? "▲" : "▼";
            graphics.text(this.font, arrow, curX + colW - 14, startY + 7, 0xFFFFFFFF, false);

            if (!isCollapsed) {
                int itemY = startY + headerH;
                if (modules.isEmpty()) {
                    // Empty placeholder
                    String emptyText = "(Empty)";
                    int eW = this.font.width(emptyText);
                    graphics.text(this.font, emptyText, curX + (colW - eW) / 2, itemY + 12, 0xFF585A6E, false);
                } else {
                    for (Module mod : modules) {
                        boolean enabled = mod.isEnabled();
                        boolean hover = mouseX >= curX && mouseX <= curX + colW && mouseY >= itemY && mouseY <= itemY + itemH;

                        int modBg;
                        int modText;
                        if (enabled) {
                            modBg = hover ? 0xFFA855F7 : 0xFF9333EA; // Vibrant purple when active
                            modText = 0xFFFFFFFF;
                        } else {
                            modBg = hover ? 0xFF202230 : 0xFF14151F; // Dark slate when inactive
                            modText = 0xFFC0C2D0;
                        }

                        graphics.fill(curX + 1, itemY, curX + colW - 1, itemY + itemH, modBg);
                        graphics.text(this.font, mod.getName(), curX + 8, itemY + 6, modText, enabled);

                        itemY += itemH;
                    }
                }
            }

            curX += colW + spacing;
        }
    }

    private void renderPlaceholderTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int winW = 380;
        int winH = 180;
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;

        RenderUtil.renderBox(graphics, winX, winY, winW, winH, 0xFF14151F, 0xFF9333EA);
        graphics.fill(winX, winY, winX + winW, winY + 24, 0xFF9333EA);
        graphics.text(this.font, currentTab + " (Placeholder)", winX + 12, winY + 7, 0xFFFFFFFF, true);

        String line1 = currentTab + " settings and options will be added here.";
        String line2 = "Switch back to 'Modules' for ESP.";
        int l1W = this.font.width(line1);
        int l2W = this.font.width(line2);
        graphics.text(this.font, line1, winX + (winW - l1W) / 2, winY + 65, 0xFFC0C2D0, false);
        graphics.text(this.font, line2, winX + (winW - l2W) / 2, winY + 85, 0xFF888B9E, false);

        int btnW = 120;
        int btnH = 22;
        int btnX = winX + (winW - btnW) / 2;
        int btnY = winY + winH - 36;
        boolean btnHov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int btnBg = btnHov ? 0xFFA855F7 : 0xFF9333EA;
        RenderUtil.renderBox(graphics, btnX, btnY, btnW, btnH, btnBg, 0xFFC084FC);
        String backText = "Back to Modules";
        int bW = this.font.width(backText);
        graphics.text(this.font, backText, btnX + (btnW - bW) / 2, btnY + 7, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        // 1. Top bar clicks
        int tabCount = TABS.length;
        int tabW = 68;
        int tabH = 20;
        int barW = tabCount * tabW;
        int startX = (this.width - barW) / 2;
        int startY = 12;

        if (mouseY >= startY && mouseY <= startY + tabH) {
            int curX = startX;
            for (String tab : TABS) {
                if (mouseX >= curX && mouseX <= curX + tabW) {
                    currentTab = tab;
                    return true;
                }
                curX += tabW;
            }
        }

        // 2. Placeholder tab button click
        if (!"Modules".equals(currentTab)) {
            int winW = 380;
            int winH = 180;
            int winX = (this.width - winW) / 2;
            int winY = (this.height - winH) / 2;
            int btnW = 120;
            int btnH = 22;
            int btnX = winX + (winW - btnW) / 2;
            int btnY = winY + winH - 36;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                currentTab = "Modules";
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        // 3. Category & Module clicks
        Category[] categories = Category.values();
        int colW = 120;
        int spacing = 10;
        int totalColsW = categories.length * colW + (categories.length - 1) * spacing;
        int catStartX = Math.max(12, (this.width - totalColsW) / 2);
        int catStartY = 46;

        int curX = catStartX;
        for (Category cat : categories) {
            boolean isCollapsed = categoryCollapsed.getOrDefault(cat, false);
            int headerH = 22;

            // Check click on Header (toggles collapsed)
            if (mouseX >= curX && mouseX <= curX + colW && mouseY >= catStartY && mouseY <= catStartY + headerH) {
                categoryCollapsed.put(cat, !isCollapsed);
                return true;
            }

            if (!isCollapsed) {
                List<Module> modules = ModuleManager.getModulesByCategory(cat);
                int itemH = 20;
                int itemY = catStartY + headerH;

                for (Module mod : modules) {
                    if (mouseX >= curX && mouseX <= curX + colW && mouseY >= itemY && mouseY <= itemY + itemH) {
                        if (button == 0) {
                            // Left-click: toggle module
                            mod.toggle();
                            ConfigService.save();
                            return true;
                        } else if (button == 1) {
                            // Right-click: Close this menu and open dedicated centered settings window!
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

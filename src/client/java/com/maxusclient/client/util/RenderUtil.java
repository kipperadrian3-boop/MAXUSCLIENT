package com.maxusclient.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class RenderUtil {

    public static void renderWindow(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, String title) {
        // Window body
        graphics.fill(x, y, x + w, y + h, 0xFF14151C);
        // Header
        graphics.fill(x, y, x + w, y + 20, 0xFF0E0F14);
        // Meteor purple accent top bar
        graphics.fill(x, y, x + w, y + 2, 0xFF7052FF);
        // Title
        graphics.text(font, title, x + 8, y + 6, 0xFFFFFFFF, false);
        // Outline border
        renderBorder(graphics, x, y, w, h, 0xFF272938);
    }

    public static void renderBox(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int bg, int border) {
        graphics.fill(x, y, x + w, y + h, bg);
        renderBorder(graphics, x, y, w, h, border);
    }

    public static void renderBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void renderCheckbox(GuiGraphicsExtractor graphics, Font font, int x, int y, boolean checked) {
        int bg = checked ? 0xFF6242EA : 0xFF101116;
        int border = checked ? 0xFF8566FF : 0xFF35384B;
        renderBox(graphics, x, y, 14, 14, bg, border);
        if (checked) {
            graphics.text(font, "X", x + 4, y + 3, 0xFFFFFFFF, false);
        }
    }

    public static void renderSearchBox(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String text, boolean focused, String placeholder) {
        int bg = focused ? 0xFF161822 : 0xFF0F1016;
        int border = focused ? 0xFF7052FF : 0xFF2A2C3C;
        renderBox(graphics, x, y, w, 16, bg, border);

        if (text.isEmpty()) {
            graphics.text(font, placeholder, x + 6, y + 4, 0xFF6C6E80, false);
        } else {
            String cursor = focused && ((System.currentTimeMillis() / 500) % 2 == 0) ? "|" : "";
            graphics.text(font, text + cursor, x + 6, y + 4, 0xFFFFFFFF, false);
        }
    }
}

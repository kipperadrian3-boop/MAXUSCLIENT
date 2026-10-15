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

    public static void renderStatusIcon(GuiGraphicsExtractor graphics, int x, int y, int size, boolean active) {
        if (active) {
            // Completely green square with white checkmark icon (No text!)
            graphics.fill(x, y, x + size, y + size, 0xFF22C55E);
            renderBorder(graphics, x, y, size, size, 0xFF16A34A);

            // Draw clean white checkmark
            int s = Math.max(8, size);
            int startX = x + (int) (s * 0.2f);
            int startY = y + (int) (s * 0.5f);
            int midX = x + (int) (s * 0.42f);
            int midY = y + (int) (s * 0.75f);
            int endX = x + (int) (s * 0.8f);
            int endY = y + (int) (s * 0.28f);

            // Short leg
            for (int i = 0; i <= (midX - startX); i++) {
                graphics.fill(startX + i, startY + i, startX + i + 1, startY + i + 1, 0xFFFFFFFF);
                graphics.fill(startX + i, startY + i + 1, startX + i + 1, startY + i + 2, 0xFFFFFFFF);
            }
            // Long leg
            for (int i = 0; i <= (endX - midX); i++) {
                int py = midY - (int) (i * ((float)(midY - endY) / (endX - midX)));
                graphics.fill(midX + i, py, midX + i + 1, py + 1, 0xFFFFFFFF);
                graphics.fill(midX + i, py + 1, midX + i + 1, py + 2, 0xFFFFFFFF);
            }
        } else {
            // Completely red square with white cross icon (No text!)
            graphics.fill(x, y, x + size, y + size, 0xFFEF4444);
            renderBorder(graphics, x, y, size, size, 0xFFDC2626);

            int inset = Math.max(2, (int) (size * 0.22f));
            for (int i = inset; i < size - inset; i++) {
                graphics.fill(x + i, y + i, x + i + 1, y + i + 1, 0xFFFFFFFF);
                graphics.fill(x + i, y + i + 1, x + i + 1, y + i + 2, 0xFFFFFFFF);
                graphics.fill(x + size - 1 - i, y + i, x + size - i, y + i + 1, 0xFFFFFFFF);
                graphics.fill(x + size - 1 - i, y + i + 1, x + size - i, y + i + 2, 0xFFFFFFFF);
            }
        }
    }

    public static void renderPlusIcon(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF22C55E);
        renderBorder(graphics, x, y, size, size, 0xFF16A34A);
        int mid = size / 2;
        int inset = Math.max(2, size / 5);
        graphics.fill(x + inset, y + mid, x + size - inset, y + mid + 1, 0xFFFFFFFF);
        graphics.fill(x + mid, y + inset, x + mid + 1, y + size - inset, 0xFFFFFFFF);
    }

    public static void renderMinusIcon(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFFEF4444);
        renderBorder(graphics, x, y, size, size, 0xFFDC2626);
        int mid = size / 2;
        int inset = Math.max(2, size / 5);
        graphics.fill(x + inset, y + mid, x + size - inset, y + mid + 1, 0xFFFFFFFF);
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

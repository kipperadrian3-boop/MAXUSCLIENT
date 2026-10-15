package com.maxusclient.client.util;

public class ColorUtil {
    public static final int PINK = 0xFFFF69B4;
    public static final int CYAN = 0xFF00FFFF;
    public static final int GREEN = 0xFF2ECC71;
    public static final int RED = 0xFFE74C3C;
    public static final int YELLOW = 0xFFF1C40F;
    public static final int ORANGE = 0xFFE67E22;
    public static final int PURPLE = 0xFF9B59B6;
    public static final int WHITE = 0xFFFFFFFF;

    public static final int[] PRESETS = {
        PINK, CYAN, GREEN, RED, YELLOW, ORANGE, PURPLE, WHITE
    };

    public static int toArgb(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int toRgb(int r, int g, int b) {
        return toArgb(r, g, b, 255);
    }

    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int getBlue(int color) {
        return color & 0xFF;
    }

    public static int getAlpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public static String toHex(int color) {
        return String.format("#%02X%02X%02X", getRed(color), getGreen(color), getBlue(color));
    }
}

package com.maxusclient.client.gui;

import com.maxusclient.client.model.BlockInfo;
import com.maxusclient.client.module.ModuleManager;
import com.maxusclient.client.module.esp.BlockEsp;
import com.maxusclient.client.service.BlockRegistryService;
import com.maxusclient.client.service.ConfigService;
import com.maxusclient.client.util.ColorUtil;
import com.maxusclient.client.util.RenderUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BlockEspSettingsScreen extends Screen {

    private final Screen parent;
    private final BlockEsp blockEsp;

    // Modals
    private boolean openListModal = false;
    private boolean openColorsModal = false;
    private boolean colorPickerActive = false;

    // Search fields
    private String allBlocksSearch = "";
    private String activeBlocksSearch = "";
    private String colorsModalSearch = "";
    private int focusedSearch = 0; // 0 = none, 1 = all blocks, 2 = active blocks, 3 = colors modal

    // Scroll offsets
    private int allBlocksScroll = 0;
    private int activeBlocksScroll = 0;
    private int colorsScroll = 0;

    // Color picker state
    private String colorPickerTarget = null; // null = globalColor, or specific blockId
    private int pickerR = 255;
    private int pickerG = 105;
    private int pickerB = 180;
    private int draggingSlider = -1; // 0 = R, 1 = G, 2 = B

    public BlockEspSettingsScreen(Screen parent) {
        super(Component.literal("Block ESP Settings"));
        this.parent = parent;
        this.blockEsp = ModuleManager.getModule(BlockEsp.class);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Dark translucent backdrop
        graphics.fill(0, 0, this.width, this.height, 0x90000000);

        if (blockEsp == null) {
            graphics.text(this.font, "Block ESP module not found!", this.width / 2 - 60, this.height / 2, 0xFFFF5555, true);
            return;
        }

        // 1. Draw Centered Settings Window
        renderCenteredSettingsWindow(graphics, mouseX, mouseY);

        // 2. Modals
        if (openListModal) {
            renderDualListModal(graphics, mouseX, mouseY);
        } else if (openColorsModal) {
            renderColorsModal(graphics, mouseX, mouseY);
        }

        if (colorPickerActive) {
            renderColorPickerModal(graphics, mouseX, mouseY);
        }
    }

    private void renderCenteredSettingsWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int winW = 360;
        int winH = 290;
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;

        // Window background & border
        RenderUtil.renderBox(graphics, winX, winY, winW, winH, 0xFF14151F, 0xFF2B2D40);

        // Title bar (Meteor Purple)
        int barH = 26;
        graphics.fill(winX, winY, winX + winW, winY + barH, 0xFF9333EA);
        graphics.text(this.font, "Block ESP - Settings", winX + 12, winY + 8, 0xFFFFFFFF, true);

        // Close button (X)
        int closeX = winX + winW - 22;
        int closeY = winY + 5;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 16 && mouseY >= closeY && mouseY <= closeY + 16;
        if (closeHover) {
            graphics.fill(closeX, closeY, closeX + 16, closeY + 16, 0x40000000);
        }
        graphics.text(this.font, "✕", closeX + 4, closeY + 3, closeHover ? 0xFFFF7777 : 0xFFFFFFFF, false);

        int curY = winY + 38;
        int rowH = 30;

        // 1. Setting: Enabled
        renderCheckboxRow(graphics, "Enabled", blockEsp.isEnabled(), winX + 16, curY, winW - 32);
        curY += rowH;

        // 2. Setting: Tracers
        renderCheckboxRow(graphics, "Tracers", blockEsp.tracers.get(), winX + 16, curY, winW - 32);
        curY += rowH;

        // 3. Setting: Use Color for all
        boolean useAll = blockEsp.useColorForAll.get();
        renderCheckboxRow(graphics, "Use Color for all", useAll, winX + 16, curY, winW - 32);

        // If Use Color for all is checked, show inline color box next to the checkbox
        if (useAll) {
            int colorBoxX = winX + winW - 48 - 24;
            int colorBoxY = curY + 2;
            int gColor = blockEsp.globalColor.get();
            graphics.fill(colorBoxX, colorBoxY, colorBoxX + 18, colorBoxY + 18, gColor);
            boolean colorHover = mouseX >= colorBoxX && mouseX <= colorBoxX + 18 && mouseY >= colorBoxY && mouseY <= colorBoxY + 18;
            RenderUtil.renderBorder(graphics, colorBoxX, colorBoxY, 18, 18, colorHover ? 0xFFFFFFFF : 0xFF888888);
        }
        curY += rowH + 4;

        // 4. Button: Color (Active Blocks)
        int btnW = winW - 32;
        int btnH = 24;
        int activeCount = blockEsp.activeBlocks.get().size();
        boolean colBtnHover = mouseX >= winX + 16 && mouseX <= winX + 16 + btnW && mouseY >= curY && mouseY <= curY + btnH;
        int colBtnBg = colBtnHover ? 0xFF242636 : 0xFF191A25;
        RenderUtil.renderBox(graphics, winX + 16, curY, btnW, btnH, colBtnBg, 0xFF35384D);
        String colBtnText = "Color (Active Blocks)  [" + activeCount + "]";
        int colTextW = this.font.width(colBtnText);
        graphics.text(this.font, colBtnText, winX + (winW - colTextW) / 2, curY + 7, 0xFFE0E0EC, false);
        curY += btnH + 8;

        // 5. Button: Open List
        boolean listBtnHover = mouseX >= winX + 16 && mouseX <= winX + 16 + btnW && mouseY >= curY && mouseY <= curY + btnH;
        int listBtnBg = listBtnHover ? 0xFFA855F7 : 0xFF9333EA;
        RenderUtil.renderBox(graphics, winX + 16, curY, btnW, btnH, listBtnBg, 0xFFC084FC);
        String listText = "Open List";
        int listTextW = this.font.width(listText);
        graphics.text(this.font, listText, winX + (winW - listTextW) / 2, curY + 7, 0xFFFFFFFF, true);
        curY += btnH + 16;

        // 6. Button: Back to Menu
        int backBtnH = 22;
        int backBtnY = winY + winH - 32;
        boolean backHover = mouseX >= winX + 16 && mouseX <= winX + 16 + btnW && mouseY >= backBtnY && mouseY <= backBtnY + backBtnH;
        int backBg = backHover ? 0xFF2D3043 : 0xFF1E202C;
        RenderUtil.renderBox(graphics, winX + 16, backBtnY, btnW, backBtnH, backBg, 0xFF3E425E);
        String backText = "← Back to Main Menu";
        int backTextW = this.font.width(backText);
        graphics.text(this.font, backText, winX + (winW - backTextW) / 2, backBtnY + 6, 0xFFB4B6C8, false);
    }

    private void renderCheckboxRow(GuiGraphicsExtractor graphics, String label, boolean checked, int x, int y, int w) {
        graphics.text(this.font, label, x, y + 6, 0xFFE0E0EC, false);

        int boxX = x + w - 20;
        int boxY = y + 2;
        int boxSize = 18;

        if (checked) {
            RenderUtil.renderBox(graphics, boxX, boxY, boxSize, boxSize, 0xFF9333EA, 0xFFA855F7);
            graphics.text(this.font, "✕", boxX + 4, boxY + 4, 0xFFFFFFFF, true);
        } else {
            RenderUtil.renderBox(graphics, boxX, boxY, boxSize, boxSize, 0xFF1A1B26, 0xFF383A50);
        }
    }

    private void renderDualListModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        int mW = 540;
        int mH = 340;
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        RenderUtil.renderBox(graphics, mX, mY, mW, mH, 0xFF14151F, 0xFF9333EA);
        graphics.fill(mX, mY, mX + mW, mY + 24, 0xFF9333EA);
        graphics.text(this.font, "Block ESP - Select Blocks", mX + 10, mY + 7, 0xFFFFFFFF, true);

        // Close button
        int cX = mX + mW - 20;
        int cY = mY + 4;
        graphics.text(this.font, "✕", cX + 2, cY + 3, 0xFFFFFFFF, false);

        int colW = 246;
        int listH = 250;
        int leftX = mX + 16;
        int leftY = mY + 32;
        int sY = leftY + 12;
        int lY = sY + 22;

        // LEFT: ALL BLOCKS
        List<BlockInfo> allBlocks = BlockRegistryService.getAllBlocks();
        graphics.text(this.font, "All Blocks (" + allBlocks.size() + ")", leftX, leftY, 0xFFB4B6C8, false);
        RenderUtil.renderSearchBox(graphics, this.font, leftX, sY, colW, allBlocksSearch, focusedSearch == 1, "Search all blocks...");
        RenderUtil.renderBox(graphics, leftX, lY, colW, listH, 0xFF101116, 0xFF252735);

        List<BlockInfo> filteredAll = BlockRegistryService.searchAllBlocks(allBlocksSearch);
        int itemH = 22;
        int totalLeftH = filteredAll.size() * itemH;
        int maxLeftScroll = Math.max(0, totalLeftH - listH);
        allBlocksScroll = Math.max(0, Math.min(allBlocksScroll, maxLeftScroll));

        int startIdx = allBlocksScroll / itemH;
        int endIdx = Math.min(filteredAll.size(), startIdx + (listH / itemH) + 2);
        int curY = lY - (allBlocksScroll % itemH);

        for (int i = startIdx; i < endIdx && i < filteredAll.size(); i++) {
            BlockInfo info = filteredAll.get(i);
            if (curY >= lY && curY + itemH <= lY + listH) {
                boolean rowHover = mouseX >= leftX && mouseX <= leftX + colW - 20 && mouseY >= curY && mouseY <= curY + itemH;
                if (rowHover) {
                    graphics.fill(leftX + 1, curY, leftX + colW - 1, curY + itemH, 0xFF1B1D28);
                }

                // Block Icon
                if (!info.getStack().isEmpty()) {
                    graphics.fakeItem(info.getStack(), leftX + 3, curY + 3);
                } else {
                    graphics.fill(leftX + 4, curY + 4, leftX + 18, curY + 18, 0xFF353748);
                }

                // Block Name
                String trimmedName = this.font.plainSubstrByWidth(info.name, colW - 54);
                graphics.text(this.font, trimmedName, leftX + 24, curY + 7, 0xFFE0E0EC, false);

                // Green "+" Button
                int plusX = leftX + colW - 20;
                int plusY = curY + 3;
                boolean plusHover = mouseX >= plusX && mouseX <= plusX + 16 && mouseY >= plusY && mouseY <= plusY + 16;
                int plusBg = plusHover ? 0xFF27AE60 : 0xFF1E8449;
                RenderUtil.renderBox(graphics, plusX, plusY, 16, 16, plusBg, 0xFF2ECC71);
                graphics.text(this.font, "+", plusX + 5, plusY + 4, 0xFFFFFFFF, false);
            }
            curY += itemH;
        }

        // RIGHT: ACTIVE BLOCKS
        int rightX = mX + 278;
        int activeCount = blockEsp.activeBlocks.get().size();
        graphics.text(this.font, "Active Blocks (" + activeCount + ")", rightX, leftY, 0xFFB4B6C8, false);
        RenderUtil.renderSearchBox(graphics, this.font, rightX, sY, colW, activeBlocksSearch, focusedSearch == 2, "Search active blocks...");
        RenderUtil.renderBox(graphics, rightX, lY, colW, listH, 0xFF101116, 0xFF252735);

        List<String> filteredActive = getFilteredActiveBlocks();
        int totalRightH = filteredActive.size() * itemH;
        int maxRightScroll = Math.max(0, totalRightH - listH);
        activeBlocksScroll = Math.max(0, Math.min(activeBlocksScroll, maxRightScroll));

        int rStartIdx = activeBlocksScroll / itemH;
        int rEndIdx = Math.min(filteredActive.size(), rStartIdx + (listH / itemH) + 2);
        int rCurY = lY - (activeBlocksScroll % itemH);

        for (int i = rStartIdx; i < rEndIdx && i < filteredActive.size(); i++) {
            String blockId = filteredActive.get(i);
            BlockInfo info = BlockRegistryService.findBlockInfo(blockId);
            if (rCurY >= lY && rCurY + itemH <= lY + listH) {
                boolean rowHover = mouseX >= rightX && mouseX <= rightX + colW - 20 && mouseY >= rCurY && mouseY <= rCurY + itemH;
                if (rowHover) {
                    graphics.fill(rightX + 1, rCurY, rightX + colW - 1, rCurY + itemH, 0xFF1B1D28);
                }

                // Block Icon
                if (info != null && !info.getStack().isEmpty()) {
                    graphics.fakeItem(info.getStack(), rightX + 3, rCurY + 3);
                } else {
                    graphics.fill(rightX + 4, rCurY + 4, rightX + 18, rCurY + 18, 0xFF353748);
                }

                // Block Name
                String name = info != null ? info.name : blockId;
                String trimmedName = this.font.plainSubstrByWidth(name, colW - 54);
                graphics.text(this.font, trimmedName, rightX + 24, rCurY + 7, 0xFFE0E0EC, false);

                // Red "-" Button
                int minusX = rightX + colW - 20;
                int minusY = rCurY + 3;
                boolean minusHover = mouseX >= minusX && mouseX <= minusX + 16 && mouseY >= minusY && mouseY <= minusY + 16;
                int minusBg = minusHover ? 0xFFC0392B : 0xFF922B21;
                RenderUtil.renderBox(graphics, minusX, minusY, 16, 16, minusBg, 0xFFE74C3C);
                graphics.text(this.font, "-", minusX + 6, minusY + 4, 0xFFFFFFFF, false);
            }
            rCurY += itemH;
        }
    }

    private void renderColorsModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        int mW = 380;
        int mH = 320;
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        RenderUtil.renderBox(graphics, mX, mY, mW, mH, 0xFF14151F, 0xFF9333EA);
        graphics.fill(mX, mY, mX + mW, mY + 24, 0xFF9333EA);
        graphics.text(this.font, "Active Blocks - Custom Colors", mX + 10, mY + 7, 0xFFFFFFFF, true);

        // Close button
        int cX = mX + mW - 20;
        int cY = mY + 4;
        graphics.text(this.font, "✕", cX + 2, cY + 3, 0xFFFFFFFF, false);

        int sY = mY + 32;
        int lW = mW - 32;
        int lX = mX + 16;
        RenderUtil.renderSearchBox(graphics, this.font, lX, sY, lW, colorsModalSearch, focusedSearch == 3, "Filter active blocks...");

        int lY = sY + 22;
        int lH = mH - 64;
        RenderUtil.renderBox(graphics, lX, lY, lW, lH, 0xFF101116, 0xFF252735);

        List<String> activeList = getFilteredColorBlocks();
        int itemH = 26;
        int totalH = activeList.size() * itemH;
        int maxScroll = Math.max(0, totalH - lH);
        colorsScroll = Math.max(0, Math.min(colorsScroll, maxScroll));

        int startIdx = colorsScroll / itemH;
        int endIdx = Math.min(activeList.size(), startIdx + (lH / itemH) + 2);
        int curY = lY - (colorsScroll % itemH);

        for (int i = startIdx; i < endIdx && i < activeList.size(); i++) {
            String blockId = activeList.get(i);
            BlockInfo info = BlockRegistryService.findBlockInfo(blockId);
            if (curY >= lY && curY + itemH <= lY + lH) {
                boolean rowHover = mouseX >= lX && mouseX <= lX + lW - 24 && mouseY >= curY && mouseY <= curY + itemH;
                if (rowHover) {
                    graphics.fill(lX + 1, curY, lX + lW - 1, curY + itemH, 0xFF1B1D28);
                }

                if (info != null && !info.getStack().isEmpty()) {
                    graphics.fakeItem(info.getStack(), lX + 4, curY + 4);
                } else {
                    graphics.fill(lX + 4, curY + 4, lX + 20, curY + 20, 0xFF353748);
                }

                String name = info != null ? info.name : blockId;
                String trimmed = this.font.plainSubstrByWidth(name, lW - 56);
                graphics.text(this.font, trimmed, lX + 26, curY + 8, 0xFFE0E0EC, false);

                int colBoxX = lX + lW - 22;
                int colBoxY = curY + 4;
                int blockColor = blockEsp.activeBlocks.getColor(blockId);

                graphics.fill(colBoxX, colBoxY, colBoxX + 16, colBoxY + 16, blockColor);
                RenderUtil.renderBorder(graphics, colBoxX, colBoxY, 16, 16, 0xFFFFFFFF);
            }
            curY += itemH;
        }

        if (activeList.isEmpty()) {
            String noMsg = "No active blocks added yet!";
            int noMsgW = this.font.width(noMsg);
            graphics.text(this.font, noMsg, mX + (mW - noMsgW) / 2, lY + lH / 2 - 4, 0xFF707280, false);
        }
    }

    private void renderColorPickerModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        int pW = 260;
        int pH = 230;
        int pX = (this.width - pW) / 2;
        int pY = (this.height - pH) / 2;

        RenderUtil.renderBox(graphics, pX, pY, pW, pH, 0xFF14151F, 0xFF9333EA);
        graphics.fill(pX, pY, pX + pW, pY + 24, 0xFF9333EA);

        String title = colorPickerTarget == null ? "Global Color Picker" : "Custom Block Color";
        graphics.text(this.font, title, pX + 10, pY + 7, 0xFFFFFFFF, true);

        int cX = pX + pW - 20;
        int cY = pY + 4;
        graphics.text(this.font, "✕", cX + 2, cY + 3, 0xFFFFFFFF, false);

        int currentColor = ColorUtil.toRgb(pickerR, pickerG, pickerB);

        // Preview Swatch
        int prevX = pX + 16;
        int prevY = pY + 32;
        graphics.fill(prevX, prevY, prevX + 40, prevY + 28, currentColor);
        RenderUtil.renderBorder(graphics, prevX, prevY, 40, 28, 0xFFFFFFFF);

        String hexText = ColorUtil.toHex(currentColor);
        graphics.text(this.font, hexText, prevX + 48, prevY + 10, 0xFFE0E0EC, false);

        // RGB Sliders
        int sliderX = pX + 16;
        int sliderW = pW - 32;
        int sY = pY + 68;

        renderColorSlider(graphics, "Red", pickerR, sliderX, sY, sliderW, 0xFFFF4444);
        renderColorSlider(graphics, "Green", pickerG, sliderX, sY + 24, sliderW, 0xFF44FF44);
        renderColorSlider(graphics, "Blue", pickerB, sliderX, sY + 48, sliderW, 0xFF4488FF);

        // Palette presets
        int palY = sY + 76;
        int palX = pX + 16;
        for (int col : ColorUtil.PRESETS) {
            graphics.fill(palX, palY, palX + 18, palY + 14, col);
            boolean hov = mouseX >= palX && mouseX <= palX + 18 && mouseY >= palY && mouseY <= palY + 14;
            RenderUtil.renderBorder(graphics, palX, palY, 18, 14, hov ? 0xFFFFFFFF : 0xFF444444);
            palX += 26;
        }

        // Apply/Done Button
        int btnY = pY + pH - 28;
        int btnW = pW - 32;
        boolean dHover = mouseX >= pX + 16 && mouseX <= pX + 16 + btnW && mouseY >= btnY && mouseY <= btnY + 20;
        int dColor = dHover ? 0xFFA855F7 : 0xFF9333EA;
        RenderUtil.renderBox(graphics, pX + 16, btnY, btnW, 20, dColor, 0xFFC084FC);
        String applyText = "Apply & Save";
        int applyW = this.font.width(applyText);
        graphics.text(this.font, applyText, pX + (pW - applyW) / 2, btnY + 6, 0xFFFFFFFF, true);
    }

    private void renderColorSlider(GuiGraphicsExtractor graphics, String label, int value, int x, int y, int w, int barColor) {
        graphics.text(this.font, label + ": " + value, x, y, 0xFFB4B6C8, false);
        int trackY = y + 10;
        int trackH = 8;
        graphics.fill(x, trackY, x + w, trackY + trackH, 0xFF222330);
        int fillW = (int) ((value / 255.0f) * w);
        graphics.fill(x, trackY, x + fillW, trackY + trackH, barColor);
        RenderUtil.renderBorder(graphics, x, trackY, w, trackH, 0xFF35384D);

        int knobX = x + fillW - 2;
        graphics.fill(knobX, trackY - 2, knobX + 4, trackY + trackH + 2, 0xFFFFFFFF);
    }

    private List<String> getFilteredActiveBlocks() {
        if (blockEsp == null) return List.of();
        List<String> list = new ArrayList<>(blockEsp.activeBlocks.get().keySet());
        if (activeBlocksSearch == null || activeBlocksSearch.trim().isEmpty()) {
            return list;
        }
        String q = activeBlocksSearch.toLowerCase().trim();
        List<String> filtered = new ArrayList<>();
        for (String id : list) {
            BlockInfo info = BlockRegistryService.findBlockInfo(id);
            String name = info != null ? info.name : id;
            if (name.toLowerCase().contains(q) || id.toLowerCase().contains(q)) {
                filtered.add(id);
            }
        }
        return filtered;
    }

    private List<String> getFilteredColorBlocks() {
        if (blockEsp == null) return List.of();
        List<String> list = new ArrayList<>(blockEsp.activeBlocks.get().keySet());
        if (colorsModalSearch == null || colorsModalSearch.trim().isEmpty()) {
            return list;
        }
        String q = colorsModalSearch.toLowerCase().trim();
        List<String> filtered = new ArrayList<>();
        for (String id : list) {
            BlockInfo info = BlockRegistryService.findBlockInfo(id);
            String name = info != null ? info.name : id;
            if (name.toLowerCase().contains(q) || id.toLowerCase().contains(q)) {
                filtered.add(id);
            }
        }
        return filtered;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (colorPickerActive) {
            handleColorPickerClick(mouseX, mouseY, button);
            return true;
        }

        if (openListModal) {
            handleDualListClick(mouseX, mouseY, button);
            return true;
        }

        if (openColorsModal) {
            handleColorsModalClick(mouseX, mouseY, button);
            return true;
        }

        // Handle Main Centered Settings Window Click
        int winW = 360;
        int winH = 290;
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;

        // Close button (X)
        int closeX = winX + winW - 22;
        int closeY = winY + 5;
        if (mouseX >= closeX && mouseX <= closeX + 16 && mouseY >= closeY && mouseY <= closeY + 16) {
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }

        // Back button
        int btnW = winW - 32;
        int backBtnH = 22;
        int backBtnY = winY + winH - 32;
        if (mouseX >= winX + 16 && mouseX <= winX + 16 + btnW && mouseY >= backBtnY && mouseY <= backBtnY + backBtnH) {
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }

        int curY = winY + 38;
        int rowH = 30;

        // Row 1: Enabled
        if (mouseX >= winX + 16 && mouseX <= winX + winW - 16 && mouseY >= curY && mouseY <= curY + 24) {
            blockEsp.toggle();
            ConfigService.save();
            return true;
        }
        curY += rowH;

        // Row 2: Tracers
        if (mouseX >= winX + 16 && mouseX <= winX + winW - 16 && mouseY >= curY && mouseY <= curY + 24) {
            blockEsp.tracers.toggle();
            ConfigService.save();
            return true;
        }
        curY += rowH;

        // Row 3: Use Color for all
        boolean useAll = blockEsp.useColorForAll.get();
        if (useAll) {
            int colorBoxX = winX + winW - 48 - 24;
            int colorBoxY = curY + 2;
            if (mouseX >= colorBoxX && mouseX <= colorBoxX + 18 && mouseY >= colorBoxY && mouseY <= colorBoxY + 18) {
                openColorPickerFor(null);
                return true;
            }
        }

        if (mouseX >= winX + 16 && mouseX <= winX + winW - 16 && mouseY >= curY && mouseY <= curY + 24) {
            blockEsp.useColorForAll.toggle();
            ConfigService.save();
            return true;
        }
        curY += rowH + 4;

        // Row 4: Color (Active Blocks) button
        int btnH = 24;
        if (mouseX >= winX + 16 && mouseX <= winX + 16 + btnW && mouseY >= curY && mouseY <= curY + btnH) {
            openColorsModal = true;
            colorsModalSearch = "";
            colorsScroll = 0;
            return true;
        }
        curY += btnH + 8;

        // Row 5: Open List button
        if (mouseX >= winX + 16 && mouseX <= winX + 16 + btnW && mouseY >= curY && mouseY <= curY + btnH) {
            openListModal = true;
            allBlocksSearch = "";
            activeBlocksSearch = "";
            allBlocksScroll = 0;
            activeBlocksScroll = 0;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void handleDualListClick(double mouseX, double mouseY, int button) {
        int mW = 540;
        int mH = 340;
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        // Close button
        int cX = mX + mW - 20;
        int cY = mY + 4;
        if (mouseX >= cX && mouseX <= cX + 16 && mouseY >= cY && mouseY <= cY + 16) {
            openListModal = false;
            return;
        }

        int colW = 246;
        int listH = 250;
        int leftX = mX + 16;
        int leftY = mY + 32;
        int sY = leftY + 12;
        int lY = sY + 22;

        // Left search bar
        if (mouseX >= leftX && mouseX <= leftX + colW && mouseY >= sY && mouseY <= sY + 18) {
            focusedSearch = 1;
            return;
        }

        // Right search bar
        int rightX = mX + 278;
        if (mouseX >= rightX && mouseX <= rightX + colW && mouseY >= sY && mouseY <= sY + 18) {
            focusedSearch = 2;
            return;
        }

        int itemH = 22;

        // Left List: Add to Active
        if (mouseX >= leftX && mouseX <= leftX + colW && mouseY >= lY && mouseY <= lY + listH) {
            List<BlockInfo> filteredAll = BlockRegistryService.searchAllBlocks(allBlocksSearch);
            int startIdx = allBlocksScroll / itemH;
            int endIdx = Math.min(filteredAll.size(), startIdx + (listH / itemH) + 2);
            int curY = lY - (allBlocksScroll % itemH);

            for (int i = startIdx; i < endIdx && i < filteredAll.size(); i++) {
                BlockInfo info = filteredAll.get(i);
                if (curY >= lY && curY + itemH <= lY + listH) {
                    int plusX = leftX + colW - 20;
                    int plusY = curY + 3;
                    if (mouseX >= plusX && mouseX <= plusX + 16 && mouseY >= plusY && mouseY <= plusY + 16) {
                        blockEsp.activeBlocks.addBlock(info.id.toString(), 0xFFFF69B4);
                        ConfigService.save();
                        return;
                    }
                }
                curY += itemH;
            }
        }

        // Right List: Remove from Active
        if (mouseX >= rightX && mouseX <= rightX + colW && mouseY >= lY && mouseY <= lY + listH) {
            List<String> filteredActive = getFilteredActiveBlocks();
            int rStartIdx = activeBlocksScroll / itemH;
            int rEndIdx = Math.min(filteredActive.size(), rStartIdx + (listH / itemH) + 2);
            int rCurY = lY - (activeBlocksScroll % itemH);

            for (int i = rStartIdx; i < rEndIdx && i < filteredActive.size(); i++) {
                String blockId = filteredActive.get(i);
                if (rCurY >= lY && rCurY + itemH <= lY + listH) {
                    int minusX = rightX + colW - 20;
                    int minusY = rCurY + 3;
                    if (mouseX >= minusX && mouseX <= minusX + 16 && mouseY >= minusY && mouseY <= minusY + 16) {
                        blockEsp.activeBlocks.removeBlock(blockId);
                        ConfigService.save();
                        return;
                    }
                }
                rCurY += itemH;
            }
        }
    }

    private void handleColorsModalClick(double mouseX, double mouseY, int button) {
        int mW = 380;
        int mH = 320;
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        int cX = mX + mW - 20;
        int cY = mY + 4;
        if (mouseX >= cX && mouseX <= cX + 16 && mouseY >= cY && mouseY <= cY + 16) {
            openColorsModal = false;
            return;
        }

        int sY = mY + 32;
        int lW = mW - 32;
        int lX = mX + 16;
        if (mouseX >= lX && mouseX <= lX + lW && mouseY >= sY && mouseY <= sY + 18) {
            focusedSearch = 3;
            return;
        }

        int lY = sY + 22;
        int lH = mH - 64;
        int itemH = 26;

        if (mouseX >= lX && mouseX <= lX + lW && mouseY >= lY && mouseY <= lY + lH) {
            List<String> activeList = getFilteredColorBlocks();
            int startIdx = colorsScroll / itemH;
            int endIdx = Math.min(activeList.size(), startIdx + (lH / itemH) + 2);
            int curY = lY - (colorsScroll % itemH);

            for (int i = startIdx; i < endIdx && i < activeList.size(); i++) {
                String blockId = activeList.get(i);
                if (curY >= lY && curY + itemH <= lY + lH) {
                    int colBoxX = lX + lW - 22;
                    int colBoxY = curY + 4;
                    if (mouseX >= colBoxX && mouseX <= colBoxX + 16 && mouseY >= colBoxY && mouseY <= colBoxY + 16) {
                        openColorPickerFor(blockId);
                        return;
                    }
                }
                curY += itemH;
            }
        }
    }

    private void handleColorPickerClick(double mouseX, double mouseY, int button) {
        int pW = 260;
        int pH = 230;
        int pX = (this.width - pW) / 2;
        int pY = (this.height - pH) / 2;

        int cX = pX + pW - 20;
        int cY = pY + 4;
        if (mouseX >= cX && mouseX <= cX + 16 && mouseY >= cY && mouseY <= cY + 16) {
            colorPickerActive = false;
            return;
        }

        int sliderX = pX + 16;
        int sliderW = pW - 32;
        int sY = pY + 68;

        if (mouseX >= sliderX && mouseX <= sliderX + sliderW) {
            if (mouseY >= sY + 8 && mouseY <= sY + 22) {
                draggingSlider = 0;
                updateSliderFromMouse(mouseX, sliderX, sliderW, 0);
                return;
            }
            if (mouseY >= sY + 32 && mouseY <= sY + 46) {
                draggingSlider = 1;
                updateSliderFromMouse(mouseX, sliderX, sliderW, 1);
                return;
            }
            if (mouseY >= sY + 56 && mouseY <= sY + 70) {
                draggingSlider = 2;
                updateSliderFromMouse(mouseX, sliderX, sliderW, 2);
                return;
            }
        }

        // Palettes
        int palY = sY + 76;
        int palX = pX + 16;
        for (int col : ColorUtil.PRESETS) {
            if (mouseX >= palX && mouseX <= palX + 18 && mouseY >= palY && mouseY <= palY + 14) {
                pickerR = ColorUtil.getRed(col);
                pickerG = ColorUtil.getGreen(col);
                pickerB = ColorUtil.getBlue(col);
                applyCurrentPickerColor();
                return;
            }
            palX += 26;
        }

        // Apply Button
        int btnY = pY + pH - 28;
        int btnW = pW - 32;
        if (mouseX >= pX + 16 && mouseX <= pX + 16 + btnW && mouseY >= btnY && mouseY <= btnY + 20) {
            applyCurrentPickerColor();
            colorPickerActive = false;
        }
    }

    private void updateSliderFromMouse(double mouseX, int x, int w, int channel) {
        float rel = (float) (mouseX - x) / (float) w;
        int val = Math.max(0, Math.min(255, (int) (rel * 255.0f)));
        if (channel == 0) pickerR = val;
        else if (channel == 1) pickerG = val;
        else if (channel == 2) pickerB = val;
        applyCurrentPickerColor();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (colorPickerActive && draggingSlider >= 0) {
            int pW = 260;
            int pX = (this.width - pW) / 2;
            int sliderX = pX + 16;
            int sliderW = pW - 32;
            updateSliderFromMouse(event.x(), sliderX, sliderW, draggingSlider);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingSlider = -1;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scrollAmt = (int) (-verticalAmount * 24);
        if (openListModal) {
            int mW = 540;
            int mX = (this.width - mW) / 2;
            int colW = 246;
            if (mouseX >= mX + 16 && mouseX <= mX + 16 + colW) {
                allBlocksScroll += scrollAmt;
                return true;
            } else if (mouseX >= mX + 278 && mouseX <= mX + 278 + colW) {
                activeBlocksScroll += scrollAmt;
                return true;
            }
        } else if (openColorsModal) {
            colorsScroll += scrollAmt;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (colorPickerActive) {
                colorPickerActive = false;
                return true;
            }
            if (openListModal) {
                openListModal = false;
                return true;
            }
            if (openColorsModal) {
                openColorsModal = false;
                return true;
            }
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }

        if (focusedSearch > 0) {
            if (keyCode == InputConstants.KEY_BACKSPACE) {
                if (focusedSearch == 1 && !allBlocksSearch.isEmpty()) {
                    allBlocksSearch = allBlocksSearch.substring(0, allBlocksSearch.length() - 1);
                } else if (focusedSearch == 2 && !activeBlocksSearch.isEmpty()) {
                    activeBlocksSearch = activeBlocksSearch.substring(0, activeBlocksSearch.length() - 1);
                } else if (focusedSearch == 3 && !colorsModalSearch.isEmpty()) {
                    colorsModalSearch = colorsModalSearch.substring(0, colorsModalSearch.length() - 1);
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_RETURN) {
                focusedSearch = 0;
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char ch = (char) event.codepoint();
        if (Character.isLetterOrDigit(ch) || ch == ' ' || ch == '_' || ch == ':') {
            if (focusedSearch == 1) {
                allBlocksSearch += ch;
                return true;
            } else if (focusedSearch == 2) {
                activeBlocksSearch += ch;
                return true;
            } else if (focusedSearch == 3) {
                colorsModalSearch += ch;
                return true;
            }
        }
        return super.charTyped(event);
    }

    private void openColorPickerFor(String blockId) {
        this.colorPickerTarget = blockId;
        int color = (blockId == null) ? blockEsp.globalColor.get() : blockEsp.activeBlocks.getColor(blockId);
        this.pickerR = ColorUtil.getRed(color);
        this.pickerG = ColorUtil.getGreen(color);
        this.pickerB = ColorUtil.getBlue(color);
        this.colorPickerActive = true;
    }

    private void applyCurrentPickerColor() {
        int color = ColorUtil.toRgb(pickerR, pickerG, pickerB);
        if (colorPickerTarget == null) {
            blockEsp.globalColor.set(color);
        } else {
            blockEsp.activeBlocks.setColor(colorPickerTarget, color);
        }
        ConfigService.save();
    }
}

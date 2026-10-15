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
        // NOTE: No background dimming at all - Minecraft stays 100% bright and clear!

        if (blockEsp == null) {
            graphics.text(this.font, "Block ESP module not found!", this.width / 2 - 50, this.height / 2, 0xFFFF5555, true);
            return;
        }

        // 1. Draw Centered Settings Window (25% smaller)
        renderCenteredSettingsWindow(graphics, mouseX, mouseY);

        // 2. Modals (rendered on top if active)
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
        // 25% smaller: width 195px, height 154px
        int winW = Math.min(195, this.width - 12);
        int winH = Math.min(154, this.height - 12);
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;

        // Window background & border
        RenderUtil.renderBox(graphics, winX, winY, winW, winH, 0xFF14151F, 0xFF2B2D40);

        // Title bar (Meteor Purple)
        int barH = 15;
        graphics.fill(winX, winY, winX + winW, winY + barH, 0xFF9333EA);
        graphics.text(this.font, "Block ESP - Settings", winX + 6, winY + 4, 0xFFFFFFFF, true);

        // Close button (X)
        int closeX = winX + winW - 14;
        int closeY = winY + 2;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 11 && mouseY >= closeY && mouseY <= closeY + 11;
        if (closeHover) {
            graphics.fill(closeX, closeY, closeX + 11, closeY + 11, 0x40000000);
        }
        graphics.text(this.font, "✕", closeX + 2, closeY + 1, closeHover ? 0xFFFF7777 : 0xFFFFFFFF, false);

        int curY = winY + 21;
        int rowH = 17;

        // 1. Setting: Enabled (Clickable to toggle; Green check when ON, Red cross when OFF)
        renderSettingRow(graphics, "Enabled", blockEsp.isEnabled(), winX + 8, curY, winW - 16);
        curY += rowH;

        // 2. Setting: Tracers
        renderSettingRow(graphics, "Tracers", blockEsp.tracers.get(), winX + 8, curY, winW - 16);
        curY += rowH;

        // 3. Setting: Use Color for all
        boolean useAll = blockEsp.useColorForAll.get();
        renderSettingRow(graphics, "Use Color for all", useAll, winX + 8, curY, winW - 16);

        // Inline color box if Use Color for all is checked
        if (useAll) {
            int colorBoxX = winX + winW - 35;
            int colorBoxY = curY + 2;
            int gColor = blockEsp.globalColor.get();
            graphics.fill(colorBoxX, colorBoxY, colorBoxX + 11, colorBoxY + 11, gColor);
            boolean colorHover = mouseX >= colorBoxX && mouseX <= colorBoxX + 11 && mouseY >= colorBoxY && mouseY <= colorBoxY + 11;
            RenderUtil.renderBorder(graphics, colorBoxX, colorBoxY, 11, 11, colorHover ? 0xFFFFFFFF : 0xFF888888);
        }
        curY += rowH + 2;

        // 4. Button: Color (Active Blocks)
        int btnW = winW - 16;
        int btnH = 15;
        int activeCount = blockEsp.activeBlocks.get().size();
        boolean colBtnHover = mouseX >= winX + 8 && mouseX <= winX + 8 + btnW && mouseY >= curY && mouseY <= curY + btnH;
        int colBtnBg = colBtnHover ? 0xFF242636 : 0xFF191A25;
        RenderUtil.renderBox(graphics, winX + 8, curY, btnW, btnH, colBtnBg, 0xFF35384D);
        String colBtnText = "Color (Active) [" + activeCount + "]";
        int colTextW = this.font.width(colBtnText);
        graphics.text(this.font, colBtnText, winX + (winW - colTextW) / 2, curY + 4, 0xFFE0E0EC, false);
        curY += btnH + 4;

        // 5. Button: Open List
        boolean listBtnHover = mouseX >= winX + 8 && mouseX <= winX + 8 + btnW && mouseY >= curY && mouseY <= curY + btnH;
        int listBtnBg = listBtnHover ? 0xFFA855F7 : 0xFF9333EA;
        RenderUtil.renderBox(graphics, winX + 8, curY, btnW, btnH, listBtnBg, 0xFFC084FC);
        String listText = "Open List";
        int listTextW = this.font.width(listText);
        graphics.text(this.font, listText, winX + (winW - listTextW) / 2, curY + 4, 0xFFFFFFFF, true);
        curY += btnH + 5;

        // 6. Button: Back to Menu
        int backBtnH = 14;
        int backBtnY = winY + winH - 18;
        boolean backHover = mouseX >= winX + 8 && mouseX <= winX + 8 + btnW && mouseY >= backBtnY && mouseY <= backBtnY + backBtnH;
        int backBg = backHover ? 0xFF2D3043 : 0xFF1E202C;
        RenderUtil.renderBox(graphics, winX + 8, backBtnY, btnW, backBtnH, backBg, 0xFF3E425E);
        String backText = "← Back";
        int backTextW = this.font.width(backText);
        graphics.text(this.font, backText, winX + (winW - backTextW) / 2, backBtnY + 3, 0xFFB4B6C8, false);
    }

    private void renderSettingRow(GuiGraphicsExtractor graphics, String label, boolean checked, int x, int y, int w) {
        String trimmed = this.font.plainSubstrByWidth(label, w - 24);
        graphics.text(this.font, trimmed, x, y + 3, 0xFFE0E0EC, false);

        int iconX = x + w - 13;
        int iconY = y + 2;
        int iconSize = 11;

        // Render Green square with white checkmark (ON) or Red square with white cross (OFF) - NO text!
        RenderUtil.renderStatusIcon(graphics, iconX, iconY, iconSize, checked);
    }

    private void renderDualListModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 25% smaller: width 285px, height 172px
        int mW = Math.min(285, this.width - 12);
        int mH = Math.min(172, this.height - 12);
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        RenderUtil.renderBox(graphics, mX, mY, mW, mH, 0xFF14151F, 0xFF9333EA);
        graphics.fill(mX, mY, mX + mW, mY + 15, 0xFF9333EA);
        graphics.text(this.font, "Block ESP - Select Blocks", mX + 6, mY + 4, 0xFFFFFFFF, true);

        // Close button
        int cX = mX + mW - 14;
        int cY = mY + 2;
        graphics.text(this.font, "✕", cX + 2, cY + 1, 0xFFFFFFFF, false);

        int colW = (mW - 20) / 2;
        int listH = mH - 58;
        int leftX = mX + 6;
        int leftY = mY + 18;
        int sY = leftY + 9;
        int lY = sY + 15;

        // LEFT: ALL BLOCKS
        List<BlockInfo> allBlocks = BlockRegistryService.getAllBlocks();
        graphics.text(this.font, "All (" + allBlocks.size() + ")", leftX, leftY, 0xFFB4B6C8, false);
        RenderUtil.renderSearchBox(graphics, this.font, leftX, sY, colW, allBlocksSearch, focusedSearch == 1, "Search all...");
        RenderUtil.renderBox(graphics, leftX, lY, colW, listH, 0xFF101116, 0xFF252735);

        List<BlockInfo> filteredAll = BlockRegistryService.searchAllBlocks(allBlocksSearch);
        int itemH = 14;
        int totalLeftH = filteredAll.size() * itemH;
        int maxLeftScroll = Math.max(0, totalLeftH - listH);
        allBlocksScroll = Math.max(0, Math.min(allBlocksScroll, maxLeftScroll));

        int startIdx = allBlocksScroll / itemH;
        int endIdx = Math.min(filteredAll.size(), startIdx + (listH / itemH) + 2);
        int curY = lY - (allBlocksScroll % itemH);

        for (int i = startIdx; i < endIdx && i < filteredAll.size(); i++) {
            BlockInfo info = filteredAll.get(i);
            if (curY >= lY && curY + itemH <= lY + listH) {
                boolean rowHover = mouseX >= leftX && mouseX <= leftX + colW - 13 && mouseY >= curY && mouseY <= curY + itemH;
                if (rowHover) {
                    graphics.fill(leftX + 1, curY, leftX + colW - 1, curY + itemH, 0xFF1B1D28);
                }

                // Block Icon
                if (!info.getStack().isEmpty()) {
                    graphics.fakeItem(info.getStack(), leftX + 1, curY);
                } else {
                    graphics.fill(leftX + 2, curY + 1, leftX + 12, curY + 11, 0xFF353748);
                }

                // Block Name
                String trimmedName = this.font.plainSubstrByWidth(info.name, colW - 28);
                graphics.text(this.font, trimmedName, leftX + 15, curY + 3, 0xFFE0E0EC, false);

                // Green "+" Icon button (No text!)
                int plusX = leftX + colW - 13;
                int plusY = curY + 2;
                RenderUtil.renderPlusIcon(graphics, plusX, plusY, 10);
            }
            curY += itemH;
        }

        // RIGHT: ACTIVE BLOCKS
        int rightX = mX + 14 + colW;
        int activeCount = blockEsp.activeBlocks.get().size();
        graphics.text(this.font, "Active (" + activeCount + ")", rightX, leftY, 0xFFB4B6C8, false);
        RenderUtil.renderSearchBox(graphics, this.font, rightX, sY, colW, activeBlocksSearch, focusedSearch == 2, "Search active...");
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
                boolean rowHover = mouseX >= rightX && mouseX <= rightX + colW - 13 && mouseY >= rCurY && mouseY <= rCurY + itemH;
                if (rowHover) {
                    graphics.fill(rightX + 1, rCurY, rightX + colW - 1, rCurY + itemH, 0xFF1B1D28);
                }

                // Block Icon
                if (info != null && !info.getStack().isEmpty()) {
                    graphics.fakeItem(info.getStack(), rightX + 1, rCurY);
                } else {
                    graphics.fill(rightX + 2, rCurY + 1, rightX + 12, rCurY + 11, 0xFF353748);
                }

                // Block Name
                String name = info != null ? info.name : blockId;
                String trimmedName = this.font.plainSubstrByWidth(name, colW - 28);
                graphics.text(this.font, trimmedName, rightX + 15, rCurY + 3, 0xFFE0E0EC, false);

                // Red "-" Icon button (No text!)
                int minusX = rightX + colW - 13;
                int minusY = rCurY + 2;
                RenderUtil.renderMinusIcon(graphics, minusX, minusY, 10);
            }
            rCurY += itemH;
        }
    }

    private void renderColorsModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 25% smaller: width 195px, height 158px
        int mW = Math.min(195, this.width - 12);
        int mH = Math.min(158, this.height - 12);
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        RenderUtil.renderBox(graphics, mX, mY, mW, mH, 0xFF14151F, 0xFF9333EA);
        graphics.fill(mX, mY, mX + mW, mY + 15, 0xFF9333EA);
        graphics.text(this.font, "Active Blocks Colors", mX + 6, mY + 4, 0xFFFFFFFF, true);

        // Close button
        int cX = mX + mW - 14;
        int cY = mY + 2;
        graphics.text(this.font, "✕", cX + 2, cY + 1, 0xFFFFFFFF, false);

        int sY = mY + 20;
        int lW = mW - 16;
        int lX = mX + 8;
        RenderUtil.renderSearchBox(graphics, this.font, lX, sY, lW, colorsModalSearch, focusedSearch == 3, "Filter active...");

        int lY = sY + 15;
        int lH = mH - 44;
        RenderUtil.renderBox(graphics, lX, lY, lW, lH, 0xFF101116, 0xFF252735);

        List<String> activeList = getFilteredColorBlocks();
        int itemH = 15;
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
                boolean rowHover = mouseX >= lX && mouseX <= lX + lW - 16 && mouseY >= curY && mouseY <= curY + itemH;
                if (rowHover) {
                    graphics.fill(lX + 1, curY, lX + lW - 1, curY + itemH, 0xFF1B1D28);
                }

                if (info != null && !info.getStack().isEmpty()) {
                    graphics.fakeItem(info.getStack(), lX + 1, curY);
                } else {
                    graphics.fill(lX + 2, curY + 1, lX + 12, curY + 11, 0xFF353748);
                }

                String name = info != null ? info.name : blockId;
                String trimmed = this.font.plainSubstrByWidth(name, lW - 32);
                graphics.text(this.font, trimmed, lX + 15, curY + 4, 0xFFE0E0EC, false);

                int colBoxX = lX + lW - 14;
                int colBoxY = curY + 2;
                int blockColor = blockEsp.activeBlocks.getColor(blockId);

                graphics.fill(colBoxX, colBoxY, colBoxX + 11, colBoxY + 11, blockColor);
                RenderUtil.renderBorder(graphics, colBoxX, colBoxY, 11, 11, 0xFFFFFFFF);
            }
            curY += itemH;
        }

        if (activeList.isEmpty()) {
            String noMsg = "No blocks added!";
            int noMsgW = this.font.width(noMsg);
            graphics.text(this.font, noMsg, mX + (mW - noMsgW) / 2, lY + lH / 2 - 4, 0xFF707280, false);
        }
    }

    private void renderColorPickerModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 25% smaller: width 158px, height 138px
        int pW = Math.min(158, this.width - 12);
        int pH = Math.min(138, this.height - 12);
        int pX = (this.width - pW) / 2;
        int pY = (this.height - pH) / 2;

        RenderUtil.renderBox(graphics, pX, pY, pW, pH, 0xFF14151F, 0xFF9333EA);
        graphics.fill(pX, pY, pX + pW, pY + 15, 0xFF9333EA);

        String title = colorPickerTarget == null ? "Global Color" : "Block Color";
        graphics.text(this.font, title, pX + 6, pY + 4, 0xFFFFFFFF, true);

        int cX = pX + pW - 14;
        int cY = pY + 2;
        graphics.text(this.font, "✕", cX + 2, cY + 1, 0xFFFFFFFF, false);

        int currentColor = ColorUtil.toRgb(pickerR, pickerG, pickerB);

        // Preview Swatch
        int prevX = pX + 8;
        int prevY = pY + 19;
        graphics.fill(prevX, prevY, prevX + 22, prevY + 14, currentColor);
        RenderUtil.renderBorder(graphics, prevX, prevY, 22, 14, 0xFFFFFFFF);

        String hexText = ColorUtil.toHex(currentColor);
        graphics.text(this.font, hexText, prevX + 26, prevY + 3, 0xFFE0E0EC, false);

        // RGB Sliders
        int sliderX = pX + 8;
        int sliderW = pW - 16;
        int sY = pY + 37;

        renderColorSlider(graphics, "R", pickerR, sliderX, sY, sliderW, 0xFFFF4444);
        renderColorSlider(graphics, "G", pickerG, sliderX, sY + 14, sliderW, 0xFF44FF44);
        renderColorSlider(graphics, "B", pickerB, sliderX, sY + 28, sliderW, 0xFF4488FF);

        // Palette presets
        int palY = sY + 44;
        int palX = pX + 8;
        for (int col : ColorUtil.PRESETS) {
            graphics.fill(palX, palY, palX + 11, palY + 9, col);
            boolean hov = mouseX >= palX && mouseX <= palX + 11 && mouseY >= palY && mouseY <= palY + 9;
            RenderUtil.renderBorder(graphics, palX, palY, 11, 9, hov ? 0xFFFFFFFF : 0xFF444444);
            palX += 15;
        }

        // Apply Button
        int btnY = pY + pH - 18;
        int btnW = pW - 16;
        boolean dHover = mouseX >= pX + 8 && mouseX <= pX + 8 + btnW && mouseY >= btnY && mouseY <= btnY + 14;
        int dColor = dHover ? 0xFFA855F7 : 0xFF9333EA;
        RenderUtil.renderBox(graphics, pX + 8, btnY, btnW, 14, dColor, 0xFFC084FC);
        String applyText = "Apply";
        int applyW = this.font.width(applyText);
        graphics.text(this.font, applyText, pX + (pW - applyW) / 2, btnY + 3, 0xFFFFFFFF, true);
    }

    private void renderColorSlider(GuiGraphicsExtractor graphics, String label, int value, int x, int y, int w, int barColor) {
        graphics.text(this.font, label + ":" + value, x, y, 0xFFB4B6C8, false);
        int trackX = x + 30;
        int trackW = w - 30;
        int trackY = y + 2;
        int trackH = 5;
        graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF222330);
        int fillW = (int) ((value / 255.0f) * trackW);
        graphics.fill(trackX, trackY, trackX + fillW, trackY + trackH, barColor);
        RenderUtil.renderBorder(graphics, trackX, trackY, trackW, trackH, 0xFF35384D);

        int knobX = trackX + fillW - 2;
        graphics.fill(knobX, trackY - 1, knobX + 3, trackY + trackH + 1, 0xFFFFFFFF);
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
        int winW = Math.min(195, this.width - 12);
        int winH = Math.min(154, this.height - 12);
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;

        // Close button (X)
        int closeX = winX + winW - 14;
        int closeY = winY + 2;
        if (mouseX >= closeX && mouseX <= closeX + 11 && mouseY >= closeY && mouseY <= closeY + 11) {
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }

        // Back button
        int btnW = winW - 16;
        int backBtnH = 14;
        int backBtnY = winY + winH - 18;
        if (mouseX >= winX + 8 && mouseX <= winX + 8 + btnW && mouseY >= backBtnY && mouseY <= backBtnY + backBtnH) {
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }

        int curY = winY + 21;
        int rowH = 17;

        // Row 1: Enabled (Left-click toggles on/off)
        if (mouseX >= winX + 8 && mouseX <= winX + winW - 8 && mouseY >= curY && mouseY <= curY + 15) {
            blockEsp.toggle();
            ConfigService.save();
            return true;
        }
        curY += rowH;

        // Row 2: Tracers
        if (mouseX >= winX + 8 && mouseX <= winX + winW - 8 && mouseY >= curY && mouseY <= curY + 15) {
            blockEsp.tracers.toggle();
            ConfigService.save();
            return true;
        }
        curY += rowH;

        // Row 3: Use Color for all
        boolean useAll = blockEsp.useColorForAll.get();
        if (useAll) {
            int colorBoxX = winX + winW - 35;
            int colorBoxY = curY + 2;
            if (mouseX >= colorBoxX && mouseX <= colorBoxX + 11 && mouseY >= colorBoxY && mouseY <= colorBoxY + 11) {
                openColorPickerFor(null);
                return true;
            }
        }

        if (mouseX >= winX + 8 && mouseX <= winX + winW - 8 && mouseY >= curY && mouseY <= curY + 15) {
            blockEsp.useColorForAll.toggle();
            ConfigService.save();
            return true;
        }
        curY += rowH + 2;

        // Row 4: Color (Active Blocks) button
        int btnH = 15;
        if (mouseX >= winX + 8 && mouseX <= winX + 8 + btnW && mouseY >= curY && mouseY <= curY + btnH) {
            openColorsModal = true;
            colorsModalSearch = "";
            colorsScroll = 0;
            return true;
        }
        curY += btnH + 4;

        // Row 5: Open List button
        if (mouseX >= winX + 8 && mouseX <= winX + 8 + btnW && mouseY >= curY && mouseY <= curY + btnH) {
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
        int mW = Math.min(285, this.width - 12);
        int mH = Math.min(172, this.height - 12);
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        // Close button
        int cX = mX + mW - 14;
        int cY = mY + 2;
        if (mouseX >= cX && mouseX <= cX + 11 && mouseY >= cY && mouseY <= cY + 11) {
            openListModal = false;
            return;
        }

        int colW = (mW - 20) / 2;
        int listH = mH - 58;
        int leftX = mX + 6;
        int leftY = mY + 18;
        int sY = leftY + 9;
        int lY = sY + 15;

        // Left search bar
        if (mouseX >= leftX && mouseX <= leftX + colW && mouseY >= sY && mouseY <= sY + 13) {
            focusedSearch = 1;
            return;
        }

        // Right search bar
        int rightX = mX + 14 + colW;
        if (mouseX >= rightX && mouseX <= rightX + colW && mouseY >= sY && mouseY <= sY + 13) {
            focusedSearch = 2;
            return;
        }

        int itemH = 14;

        // Left List: Add to Active
        if (mouseX >= leftX && mouseX <= leftX + colW && mouseY >= lY && mouseY <= lY + listH) {
            List<BlockInfo> filteredAll = BlockRegistryService.searchAllBlocks(allBlocksSearch);
            int startIdx = allBlocksScroll / itemH;
            int endIdx = Math.min(filteredAll.size(), startIdx + (listH / itemH) + 2);
            int curY = lY - (allBlocksScroll % itemH);

            for (int i = startIdx; i < endIdx && i < filteredAll.size(); i++) {
                BlockInfo info = filteredAll.get(i);
                if (curY >= lY && curY + itemH <= lY + listH) {
                    int plusX = leftX + colW - 13;
                    int plusY = curY + 2;
                    if (mouseX >= plusX && mouseX <= plusX + 10 && mouseY >= plusY && mouseY <= plusY + 10) {
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
                    int minusX = rightX + colW - 13;
                    int minusY = rCurY + 2;
                    if (mouseX >= minusX && mouseX <= minusX + 10 && mouseY >= minusY && mouseY <= minusY + 10) {
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
        int mW = Math.min(195, this.width - 12);
        int mH = Math.min(158, this.height - 12);
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        int cX = mX + mW - 14;
        int cY = mY + 2;
        if (mouseX >= cX && mouseX <= cX + 11 && mouseY >= cY && mouseY <= cY + 11) {
            openColorsModal = false;
            return;
        }

        int sY = mY + 20;
        int lW = mW - 16;
        int lX = mX + 8;
        if (mouseX >= lX && mouseX <= lX + lW && mouseY >= sY && mouseY <= sY + 13) {
            focusedSearch = 3;
            return;
        }

        int lY = sY + 15;
        int lH = mH - 44;
        int itemH = 15;

        if (mouseX >= lX && mouseX <= lX + lW && mouseY >= lY && mouseY <= lY + lH) {
            List<String> activeList = getFilteredColorBlocks();
            int startIdx = colorsScroll / itemH;
            int endIdx = Math.min(activeList.size(), startIdx + (lH / itemH) + 2);
            int curY = lY - (colorsScroll % itemH);

            for (int i = startIdx; i < endIdx && i < activeList.size(); i++) {
                String blockId = activeList.get(i);
                if (curY >= lY && curY + itemH <= lY + lH) {
                    int colBoxX = lX + lW - 14;
                    int colBoxY = curY + 2;
                    if (mouseX >= colBoxX && mouseX <= colBoxX + 11 && mouseY >= colBoxY && mouseY <= colBoxY + 11) {
                        openColorPickerFor(blockId);
                        return;
                    }
                }
                curY += itemH;
            }
        }
    }

    private void handleColorPickerClick(double mouseX, double mouseY, int button) {
        int pW = Math.min(158, this.width - 12);
        int pH = Math.min(138, this.height - 12);
        int pX = (this.width - pW) / 2;
        int pY = (this.height - pH) / 2;

        int cX = pX + pW - 14;
        int cY = pY + 2;
        if (mouseX >= cX && mouseX <= cX + 11 && mouseY >= cY && mouseY <= cY + 11) {
            colorPickerActive = false;
            return;
        }

        int sliderX = pX + 8;
        int sliderW = pW - 16;
        int sY = pY + 37;

        if (mouseX >= sliderX + 30 && mouseX <= sliderX + sliderW) {
            if (mouseY >= sY && mouseY <= sY + 12) {
                draggingSlider = 0;
                updateSliderFromMouse(mouseX, sliderX + 30, sliderW - 30, 0);
                return;
            }
            if (mouseY >= sY + 14 && mouseY <= sY + 26) {
                draggingSlider = 1;
                updateSliderFromMouse(mouseX, sliderX + 30, sliderW - 30, 1);
                return;
            }
            if (mouseY >= sY + 28 && mouseY <= sY + 40) {
                draggingSlider = 2;
                updateSliderFromMouse(mouseX, sliderX + 30, sliderW - 30, 2);
                return;
            }
        }

        // Palettes
        int palY = sY + 44;
        int palX = pX + 8;
        for (int col : ColorUtil.PRESETS) {
            if (mouseX >= palX && mouseX <= palX + 11 && mouseY >= palY && mouseY <= palY + 9) {
                pickerR = ColorUtil.getRed(col);
                pickerG = ColorUtil.getGreen(col);
                pickerB = ColorUtil.getBlue(col);
                applyCurrentPickerColor();
                return;
            }
            palX += 15;
        }

        // Apply Button
        int btnY = pY + pH - 18;
        int btnW = pW - 16;
        if (mouseX >= pX + 8 && mouseX <= pX + 8 + btnW && mouseY >= btnY && mouseY <= btnY + 14) {
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
            int pW = Math.min(158, this.width - 12);
            int pX = (this.width - pW) / 2;
            int sliderX = pX + 8 + 30;
            int sliderW = pW - 16 - 30;
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
        int scrollAmt = (int) (-verticalAmount * 16);
        if (openListModal) {
            int mW = Math.min(285, this.width - 12);
            int mX = (this.width - mW) / 2;
            int colW = (mW - 20) / 2;
            if (mouseX >= mX + 6 && mouseX <= mX + 6 + colW) {
                allBlocksScroll += scrollAmt;
                return true;
            } else if (mouseX >= mX + 14 + colW && mouseX <= mX + 14 + 2 * colW) {
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

package com.maxusclient.client.gui;

import com.maxusclient.client.model.BlockInfo;
import com.maxusclient.client.module.Category;
import com.maxusclient.client.module.Module;
import com.maxusclient.client.module.ModuleManager;
import com.maxusclient.client.module.esp.BlockEsp;
import com.maxusclient.client.service.BlockRegistryService;
import com.maxusclient.client.service.ConfigService;
import com.maxusclient.client.util.ColorUtil;
import com.maxusclient.client.util.RenderUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MaxusMenuScreen extends Screen {

    // UI Window States
    private boolean showSettings = true;
    private boolean showBlockListModal = false;
    private boolean showColorListModal = false;
    private boolean showColorPickerModal = false;

    // Color Picker State
    private String colorPickerTargetId = null; // null = global color
    private String colorPickerTitle = "Global Color";
    private int pickerR = 255;
    private int pickerG = 105;
    private int pickerB = 180;
    private int draggingSlider = -1; // 0=R, 1=G, 2=B

    // Search Strings
    private String allBlocksSearch = "";
    private String activeBlocksSearch = "";
    private String colorsSearch = "";
    private int focusedSearch = 0; // 0=none, 1=all blocks, 2=active blocks, 3=colors

    // Scroll Offsets
    private int allBlocksScroll = 0;
    private int activeBlocksScroll = 0;
    private int colorsScroll = 0;

    public MaxusMenuScreen() {
        super(Component.literal("MaxusClient"));
        BlockRegistryService.ensureCache();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent Meteor dark overlay
        graphics.fill(0, 0, this.width, this.height, 0x66000000);

        BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);

        // 1. ESP Category Window (Left)
        int espX = 30;
        int espY = 30;
        int espW = 150;
        int espH = 65;

        RenderUtil.renderWindow(graphics, this.font, espX, espY, espW, espH, Category.ESP.getDisplayName());

        // Module Button: "Block ESP"
        int btnX = espX + 6;
        int btnY = espY + 24;
        int btnW = espW - 12;
        int btnH = 22;

        boolean isBlockEspOn = blockEsp != null && blockEsp.isEnabled();
        boolean isHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        int btnBg = isBlockEspOn
            ? (isHovered ? 0xFF7252F5 : 0xFF6242EA)
            : (isHovered ? 0xFF232532 : 0xFF191A23);
        int btnBorder = isBlockEspOn ? 0xFF8566FF : 0xFF2D2F3F;
        int btnTextColor = isBlockEspOn ? 0xFFFFFFFF : (isHovered ? 0xFFDDDDFF : 0xFFA5A6B5);

        RenderUtil.renderBox(graphics, btnX, btnY, btnW, btnH, btnBg, btnBorder);
        graphics.text(this.font, "Block ESP", btnX + 8, btnY + 7, btnTextColor, false);
        graphics.text(this.font, "⚙", btnX + btnW - 14, btnY + 7, isBlockEspOn ? 0xFFFFFFFF : 0xFF6A6B7E, false);

        // 2. Settings Panel Window (Next to ESP)
        if (showSettings && blockEsp != null) {
            int setX = espX + espW + 15;
            int setY = espY;
            int setW = 230;
            int setH = 154;

            RenderUtil.renderWindow(graphics, this.font, setX, setY, setW, setH, "Block ESP Settings");

            int rowY = setY + 26;

            // Row 1: Enabled
            graphics.text(this.font, "Enabled", setX + 10, rowY + 3, 0xFFD8D9E5, false);
            RenderUtil.renderCheckbox(graphics, this.font, setX + setW - 24, rowY, blockEsp.isEnabled());

            // Row 2: Tracers
            rowY += 24;
            graphics.text(this.font, "Tracers", setX + 10, rowY + 3, 0xFFD8D9E5, false);
            RenderUtil.renderCheckbox(graphics, this.font, setX + setW - 24, rowY, blockEsp.tracers.get());

            // Row 3: Use Color for all
            rowY += 24;
            graphics.text(this.font, "Use Color for all", setX + 10, rowY + 3, 0xFFD8D9E5, false);
            RenderUtil.renderCheckbox(graphics, this.font, setX + setW - 44, rowY, blockEsp.useColorForAll.get());

            // If Use Color for all is enabled, show global color square right next to it!
            if (blockEsp.useColorForAll.get()) {
                int colBoxX = setX + setW - 24;
                graphics.fill(colBoxX, rowY, colBoxX + 14, rowY + 14, blockEsp.globalColor.get());
                RenderUtil.renderBorder(graphics, colBoxX, rowY, 14, 14, 0xFFFFFFFF);
            }

            // Row 4: Colors (Active Blocks)
            rowY += 24;
            boolean cHover = mouseX >= setX + 6 && mouseX <= setX + setW - 6 && mouseY >= rowY && mouseY <= rowY + 20;
            RenderUtil.renderBox(graphics, setX + 6, rowY, setW - 12, 20, cHover ? 0xFF2A2C3C : 0xFF1C1E29, 0xFF313447);
            graphics.text(this.font, "Color (Active Blocks)", setX + 12, rowY + 6, 0xFFDDDDEE, false);
            graphics.text(this.font, "→", setX + setW - 22, rowY + 6, 0xFF8888AA, false);

            // Row 5: Open List
            rowY += 24;
            boolean lHover = mouseX >= setX + 6 && mouseX <= setX + setW - 6 && mouseY >= rowY && mouseY <= rowY + 20;
            RenderUtil.renderBox(graphics, setX + 6, rowY, setW - 12, 20, lHover ? 0xFF2A2C3C : 0xFF1C1E29, 0xFF313447);
            graphics.text(this.font, "Open List (Block ESP)", setX + 12, rowY + 6, 0xFFDDDDEE, false);
            graphics.text(this.font, "☰", setX + setW - 22, rowY + 6, 0xFF8888AA, false);
        }

        // 3. Modals on top (if open)
        if (showBlockListModal) {
            renderBlockListModal(graphics, mouseX, mouseY);
        } else if (showColorListModal) {
            renderColorListModal(graphics, mouseX, mouseY);
        }

        if (showColorPickerModal) {
            renderColorPickerModal(graphics, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    // ==========================================
    // MODAL: BLOCK SELECTION (DUAL LIST: ALL BLOCKS + ACTIVE BLOCKS)
    // ==========================================
    private void renderBlockListModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, 0x99000000);

        BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);

        int mW = 540;
        int mH = 340;
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        RenderUtil.renderWindow(graphics, this.font, mX, mY, mW, mH, "Block ESP - Block Selection");
        graphics.text(this.font, "✕", mX + mW - 18, mY + 3, 0xFFA0A2B4, false);

        int colW = 250;
        int listH = 260;

        // --- LEFT COLUMN: ALL BLOCKS ---
        int leftX = mX + 12;
        int leftY = mY + 26;
        graphics.text(this.font, "All Blocks (" + BlockRegistryService.getAllBlocks().size() + ")", leftX, leftY, 0xFFB4B6C8, false);

        int sY = leftY + 12;
        RenderUtil.renderSearchBox(graphics, this.font, leftX, sY, colW, allBlocksSearch, focusedSearch == 1, "Search all blocks...");

        int lY = sY + 22;
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
                if (!info.stack.isEmpty()) {
                    graphics.fakeItem(info.stack, leftX + 3, curY + 3);
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

        // --- RIGHT COLUMN: ACTIVE BLOCKS ---
        int rightX = mX + 278;
        int activeCount = blockEsp != null ? blockEsp.activeBlocks.get().size() : 0;
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
                if (info != null && !info.stack.isEmpty()) {
                    graphics.fakeItem(info.stack, rightX + 3, rCurY + 3);
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
                int minusBg = minusHover ? 0xFFE74C3C : 0xFFC0392B;
                RenderUtil.renderBox(graphics, minusX, minusY, 16, 16, minusBg, 0xFFFF5555);
                graphics.text(this.font, "-", minusX + 6, minusY + 4, 0xFFFFFFFF, false);
            }
            rCurY += itemH;
        }
    }

    // ==========================================
    // MODAL: ACTIVE BLOCK COLORS
    // ==========================================
    private void renderColorListModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, 0x99000000);

        BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);

        int mW = 340;
        int mH = 320;
        int mX = (this.width - mW) / 2;
        int mY = (this.height - mH) / 2;

        RenderUtil.renderWindow(graphics, this.font, mX, mY, mW, mH, "Active Block Colors");
        graphics.text(this.font, "✕", mX + mW - 18, mY + 3, 0xFFA0A2B4, false);

        int sY = mY + 28;
        RenderUtil.renderSearchBox(graphics, this.font, mX + 12, sY, mW - 24, colorsSearch, focusedSearch == 3, "Search active blocks...");

        int lX = mX + 12;
        int lY = sY + 22;
        int lW = mW - 24;
        int lH = 240;

        RenderUtil.renderBox(graphics, lX, lY, lW, lH, 0xFF101116, 0xFF252735);

        List<String> activeList = getFilteredColorBlocks();
        int itemH = 24;
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

                if (info != null && !info.stack.isEmpty()) {
                    graphics.fakeItem(info.stack, lX + 4, curY + 4);
                } else {
                    graphics.fill(lX + 4, curY + 4, lX + 20, curY + 20, 0xFF353748);
                }

                String name = info != null ? info.name : blockId;
                String trimmed = this.font.plainSubstrByWidth(name, lW - 56);
                graphics.text(this.font, trimmed, lX + 26, curY + 8, 0xFFE0E0EC, false);

                int colBoxX = lX + lW - 22;
                int colBoxY = curY + 4;
                int blockColor = blockEsp != null ? blockEsp.activeBlocks.getColor(blockId) : 0xFFFF69B4;

                graphics.fill(colBoxX, colBoxY, colBoxX + 16, colBoxY + 16, blockColor);
                RenderUtil.renderBorder(graphics, colBoxX, colBoxY, 16, 16, 0xFFFFFFFF);
            }
            curY += itemH;
        }
    }

    // ==========================================
    // MODAL: COLOR PICKER
    // ==========================================
    private void renderColorPickerModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, 0xBB000000);

        int pW = 250;
        int pH = 230;
        int pX = (this.width - pW) / 2;
        int pY = (this.height - pH) / 2;

        RenderUtil.renderWindow(graphics, this.font, pX, pY, pW, pH, "Color: " + this.font.plainSubstrByWidth(colorPickerTitle, 160));
        graphics.text(this.font, "✕", pX + pW - 18, pY + 3, 0xFFA0A2B4, false);

        int currentColor = ColorUtil.toRgb(pickerR, pickerG, pickerB);

        // Preview Box & Hex string
        graphics.fill(pX + 14, pY + 26, pX + 64, pY + 54, currentColor);
        RenderUtil.renderBorder(graphics, pX + 14, pY + 26, 50, 28, 0xFFFFFFFF);

        String hex = ColorUtil.toHex(currentColor);
        graphics.text(this.font, hex, pX + 72, pY + 30, 0xFFFFFFFF, false);
        graphics.text(this.font, "RGB: " + pickerR + ", " + pickerG + ", " + pickerB, pX + 72, pY + 42, 0xFFAAAABB, false);

        // Sliders (Red, Green, Blue)
        int sliderY = pY + 64;
        renderSlider(graphics, pX + 14, sliderY, pW - 28, "Red: " + pickerR, pickerR / 255.0f, 0xFFE74C3C);
        sliderY += 24;
        renderSlider(graphics, pX + 14, sliderY, pW - 28, "Green: " + pickerG, pickerG / 255.0f, 0xFF2ECC71);
        sliderY += 24;
        renderSlider(graphics, pX + 14, sliderY, pW - 28, "Blue: " + pickerB, pickerB / 255.0f, 0xFF3498DB);

        // Preset Colors Palette
        int palY = sliderY + 26;
        int palX = pX + 14;
        for (int c : ColorUtil.PRESETS) {
            graphics.fill(palX, palY, palX + 18, palY + 14, c);
            RenderUtil.renderBorder(graphics, palX, palY, 18, 14, (c == currentColor) ? 0xFFFFFFFF : 0xFF444455);
            palX += 28;
        }

        // Done Button
        int doneY = pY + pH - 28;
        boolean doneHover = mouseX >= pX + 14 && mouseX <= pX + pW - 14 && mouseY >= doneY && mouseY <= doneY + 18;
        int doneBg = doneHover ? 0xFF7252F5 : 0xFF6242EA;
        RenderUtil.renderBox(graphics, pX + 14, doneY, pW - 28, 18, doneBg, 0xFF8566FF);
        graphics.centeredText(this.font, "Save & Close", pX + pW / 2, doneY + 5, 0xFFFFFFFF);
    }

    private void renderSlider(GuiGraphicsExtractor graphics, int x, int y, int w, String text, float value, int barColor) {
        graphics.text(this.font, text, x, y - 1, 0xFFDDDDDD, false);
        int trackY = y + 10;
        graphics.fill(x, trackY, x + w, trackY + 6, 0xFF222432);
        int fillW = (int) (w * value);
        if (fillW > 0) {
            graphics.fill(x, trackY, x + fillW, trackY + 6, barColor);
        }
        int knobX = x + fillW - 3;
        graphics.fill(knobX, trackY - 2, knobX + 6, trackY + 8, 0xFFFFFFFF);
    }

    // ==========================================
    // MOUSE INPUT HANDLERS
    // ==========================================
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);

        // 1. Color Picker Active
        if (showColorPickerModal) {
            int pW = 250;
            int pH = 230;
            int pX = (this.width - pW) / 2;
            int pY = (this.height - pH) / 2;

            if (mx >= pX + pW - 18 && mx <= pX + pW - 4 && my >= pY + 3 && my <= pY + 17) {
                closeColorPicker();
                return true;
            }

            int sliderY = pY + 64;
            if (mx >= pX + 14 && mx <= pX + pW - 14) {
                if (my >= sliderY + 8 && my <= sliderY + 18) {
                    draggingSlider = 0;
                    updateSliderValue(0, (float) (mx - (pX + 14)) / (pW - 28));
                    return true;
                }
                if (my >= sliderY + 32 && my <= sliderY + 42) {
                    draggingSlider = 1;
                    updateSliderValue(1, (float) (mx - (pX + 14)) / (pW - 28));
                    return true;
                }
                if (my >= sliderY + 56 && my <= sliderY + 66) {
                    draggingSlider = 2;
                    updateSliderValue(2, (float) (mx - (pX + 14)) / (pW - 28));
                    return true;
                }
            }

            int palY = sliderY + 26;
            int palX = pX + 14;
            for (int c : ColorUtil.PRESETS) {
                if (mx >= palX && mx <= palX + 18 && my >= palY && my <= palY + 14) {
                    applyColor(c);
                    return true;
                }
                palX += 28;
            }

            int doneY = pY + pH - 28;
            if (mx >= pX + 14 && mx <= pX + pW - 14 && my >= doneY && my <= doneY + 18) {
                closeColorPicker();
                return true;
            }
            return true;
        }

        // 2. Active Block Colors Modal Active
        if (showColorListModal) {
            int mW = 340;
            int mH = 320;
            int mX = (this.width - mW) / 2;
            int mY = (this.height - mH) / 2;

            if (mx >= mX + mW - 18 && mx <= mX + mW - 4 && my >= mY + 3 && my <= mY + 17) {
                showColorListModal = false;
                return true;
            }

            int sY = mY + 28;
            if (mx >= mX + 12 && mx <= mX + mW - 12 && my >= sY && my <= sY + 16) {
                focusedSearch = 3;
                return true;
            }

            int lX = mX + 12;
            int lY = sY + 22;
            int lW = mW - 24;
            int lH = 240;

            if (mx >= lX && mx <= lX + lW && my >= lY && my <= lY + lH) {
                List<String> activeList = getFilteredColorBlocks();
                int itemH = 24;
                int startIdx = colorsScroll / itemH;
                int clickedIndex = startIdx + (int) ((my - lY + (colorsScroll % itemH)) / itemH);
                if (clickedIndex >= 0 && clickedIndex < activeList.size()) {
                    String blockId = activeList.get(clickedIndex);
                    BlockInfo info = BlockRegistryService.findBlockInfo(blockId);
                    int col = blockEsp != null ? blockEsp.activeBlocks.getColor(blockId) : 0xFFFF69B4;
                    openColorPicker(blockId, info != null ? info.name : blockId, col);
                    return true;
                }
            }
            return true;
        }

        // 3. Block Selection Modal Active (Dual List)
        if (showBlockListModal) {
            int mW = 540;
            int mH = 340;
            int mX = (this.width - mW) / 2;
            int mY = (this.height - mH) / 2;

            if (mx >= mX + mW - 18 && mx <= mX + mW - 4 && my >= mY + 3 && my <= mY + 17) {
                showBlockListModal = false;
                return true;
            }

            int colW = 250;
            int listH = 260;
            int leftX = mX + 12;
            int leftY = mY + 26;
            int sY = leftY + 12;
            int lY = sY + 22;

            if (mx >= leftX && mx <= leftX + colW && my >= sY && my <= sY + 16) {
                focusedSearch = 1;
                return true;
            }

            int rightX = mX + 278;
            if (mx >= rightX && mx <= rightX + colW && my >= sY && my <= sY + 16) {
                focusedSearch = 2;
                return true;
            }

            if (mx >= leftX && mx <= leftX + colW && my >= lY && my <= lY + listH && blockEsp != null) {
                List<BlockInfo> filteredAll = BlockRegistryService.searchAllBlocks(allBlocksSearch);
                int itemH = 22;
                int startIdx = allBlocksScroll / itemH;
                int clickedIdx = startIdx + (int) ((my - lY + (allBlocksScroll % itemH)) / itemH);
                if (clickedIdx >= 0 && clickedIdx < filteredAll.size()) {
                    BlockInfo info = filteredAll.get(clickedIdx);
                    blockEsp.activeBlocks.addBlock(info.id.toString(), 0xFFFF69B4);
                    ConfigService.save();
                    return true;
                }
            }

            if (mx >= rightX && mx <= rightX + colW && my >= lY && my <= lY + listH && blockEsp != null) {
                List<String> filteredActive = getFilteredActiveBlocks();
                int itemH = 22;
                int startIdx = activeBlocksScroll / itemH;
                int clickedIdx = startIdx + (int) ((my - lY + (activeBlocksScroll % itemH)) / itemH);
                if (clickedIdx >= 0 && clickedIdx < filteredActive.size()) {
                    String blockId = filteredActive.get(clickedIdx);
                    blockEsp.activeBlocks.removeBlock(blockId);
                    ConfigService.save();
                    return true;
                }
            }
            return true;
        }

        // 4. Main Menu Clicks
        int espX = 30;
        int espY = 30;
        int espW = 150;
        int btnX = espX + 6;
        int btnY = espY + 24;
        int btnW = espW - 12;
        int btnH = 22;

        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH && blockEsp != null) {
            if (button == 0) { // Left click = Toggle ON/OFF
                blockEsp.toggle();
                ConfigService.save();
                return true;
            } else if (button == 1) { // Right click = Toggle Settings
                showSettings = !showSettings;
                return true;
            }
        }

        if (showSettings && blockEsp != null) {
            int setX = espX + espW + 15;
            int setY = espY;
            int setW = 230;

            int rowY = setY + 26;

            // Row 1: Enabled
            if (mx >= setX + 6 && mx <= setX + setW - 6 && my >= rowY && my <= rowY + 18) {
                blockEsp.toggle();
                ConfigService.save();
                return true;
            }

            // Row 2: Tracers
            rowY += 24;
            if (mx >= setX + 6 && mx <= setX + setW - 6 && my >= rowY && my <= rowY + 18) {
                blockEsp.tracers.toggle();
                ConfigService.save();
                return true;
            }

            // Row 3: Use Color for all
            rowY += 24;
            if (mx >= setX + 6 && mx <= setX + setW - 28 && my >= rowY && my <= rowY + 18) {
                blockEsp.useColorForAll.toggle();
                ConfigService.save();
                return true;
            }

            if (blockEsp.useColorForAll.get()) {
                int colBoxX = setX + setW - 24;
                if (mx >= colBoxX && mx <= colBoxX + 16 && my >= rowY && my <= rowY + 16) {
                    openColorPicker(null, "Global Color", blockEsp.globalColor.get());
                    return true;
                }
            }

            // Row 4: Colors (Active Blocks)
            rowY += 24;
            if (mx >= setX + 6 && mx <= setX + setW - 6 && my >= rowY && my <= rowY + 20) {
                showColorListModal = true;
                return true;
            }

            // Row 5: Open List
            rowY += 24;
            if (mx >= setX + 6 && mx <= setX + setW - 6 && my >= rowY && my <= rowY + 20) {
                showBlockListModal = true;
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingSlider = -1;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (showColorPickerModal && draggingSlider >= 0) {
            int pW = 250;
            int pX = (this.width - pW) / 2;
            float val = (float) (event.x() - (pX + 14)) / (pW - 28);
            updateSliderValue(draggingSlider, val);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int delta = (int) (verticalAmount * -24);

        if (showBlockListModal) {
            int mW = 540;
            int mX = (this.width - mW) / 2;
            int leftX = mX + 12;
            int rightX = mX + 278;
            int colW = 250;

            if (mouseX >= leftX && mouseX <= leftX + colW) {
                allBlocksScroll += delta;
                return true;
            }
            if (mouseX >= rightX && mouseX <= rightX + colW) {
                activeBlocksScroll += delta;
                return true;
            }
        } else if (showColorListModal) {
            colorsScroll += delta;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ==========================================
    // KEYBOARD INPUT HANDLERS
    // ==========================================
    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == InputConstants.KEY_ESCAPE) {
            if (showColorPickerModal) {
                closeColorPicker();
                return true;
            }
            if (showBlockListModal) {
                showBlockListModal = false;
                return true;
            }
            if (showColorListModal) {
                showColorListModal = false;
                return true;
            }
            this.onClose();
            return true;
        }

        if (key == InputConstants.KEY_BACKSPACE) {
            if (focusedSearch == 1 && !allBlocksSearch.isEmpty()) {
                allBlocksSearch = allBlocksSearch.substring(0, allBlocksSearch.length() - 1);
                allBlocksScroll = 0;
                return true;
            }
            if (focusedSearch == 2 && !activeBlocksSearch.isEmpty()) {
                activeBlocksSearch = activeBlocksSearch.substring(0, activeBlocksSearch.length() - 1);
                activeBlocksScroll = 0;
                return true;
            }
            if (focusedSearch == 3 && !colorsSearch.isEmpty()) {
                colorsSearch = colorsSearch.substring(0, colorsSearch.length() - 1);
                colorsScroll = 0;
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
                allBlocksScroll = 0;
                return true;
            }
            if (focusedSearch == 2) {
                activeBlocksSearch += ch;
                activeBlocksScroll = 0;
                return true;
            }
            if (focusedSearch == 3) {
                colorsSearch += ch;
                colorsScroll = 0;
                return true;
            }
        }
        return super.charTyped(event);
    }

    // ==========================================
    // COLOR PICKER HELPERS
    // ==========================================
    private void openColorPicker(String targetId, String title, int initialColor) {
        this.colorPickerTargetId = targetId;
        this.colorPickerTitle = title;
        this.pickerR = ColorUtil.getRed(initialColor);
        this.pickerG = ColorUtil.getGreen(initialColor);
        this.pickerB = ColorUtil.getBlue(initialColor);
        this.showColorPickerModal = true;
    }

    private void updateSliderValue(int slider, float val) {
        int v = Math.max(0, Math.min(255, Math.round(val * 255.0f)));
        if (slider == 0) pickerR = v;
        if (slider == 1) pickerG = v;
        if (slider == 2) pickerB = v;
        savePickerColor();
    }

    private void applyColor(int argb) {
        this.pickerR = ColorUtil.getRed(argb);
        this.pickerG = ColorUtil.getGreen(argb);
        this.pickerB = ColorUtil.getBlue(argb);
        savePickerColor();
    }

    private void savePickerColor() {
        int color = ColorUtil.toRgb(pickerR, pickerG, pickerB);
        BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);
        if (blockEsp != null) {
            if (colorPickerTargetId == null) {
                blockEsp.globalColor.set(color);
            } else {
                blockEsp.activeBlocks.setColor(colorPickerTargetId, color);
            }
            ConfigService.save();
        }
    }

    private void closeColorPicker() {
        savePickerColor();
        this.showColorPickerModal = false;
        this.draggingSlider = -1;
    }

    private List<String> getFilteredActiveBlocks() {
        List<String> list = new ArrayList<>();
        BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);
        if (blockEsp == null) return list;

        String query = activeBlocksSearch.toLowerCase().trim();
        for (String id : blockEsp.activeBlocks.get().keySet()) {
            BlockInfo info = BlockRegistryService.findBlockInfo(id);
            String name = info != null ? info.name : id;
            if (activeBlocksSearch.isEmpty() || name.toLowerCase().contains(query) || id.toLowerCase().contains(query)) {
                list.add(id);
            }
        }
        return list;
    }

    private List<String> getFilteredColorBlocks() {
        List<String> list = new ArrayList<>();
        BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);
        if (blockEsp == null) return list;

        String query = colorsSearch.toLowerCase().trim();
        for (String id : blockEsp.activeBlocks.get().keySet()) {
            BlockInfo info = BlockRegistryService.findBlockInfo(id);
            String name = info != null ? info.name : id;
            if (colorsSearch.isEmpty() || name.toLowerCase().contains(query) || id.toLowerCase().contains(query)) {
                list.add(id);
            }
        }
        return list;
    }

    @Override
    public void onClose() {
        ConfigService.save();
        super.onClose();
    }
}

package com.iterablock.client.gui.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.gui.GuiBuilderHelperMainMenu;
import com.iterablock.client.hotkeys.VanillaKeyMappings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.glfw.GLFW;

public class GuiBuilderHelperSettings extends GuiBase {
    private static final int SAFE_MARGIN = 22;
    private static final int TOP_Y = 28;
    private static final int TAB_HEIGHT = 15;
    private static final int TAB_GAP = 2;
    private static final int ROW_HEIGHT = 22;
    private static final int ROW_GAP = 2;
    private static final int VALUE_WIDTH = 138;
    private static final int RESET_WIDTH = 48;
    private static final int BACK_BUTTON_WIDTH = 52;
    private static final int BACK_BUTTON_HEIGHT = 16;
    private static final double TEXT_SCALE = 0.56;
    private static final double HOVER_SPEED = 8.0;
    private static final int INDICATOR_SEGMENTS = 3;
    private static final int INDICATOR_TOP_RGB = 0xF3C6D3;
    private static final int INDICATOR_MIDDLE_RGB = 0xBFE3D7;
    private static final int INDICATOR_BOTTOM_RGB = 0xF6D6A8;
    private static final int FRAME_RGB = 0xF4F7F7;
    private static final int GREEN = 0xFF76D18A;
    private static final int RED = 0xFFE27676;
    private static final int TEXT = 0xFFDDE8E8;
    private static final int MUTED = 0xFF91A0A3;

    private final List<ConfigEntry> entries = new ArrayList<>();
    private final double[] tabHover = new double[Category.values().length];
    private final double[][] tabIndicator = new double[Category.values().length][INDICATOR_SEGMENTS];
    private final double[][] tabIndicatorVelocity = new double[Category.values().length][INDICATOR_SEGMENTS];
    private double[] valueHover = new double[0];
    private double[] resetHover = new double[0];
    private double backHover;
    private Category category = Category.LITEMATIC;
    private ConfigEntry editingEntry;
    private int scrollOffset;
    private long lastFrameNanos;
    private final boolean returnToMainMenu;
    private ConfigEntry listeningKeyEntry;

    public GuiBuilderHelperSettings() {
        this(false);
    }

    public GuiBuilderHelperSettings(boolean returnToMainMenu) {
        this.returnToMainMenu = returnToMainMenu;
        this.setTitle(Lang.tr("iterablock.gui.settings.title"));
        this.createEntries();
        this.loadConfig();
        this.ensureHoverSize();
    }

    @Override
    public void initGui() {
        this.clearElements();
    }

    @Override
    protected void drawScreenBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x76000000);
    }

    @Override
    protected void drawTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void drawContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Layout layout = this.createLayout();
        this.updateAnimations(mouseX, mouseY, layout);

        this.drawTextWithShadow(guiGraphics, Lang.tr("iterablock.gui.settings.title"), layout.x(), layout.titleY(), 0xFFF3EECF);
        this.drawTabs(guiGraphics, mouseX, mouseY, layout);
        this.drawListPanel(guiGraphics, mouseX, mouseY, layout);
        this.drawCategoryNote(guiGraphics, layout);
        this.drawBackButton(guiGraphics, mouseX, mouseY, layout);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        Layout layout = this.createLayout();
        int tabX = layout.x();

        if (this.isInside(mouseX, mouseY, layout.backX(), layout.backY(), BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT)) {
            this.listeningKeyEntry = null;
            this.returnToMainMenu();
            return true;
        }

        for (Category tab : Category.values()) {
            int tabWidth = this.getTabWidth(tab);

            if (this.isInside(mouseX, mouseY, tabX, layout.tabY(), tabWidth, TAB_HEIGHT)) {
                this.category = tab;
                this.scrollOffset = 0;
                this.editingEntry = null;
                this.listeningKeyEntry = null;
                return true;
            }

            tabX += tabWidth + TAB_GAP;
        }

        for (RowPlacement row : this.getVisibleRows(layout)) {
            ConfigEntry entry = row.entry();

            if (this.isInside(mouseX, mouseY, row.valueX(), row.y() + 3, row.valueWidth(), 16)) {
                this.handleValueClick(entry);
                return true;
            }

            if (this.isInside(mouseX, mouseY, row.resetX(), row.y() + 3, RESET_WIDTH, 16)) {
                entry.reset();
                if (entry.type == EntryType.KEYBIND) {
                    VanillaKeyMappings.resetKey(entry.key);
                    entry.value = VanillaKeyMappings.getKeyName(entry.key, entry.defaultValue);
                }
                this.editingEntry = null;
                this.listeningKeyEntry = null;
                this.saveConfig();
                return true;
            }
        }

        this.editingEntry = null;
        this.listeningKeyEntry = null;
        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseScrolled(int mouseX, int mouseY, double amount, double amountHorizontal) {
        Layout layout = this.createLayout();

        if (this.isInside(mouseX, mouseY, layout.x(), layout.listY(), layout.width(), layout.listHeight())) {
            int maxOffset = this.getMaxScrollOffset(layout);
            this.scrollOffset = Math.max(0, Math.min(maxOffset, this.scrollOffset - (int) Math.signum(amount)));
            return true;
        }

        return super.onMouseScrolled(mouseX, mouseY, amount, amountHorizontal);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (this.listeningKeyEntry != null) {
            if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
                return true;
            }

            this.listeningKeyEntry.value = BuilderHelperClientConfig.keyNameFromCode(keyCode);
            VanillaKeyMappings.setKey(this.listeningKeyEntry.key, keyCode);
            this.listeningKeyEntry.value = VanillaKeyMappings.getKeyName(this.listeningKeyEntry.key, this.listeningKeyEntry.value);
            this.listeningKeyEntry = null;
            this.editingEntry = null;
            this.saveConfig();
            return true;
        }

        if (this.editingEntry != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !this.editingEntry.value.isEmpty()) {
                this.editingEntry.value = this.editingEntry.value.substring(0, this.editingEntry.value.length() - 1);
                this.saveConfig();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                this.editingEntry.value = "";
                this.saveConfig();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.editingEntry = null;
                this.listeningKeyEntry = null;
                this.saveConfig();
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.closeGui(true);
            if (this.returnToMainMenu) {
                GuiBase.openGui(new GuiBuilderHelperMainMenu());
            }
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(char character, int modifiers) {
        if (this.listeningKeyEntry != null) {
            return true;
        }

        if (this.editingEntry != null && character >= 32 && character != 127 && this.editingEntry.value.length() < 120) {
            this.editingEntry.value += character;
            this.saveConfig();
            return true;
        }

        return super.onCharTyped(character, modifiers);
    }

    private void drawTabs(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        int x = layout.x();

        for (Category tab : Category.values()) {
            int width = this.getTabWidth(tab);
            boolean selected = tab == this.category;
            boolean pressed = this.isLeftMouseDown() && this.isInside(mouseX, mouseY, x, layout.tabY(), width, TAB_HEIGHT);
            double hover = this.easeOutCubic(this.tabHover[tab.ordinal()]);
            int fill = this.withAlpha(this.blendRgb(0x111719, 0x273033, Math.max(hover, selected ? 0.7 : 0.0)), selected ? 0.76 : 0.58 + hover * 0.16);

            guiGraphics.fill(x, layout.tabY(), x + width, layout.tabY() + TAB_HEIGHT, fill);
            this.drawRectFrame(guiGraphics, x, layout.tabY(), width, TAB_HEIGHT, selected ? 0.62 : 0.26 + hover * 0.24);
            this.drawSegmentIndicator(guiGraphics, x + 3, layout.tabY() + TAB_HEIGHT - 6, width - 6, this.tabIndicator[tab.ordinal()], pressed ? IndicatorState.PRESSED : selected ? IndicatorState.SELECTED : hover > 0.02 ? IndicatorState.HOVERED : IndicatorState.NORMAL);
            this.drawText(guiGraphics, tab.title(), x + 3, layout.tabY() + 3, selected || hover > 0.02 ? 0xFFFFF1B0 : TEXT);
            x += width + TAB_GAP;
        }
    }

    private void drawListPanel(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        guiGraphics.fill(layout.x(), layout.listY(), layout.x() + layout.width(), layout.listY() + layout.listHeight(), 0x7C0B1012);
        this.drawCornerFrame(guiGraphics, layout.x(), layout.listY(), layout.width(), layout.listHeight(), 7, 0.42);

        for (RowPlacement row : this.getVisibleRows(layout)) {
            this.drawRow(guiGraphics, mouseX, mouseY, row);
        }

        this.drawScrollbar(guiGraphics, layout);
    }

    private void drawCategoryNote(GuiGraphics guiGraphics, Layout layout) {
        if (this.category != Category.FILES) {
            return;
        }

        int y = layout.listY() + layout.listHeight() - 28;
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.settings.note.litematic_path.absolute"), layout.x() + 16, y, MUTED);
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.settings.note.litematic_path.default"), layout.x() + 16, y + 12, MUTED);
    }

    private void drawBackButton(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        double hover = this.easeOutCubic(this.backHover);
        int offset = (int) Math.round(hover * 3.0);
        int x = layout.backX() + offset;
        int y = layout.backY();

        this.drawSimpleButtonBox(guiGraphics, x, y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, hover);
        String backText = Lang.tr("iterablock.gui.button.back");
        int backTextX = x + Math.max(2, (BACK_BUTTON_WIDTH - this.getScaledStringWidth(backText)) / 2);
        this.drawText(guiGraphics, backText, backTextX, y + 5, 0xFFFFFFFF);
    }

    private void drawRow(GuiGraphics guiGraphics, int mouseX, int mouseY, RowPlacement row) {
        ConfigEntry entry = row.entry();
        double valueHoverEase = this.easeOutCubic(this.valueHover[entry.index]);
        double resetHoverEase = this.easeOutCubic(this.resetHover[entry.index]);
        int y = row.y();
        int valueX = row.valueX() + (int) Math.round(valueHoverEase * 3.0);
        int valueTextColor = entry.type == EntryType.BOOLEAN ? (entry.booleanValue() ? GREEN : RED) : TEXT;

        guiGraphics.fill(row.x(), y, row.x() + row.width(), y + ROW_HEIGHT, 0x33151A1D);
        this.drawRectFrame(guiGraphics, row.x(), y, row.width(), ROW_HEIGHT, 0.24);
        this.drawText(guiGraphics, entry.label(), row.x() + 10, y + 8, TEXT);

        this.drawSimpleButtonBox(guiGraphics, valueX, y + 3, row.valueWidth(), 16, valueHoverEase);
        String valueText = entry.displayValue(this.editingEntry == entry, this.listeningKeyEntry == entry);
        int valueTextX = valueX + Math.max(4, (row.valueWidth() - this.getScaledStringWidth(valueText)) / 2);
        this.drawText(guiGraphics, valueText, valueTextX, y + 8, valueTextColor);

        this.drawSimpleButtonBox(guiGraphics, row.resetX(), y + 3, RESET_WIDTH, 16, resetHoverEase);
        String resetText = Lang.tr("iterablock.gui.settings.reset");
        int resetTextX = row.resetX() + Math.max(2, (RESET_WIDTH - this.getScaledStringWidth(resetText)) / 2);
        this.drawText(guiGraphics, resetText, resetTextX, y + 8, 0xFFFFFFFF);
    }

    private void drawSimpleButtonBox(GuiGraphics guiGraphics, int x, int y, int width, int height, double hover) {
        int fill = hover > 0.02 ? 0xAA5E6666 : 0x8A4A5050;
        int border = hover > 0.02 ? 0xE8FFFFFF : 0xBFFFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    private void drawSegmentIndicator(GuiGraphics guiGraphics, int x, int y, int availableWidth, double[] progress, IndicatorState state) {
        int shortLength = Math.max(5, Math.min(8, availableWidth));
        int middleLength = Math.max(shortLength + 5, Math.min(18, availableWidth));
        int longLength = Math.max(middleLength + 8, Math.min(36, availableWidth));
        int thickness = state == IndicatorState.SELECTED ? 2 : 1;
        double strongestProgress = Math.max(progress[0], Math.max(progress[1], progress[2]));
        double baseAlpha = switch (state) {
            case SELECTED -> 0.92;
            case HOVERED -> 0.34 + strongestProgress * 0.48;
            case PRESSED -> 0.55;
            case DISABLED -> 0.10;
            case NORMAL -> 0.18;
        };
        double compress = state == IndicatorState.PRESSED ? 0.86 : 1.0;

        this.drawIndicatorSegment(guiGraphics, x, y, shortLength, thickness, progress[0] * compress, baseAlpha * 0.88, INDICATOR_TOP_RGB);
        this.drawIndicatorSegment(guiGraphics, x, y + 2, middleLength, thickness, progress[1] * compress, baseAlpha * 0.92, INDICATOR_MIDDLE_RGB);
        this.drawIndicatorSegment(guiGraphics, x, y + 4, longLength, thickness, progress[2] * compress, baseAlpha, INDICATOR_BOTTOM_RGB);
    }

    private int drawIndicatorSegment(GuiGraphics guiGraphics, int x, int y, int length, int thickness, double progress, double alpha, int color) {
        int visible = Math.max(0, (int) Math.round(length * Math.max(0.0, Math.min(1.0, progress))));

        if (visible <= 0) {
            return x;
        }

        this.drawSmoothLine(guiGraphics, x, y + thickness * 0.5, x + visible, y + thickness * 0.5, thickness + 2.0, this.withAlpha(color, alpha * 0.18));
        this.drawSmoothLine(guiGraphics, x, y + thickness * 0.5, x + visible, y + thickness * 0.5, thickness, this.withAlpha(color, alpha));
        return x + length;
    }

    private void drawCornerFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, int cornerLength, double alpha) {
        int color = this.withAlpha(FRAME_RGB, alpha);
        int glow = this.withAlpha(FRAME_RGB, alpha * 0.05);
        int right = x + width;
        int bottom = y + height;
        int cut = Math.max(3, Math.min(cornerLength, Math.min(width, height) / 3));

        this.drawChamferedRing(guiGraphics, x, y, right, bottom, cut, 2.4, glow);
        this.drawChamferedRing(guiGraphics, x, y, right, bottom, cut, 1.0, color);
    }

    private void drawRectFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, double alpha) {
        int color = this.withAlpha(FRAME_RGB, alpha);
        int glow = this.withAlpha(FRAME_RGB, alpha * 0.05);
        int right = x + width;
        int bottom = y + height;

        this.drawRectRing(guiGraphics, x, y, right, bottom, 2.2, glow);
        this.drawRectRing(guiGraphics, x, y, right, bottom, 1.0, color);
    }

    private void drawControlFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, double alpha) {
        int color = this.withAlpha(0xDDE4E4, alpha);
        int glow = this.withAlpha(0xF4F7F7, alpha * 0.08);
        int right = x + width;
        int bottom = y + height;

        this.drawRectRing(guiGraphics, x, y, right, bottom, 3.0, glow);
        this.drawRectRing(guiGraphics, x, y, right, bottom, 1.55, color);
    }

    private void drawRectRing(GuiGraphics guiGraphics, double x, double y, double right, double bottom, double thickness, int color) {
        double innerX = x + thickness;
        double innerY = y + thickness;
        double innerRight = right - thickness;
        double innerBottom = bottom - thickness;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        this.addQuad(buffer, guiGraphics, x, y, right, y, innerRight, innerY, innerX, innerY, color);
        this.addQuad(buffer, guiGraphics, right, y, right, bottom, innerRight, innerBottom, innerRight, innerY, color);
        this.addQuad(buffer, guiGraphics, right, bottom, x, bottom, innerX, innerBottom, innerRight, innerBottom, color);
        this.addQuad(buffer, guiGraphics, x, bottom, x, y, innerX, innerY, innerX, innerBottom, color);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
    }

    private void drawChamferedRing(GuiGraphics guiGraphics, double x, double y, double right, double bottom, double cut, double thickness, int color) {
        double inset = thickness;
        double[][] outer = {
                {x + cut, y},
                {right - cut, y},
                {right, y + cut},
                {right, bottom - cut},
                {right - cut, bottom},
                {x + cut, bottom},
                {x, bottom - cut},
                {x, y + cut}
        };
        double innerX = x + inset;
        double innerY = y + inset;
        double innerRight = right - inset;
        double innerBottom = bottom - inset;
        double innerCut = Math.max(1.0, cut - inset * 0.42);
        double[][] inner = {
                {innerX + innerCut, innerY},
                {innerRight - innerCut, innerY},
                {innerRight, innerY + innerCut},
                {innerRight, innerBottom - innerCut},
                {innerRight - innerCut, innerBottom},
                {innerX + innerCut, innerBottom},
                {innerX, innerBottom - innerCut},
                {innerX, innerY + innerCut}
        };

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < outer.length; i++) {
            int next = (i + 1) % outer.length;
            buffer.addVertex(guiGraphics.pose().last(), (float) outer[i][0], (float) outer[i][1], 0.0F).setColor(color);
            buffer.addVertex(guiGraphics.pose().last(), (float) outer[next][0], (float) outer[next][1], 0.0F).setColor(color);
            buffer.addVertex(guiGraphics.pose().last(), (float) inner[next][0], (float) inner[next][1], 0.0F).setColor(color);
            buffer.addVertex(guiGraphics.pose().last(), (float) inner[i][0], (float) inner[i][1], 0.0F).setColor(color);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void addQuad(BufferBuilder buffer, GuiGraphics guiGraphics, double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, int color) {
        buffer.addVertex(guiGraphics.pose().last(), (float) x1, (float) y1, 0.0F).setColor(color);
        buffer.addVertex(guiGraphics.pose().last(), (float) x2, (float) y2, 0.0F).setColor(color);
        buffer.addVertex(guiGraphics.pose().last(), (float) x3, (float) y3, 0.0F).setColor(color);
        buffer.addVertex(guiGraphics.pose().last(), (float) x4, (float) y4, 0.0F).setColor(color);
    }

    private void drawSmoothLine(GuiGraphics guiGraphics, double x1, double y1, double x2, double y2, double thickness, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length <= 0.001) {
            return;
        }

        double normalX = -dy / length * thickness * 0.5;
        double normalY = dx / length * thickness * 0.5;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(guiGraphics.pose().last(), (float) (x1 + normalX), (float) (y1 + normalY), 0.0F).setColor(color);
        buffer.addVertex(guiGraphics.pose().last(), (float) (x2 + normalX), (float) (y2 + normalY), 0.0F).setColor(color);
        buffer.addVertex(guiGraphics.pose().last(), (float) (x2 - normalX), (float) (y2 - normalY), 0.0F).setColor(color);
        buffer.addVertex(guiGraphics.pose().last(), (float) (x1 - normalX), (float) (y1 - normalY), 0.0F).setColor(color);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
    }

    private void drawScrollbar(GuiGraphics guiGraphics, Layout layout) {
        int totalRows = this.getEntriesForCategory().size();
        int visibleRows = this.getVisibleRowCount(layout);

        if (totalRows <= visibleRows) {
            return;
        }

        int barX = layout.x() + layout.width() - 5;
        int trackY = layout.listY() + 4;
        int trackHeight = layout.listHeight() - 8;
        int thumbHeight = Math.max(16, trackHeight * visibleRows / totalRows);
        int maxOffset = Math.max(1, totalRows - visibleRows);
        int thumbY = trackY + (trackHeight - thumbHeight) * this.scrollOffset / maxOffset;

        guiGraphics.fill(barX, trackY, barX + 2, trackY + trackHeight, this.withAlpha(FRAME_RGB, 0.16));
        guiGraphics.fill(barX - 1, thumbY, barX + 3, thumbY + thumbHeight, this.withAlpha(FRAME_RGB, 0.56));
    }

    private void updateAnimations(int mouseX, int mouseY, Layout layout) {
        long now = System.nanoTime();
        double deltaTime = this.lastFrameNanos == 0L ? 1.0 / 60.0 : (now - this.lastFrameNanos) / 1_000_000_000.0;
        this.lastFrameNanos = now;
        deltaTime = Math.max(0.0, Math.min(0.05, deltaTime));

        int tabX = layout.x();

        for (Category tab : Category.values()) {
            int width = this.getTabWidth(tab);
            double target = this.isInside(mouseX, mouseY, tabX, layout.tabY(), width, TAB_HEIGHT) ? 1.0 : 0.0;
            this.tabHover[tab.ordinal()] = this.approach(this.tabHover[tab.ordinal()], target, HOVER_SPEED, deltaTime);
            this.updateIndicatorPid(this.tabIndicator[tab.ordinal()], this.tabIndicatorVelocity[tab.ordinal()], tab == this.category ? IndicatorState.SELECTED : target > 0.0 ? IndicatorState.HOVERED : IndicatorState.NORMAL, deltaTime);
            tabX += width + TAB_GAP;
        }

        for (ConfigEntry entry : this.entries) {
            this.valueHover[entry.index] = this.approach(this.valueHover[entry.index], 0.0, HOVER_SPEED, deltaTime);
            this.resetHover[entry.index] = this.approach(this.resetHover[entry.index], 0.0, HOVER_SPEED, deltaTime);
        }

        for (RowPlacement row : this.getVisibleRows(layout)) {
            ConfigEntry entry = row.entry();
            double valueTarget = this.isInside(mouseX, mouseY, row.valueX(), row.y() + 3, row.valueWidth(), 16) ? 1.0 : 0.0;
            double resetTarget = this.isInside(mouseX, mouseY, row.resetX(), row.y() + 3, RESET_WIDTH, 16) ? 1.0 : 0.0;
            this.valueHover[entry.index] = this.approach(this.valueHover[entry.index], valueTarget, HOVER_SPEED, deltaTime);
            this.resetHover[entry.index] = this.approach(this.resetHover[entry.index], resetTarget, HOVER_SPEED, deltaTime);
        }

        double backTarget = this.isInside(mouseX, mouseY, layout.backX(), layout.backY(), BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT) ? 1.0 : 0.0;
        this.backHover = this.approach(this.backHover, backTarget, HOVER_SPEED, deltaTime);
    }

    private void returnToMainMenu() {
        this.closeGui(true);
        GuiBase.openGui(new GuiBuilderHelperMainMenu());
    }

    private void handleValueClick(ConfigEntry entry) {
        if (entry.type == EntryType.BOOLEAN) {
            entry.value = Boolean.toString(!entry.booleanValue());
            this.editingEntry = null;
            this.listeningKeyEntry = null;
            this.saveConfig();
        } else if (entry.type == EntryType.ENUM) {
            entry.cycleEnum();
            this.editingEntry = null;
            this.listeningKeyEntry = null;
            this.saveConfig();
        } else if (entry.type == EntryType.KEYBIND) {
            this.editingEntry = null;
            this.listeningKeyEntry = entry;
        } else if (entry.type == EntryType.TEXT) {
            this.editingEntry = entry;
            this.listeningKeyEntry = null;
        }
    }

    private List<RowPlacement> getVisibleRows(Layout layout) {
        List<ConfigEntry> categoryEntries = this.getEntriesForCategory();
        List<RowPlacement> rows = new ArrayList<>();
        int visibleRows = this.getVisibleRowCount(layout);
        int resetX = layout.x() + layout.width() - RESET_WIDTH - 12;
        int labelWidth = 0;

        for (ConfigEntry entry : categoryEntries) {
            labelWidth = Math.max(labelWidth, this.getScaledStringWidth(entry.label()));
        }

        int labelToValue = Math.max(86, Math.min(210, labelWidth + 24));
        int valueX = Math.min(layout.x() + labelToValue, resetX - VALUE_WIDTH - 12);
        int rowY = layout.listY() + 8;

        for (int i = 0; i < visibleRows && i + this.scrollOffset < categoryEntries.size(); i++) {
            ConfigEntry entry = categoryEntries.get(i + this.scrollOffset);
            rows.add(new RowPlacement(entry, layout.x() + 8, rowY, layout.width() - 18, valueX, Math.max(80, resetX - valueX - 10), resetX));
            rowY += ROW_HEIGHT + ROW_GAP;
        }

        return rows;
    }

    private List<ConfigEntry> getEntriesForCategory() {
        List<ConfigEntry> list = new ArrayList<>();

        for (ConfigEntry entry : this.entries) {
            if (entry.category == this.category) {
                list.add(entry);
            }
        }

        return list;
    }

    private int getVisibleRowCount(Layout layout) {
        return Math.max(1, (layout.listHeight() - 8) / (ROW_HEIGHT + ROW_GAP));
    }

    private int getMaxScrollOffset(Layout layout) {
        return Math.max(0, this.getEntriesForCategory().size() - this.getVisibleRowCount(layout));
    }

    private int getTabWidth(Category tab) {
        return this.getScaledStringWidth(tab.title()) + 6;
    }

    private void drawText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        this.drawScaledString(guiGraphics, text, x, y, color, false);
    }

    private void drawTextWithShadow(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        this.drawScaledString(guiGraphics, text, x, y, color, true);
    }

    private void drawScaledString(GuiGraphics guiGraphics, String text, int x, int y, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) TEXT_SCALE, (float) TEXT_SCALE, 1.0F);
        guiGraphics.drawString(this.textRenderer, text, (int) Math.round(x / TEXT_SCALE), (int) Math.round(y / TEXT_SCALE), color, shadow);
        guiGraphics.pose().popPose();
    }

    private int getScaledStringWidth(String text) {
        return (int) Math.ceil(this.getStringWidth(text) * TEXT_SCALE);
    }

    private Layout createLayout() {
        int x = SAFE_MARGIN;
        int y = TOP_Y;
        int width = Math.max(260, this.width - SAFE_MARGIN * 2);
        int listY = y + 29;
        int backY = this.height - SAFE_MARGIN - BACK_BUTTON_HEIGHT;
        int backX = x + width - BACK_BUTTON_WIDTH;
        int listHeight = Math.max(80, backY - listY - 8);
        return new Layout(x, y - 16, y, listY, width, listHeight, backX, backY);
    }

    private void createEntries() {
        this.entries.clear();
        this.add(Category.FILES, "litematicPath", "iterablock.gui.settings.option.litematic_path", EntryType.TEXT, BuilderHelperClientConfig.getDefaultLitematicPath().toString());
        this.add(Category.HOTKEYS, "openFilesKey", "iterablock.gui.settings.keybind.open_files", EntryType.KEYBIND, "I");
        this.add(Category.HOTKEYS, "openRadialKey", "iterablock.gui.settings.keybind.open_radial", EntryType.KEYBIND, "U");
        this.add(Category.HOTKEYS, "openMainMenuKey", "iterablock.gui.settings.keybind.open_main_menu", EntryType.KEYBIND, "O");
        this.add(Category.HOTKEYS, "placeProjectionKey", "iterablock.gui.settings.keybind.place_projection", EntryType.KEYBIND, "Y");
        this.add(Category.HOTKEYS, "rotateProjectionKey", "iterablock.gui.settings.keybind.rotate_projection", EntryType.KEYBIND, "R");
        this.add(Category.LITEMATIC, "placementRange", "iterablock.gui.settings.option.placement_range", EntryType.TEXT, "100");
        this.add(Category.LITEMATIC, "linearArrayRenderLimit", "iterablock.gui.settings.option.linear_array_render_limit", EntryType.TEXT, "5");
        this.add(Category.LITEMATIC, "volumeArrayRenderLimit", "iterablock.gui.settings.option.volume_array_render_limit", EntryType.TEXT, "3");
    }

    private void add(Category category, String key, String labelKey, EntryType type, String defaultValue, String... enumValues) {
        this.entries.add(new ConfigEntry(this.entries.size(), category, key, labelKey, type, defaultValue, enumValues));
    }

    private void loadConfig() {
        Properties properties = new Properties();
        Path path = this.getConfigPath();

        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException ignored) {
            }
        }

        for (ConfigEntry entry : this.entries) {
            entry.value = properties.getProperty(entry.key, entry.defaultValue);
            if (entry.type == EntryType.KEYBIND) {
                entry.value = VanillaKeyMappings.getKeyName(entry.key, entry.defaultValue);
            }
        }
    }

    private void saveConfig() {
        Properties properties = new Properties();
        Path path = this.getConfigPath();

        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException ignored) {
            }
        }

        for (ConfigEntry entry : this.entries) {
            properties.setProperty(entry.key, entry.value);
        }

        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "BuilderHelper client settings");
            }
        } catch (IOException ignored) {
        }
    }

    private Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("builderhelper-client.properties");
    }

    private void ensureHoverSize() {
        this.valueHover = new double[this.entries.size()];
        this.resetHover = new double[this.entries.size()];
    }

    private double approach(double current, double target, double speed, double deltaTime) {
        double step = speed * deltaTime;

        if (current < target) {
            return Math.min(target, current + step);
        }

        return Math.max(target, current - step);
    }

    private void updateIndicatorPid(double[] values, double[] velocities, IndicatorState state, double deltaTime) {
        double[] stiffness = {14.0, 24.0, 40.0};
        double[] damping = {6.8, 8.4, 10.0};

        for (int i = 0; i < INDICATOR_SEGMENTS; i++) {
            double target = this.getIndicatorTarget(state, i);
            double error = target - values[i];
            double acceleration = error * stiffness[i] - velocities[i] * damping[i];

            velocities[i] += acceleration * deltaTime;
            values[i] += velocities[i] * deltaTime;

            if (values[i] < 0.0 || values[i] > 1.0) {
                values[i] = Math.max(0.0, Math.min(1.0, values[i]));
                velocities[i] = 0.0;
            }
        }
    }

    private double getIndicatorTarget(IndicatorState state, int segment) {
        return switch (state) {
            case SELECTED, HOVERED -> 1.0;
            case PRESSED -> 0.86;
            case DISABLED -> segment == 0 ? 0.25 : 0.0;
            case NORMAL -> segment == 0 ? 0.45 : 0.0;
        };
    }

    private double easeOutCubic(double value) {
        double inverse = 1.0 - value;
        return 1.0 - inverse * inverse * inverse;
    }

    private int blendRgb(int first, int second, double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        int red = (int) Math.round(((first >> 16) & 0xFF) + (((second >> 16) & 0xFF) - ((first >> 16) & 0xFF)) * t);
        int green = (int) Math.round(((first >> 8) & 0xFF) + (((second >> 8) & 0xFF) - ((first >> 8) & 0xFF)) * t);
        int blue = (int) Math.round((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * t);
        return red << 16 | green << 8 | blue;
    }

    private int withAlpha(int rgb, double alpha) {
        int alphaByte = (int) Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0);
        return alphaByte << 24 | rgb;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isLeftMouseDown() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private enum Category {
        FILES("iterablock.gui.settings.tab.files"),
        HOTKEYS("iterablock.gui.settings.tab.hotkeys"),
        LITEMATIC("iterablock.gui.settings.tab.litematic");

        private final String titleKey;

        Category(String titleKey) {
            this.titleKey = titleKey;
        }

        private String title() {
            return Lang.tr(this.titleKey);
        }
    }

    private enum EntryType {
        BOOLEAN,
        TEXT,
        ENUM,
        KEYBIND
    }

    private enum IndicatorState {
        NORMAL,
        HOVERED,
        SELECTED,
        PRESSED,
        DISABLED
    }

    private static class ConfigEntry {
        private final int index;
        private final Category category;
        private final String key;
        private final String labelKey;
        private final EntryType type;
        private final String defaultValue;
        private final String[] enumValues;
        private String value;

        private ConfigEntry(int index, Category category, String key, String labelKey, EntryType type, String defaultValue, String[] enumValues) {
            this.index = index;
            this.category = category;
            this.key = key;
            this.labelKey = labelKey;
            this.type = type;
            this.defaultValue = defaultValue;
            this.enumValues = enumValues;
            this.value = defaultValue;
        }

        private String label() {
            return Lang.tr(this.labelKey);
        }

        private boolean booleanValue() {
            return Boolean.parseBoolean(this.value);
        }

        private void cycleEnum() {
            if (this.enumValues.length == 0) {
                return;
            }

            int index = 0;

            for (int i = 0; i < this.enumValues.length; i++) {
                if (this.enumValues[i].equals(this.value)) {
                    index = i;
                    break;
                }
            }

            this.value = this.enumValues[(index + 1) % this.enumValues.length];
        }

        private void reset() {
            this.value = this.defaultValue;
        }

        private String displayValue(boolean editing, boolean listening) {
            if (listening) {
                return Lang.tr("iterablock.gui.settings.keybind.listening");
            }

            if (this.type == EntryType.BOOLEAN) {
                return Lang.tr(this.booleanValue() ? "iterablock.gui.settings.value.true" : "iterablock.gui.settings.value.false");
            }

            return editing ? this.value + "_" : this.value;
        }
    }

    private record Layout(int x, int titleY, int tabY, int listY, int width, int listHeight, int backX, int backY) {
    }

    private record RowPlacement(ConfigEntry entry, int x, int y, int width, int valueX, int valueWidth, int resetX) {
    }
}

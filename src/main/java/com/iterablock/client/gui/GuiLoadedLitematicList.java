package com.iterablock.client.gui;

import java.util.List;

import com.iterablock.client.Lang;
import com.iterablock.client.template.LoadedLitematicManager;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiLoadedLitematicList extends GuiBase {
    private static final int SAFE_MARGIN = 22;
    private static final int PANEL_MAX_WIDTH = 260;
    private static final int OUTER_PADDING = 8;
    private static final int ROW_HEIGHT = 18;
    private static final int UNLOAD_WIDTH = 58;
    private static final int BUTTON_WIDTH = 58;
    private static final int BUTTON_HEIGHT = 16;
    private static final double TEXT_SCALE = 0.52;
    private static final int TEXT = 0xFFDDE8E8;
    private static final int MUTED = 0xFF91A0A3;

    private int selectedIndex = -1;
    private int scrollOffset;
    private final boolean returnToMainMenu;

    public GuiLoadedLitematicList() {
        this(false);
    }

    public GuiLoadedLitematicList(boolean returnToMainMenu) {
        this.returnToMainMenu = returnToMainMenu;
        this.setTitle(Lang.tr("iterablock.gui.loaded_list.title"));
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
        List<LoadedLitematicManager.Entry> entries = LoadedLitematicManager.getEntries();

        this.syncSelection(entries, layout);
        this.drawPanel(guiGraphics, layout);
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.loaded_list.title"), layout.x(), layout.titleY(), 0xFFF3EECF, true);
        this.drawRows(guiGraphics, mouseX, mouseY, layout, entries);
        this.drawButtons(guiGraphics, mouseX, mouseY, layout);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        Layout layout = this.createLayout();
        List<LoadedLitematicManager.Entry> entries = LoadedLitematicManager.getEntries();

        if (this.isInside(mouseX, mouseY, layout.listX(), layout.listY(), layout.listWidth(), layout.listHeight())) {
            int index = this.scrollOffset + (mouseY - layout.listY()) / ROW_HEIGHT;

            if (index >= 0 && index < entries.size()) {
                int rowY = layout.listY() + (index - this.scrollOffset) * ROW_HEIGHT;

                if (this.isInside(mouseX, mouseY, layout.unloadX(), rowY + 2, UNLOAD_WIDTH, 14)) {
                    this.unloadEntry(entries.get(index));
                    return true;
                }

                this.selectedIndex = index;
                LoadedLitematicManager.select(entries.get(index));
                return true;
            }
        }

        if (this.isInside(mouseX, mouseY, layout.closeX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            this.returnToMainMenu();
            return true;
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseScrolled(int mouseX, int mouseY, double amount, double amountHorizontal) {
        Layout layout = this.createLayout();

        if (this.isInside(mouseX, mouseY, layout.listX(), layout.listY(), layout.listWidth(), layout.listHeight())) {
            int maxOffset = Math.max(0, LoadedLitematicManager.getEntries().size() - this.getVisibleRowCount(layout));
            this.scrollOffset = Math.max(0, Math.min(maxOffset, this.scrollOffset - (int) Math.signum(amount)));
            return true;
        }

        return super.onMouseScrolled(mouseX, mouseY, amount, amountHorizontal);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.closeGui(true);
            if (this.returnToMainMenu) {
                GuiBase.openGui(new GuiBuilderHelperMainMenu());
            }
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    private void drawPanel(GuiGraphics guiGraphics, Layout layout) {
        int x = layout.x();
        int y = layout.panelY();
        int right = x + layout.width();
        int bottom = y + layout.panelHeight();

        guiGraphics.fill(x, y, right, bottom, 0x7A0B1012);
        guiGraphics.fill(x, y, right, y + 1, 0xBFFFFFFF);
        guiGraphics.fill(x, y, x + 1, bottom, 0x66FFFFFF);
        guiGraphics.fill(right - 1, y, right, bottom, 0x66FFFFFF);
        guiGraphics.fill(x, bottom - 1, right, bottom, 0x66FFFFFF);
        this.drawSimpleButtonBox(guiGraphics, layout.listX(), layout.listY(), layout.listWidth(), layout.listHeight(), false);
    }

    private void drawRows(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout, List<LoadedLitematicManager.Entry> entries) {
        if (entries.isEmpty()) {
            this.drawText(guiGraphics, Lang.tr("iterablock.gui.loaded_list.empty"), layout.listX() + 8, layout.listY() + 8, MUTED, false);
            return;
        }

        int rows = this.getVisibleRowCount(layout);

        for (int i = 0; i < rows && i + this.scrollOffset < entries.size(); i++) {
            int index = i + this.scrollOffset;
            LoadedLitematicManager.Entry entry = entries.get(index);
            int y = layout.listY() + i * ROW_HEIGHT;
            boolean hovered = this.isInside(mouseX, mouseY, layout.listX() + 3, y + 2, layout.listWidth() - 6, ROW_HEIGHT - 4);
            boolean unloadHovered = this.isInside(mouseX, mouseY, layout.unloadX(), y + 2, UNLOAD_WIDTH, 14);
            boolean selected = entry == LoadedLitematicManager.selectedEntry || index == this.selectedIndex;
            int textRight = layout.unloadX() - 8;

            this.drawSimpleButtonBox(guiGraphics, layout.listX() + 3, y + 2, layout.listWidth() - 6, ROW_HEIGHT - 4, selected || hovered);
            this.drawText(guiGraphics, this.truncate(entry.displayName(), Math.max(12, (textRight - layout.listX()) / 5)), layout.listX() + 10, y + 7, TEXT, false);
            this.drawButton(guiGraphics, layout.unloadX(), y + 2, UNLOAD_WIDTH, 14, Lang.tr("iterablock.gui.loaded_list.unload"), unloadHovered);
        }
    }

    private void drawButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        this.drawButton(guiGraphics, layout.closeX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT, Lang.tr("iterablock.gui.button.back"), this.isInside(mouseX, mouseY, layout.closeX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT));
    }

    private void drawButton(GuiGraphics guiGraphics, int x, int y, int width, int height, String label, boolean hovered) {
        this.drawSimpleButtonBox(guiGraphics, x, y, width, height, hovered);
        int labelX = x + Math.max(3, (int) Math.round((width - this.textRenderer.width(label) * TEXT_SCALE) / 2.0));
        int labelY = y + (int) Math.round((height - 8.0 * TEXT_SCALE) / 2.0);
        this.drawText(guiGraphics, label, labelX, labelY, 0xFFFFFFFF, false);
    }

    private void drawSimpleButtonBox(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered) {
        int fill = hovered ? 0xAA5E6666 : 0x8A4A5050;
        int border = hovered ? 0xE8FFFFFF : 0xBFFFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    private void unloadEntry(LoadedLitematicManager.Entry entry) {
        if (entry == null) {
            return;
        }

        LoadedLitematicManager.unload(entry);
        List<LoadedLitematicManager.Entry> updatedEntries = LoadedLitematicManager.getEntries();
        this.selectedIndex = updatedEntries.indexOf(LoadedLitematicManager.selectedEntry);
        this.clampScrollOffset(updatedEntries, this.createLayout());
    }

    private void returnToMainMenu() {
        this.closeGui(true);
        GuiBase.openGui(new GuiBuilderHelperMainMenu());
    }

    private void syncSelection(List<LoadedLitematicManager.Entry> entries, Layout layout) {
        int currentIndex = entries.indexOf(LoadedLitematicManager.selectedEntry);

        if (currentIndex >= 0) {
            this.selectedIndex = currentIndex;
        }

        if (this.selectedIndex >= entries.size()) {
            this.selectedIndex = entries.size() - 1;
        }

        if (entries.isEmpty()) {
            this.selectedIndex = -1;
        }

        this.clampScrollOffset(entries, layout);
    }

    private void clampScrollOffset(List<LoadedLitematicManager.Entry> entries, Layout layout) {
        int maxOffset = Math.max(0, entries.size() - this.getVisibleRowCount(layout));
        this.scrollOffset = Math.max(0, Math.min(maxOffset, this.scrollOffset));
    }

    private Layout createLayout() {
        int horizontalMargin = Math.min(SAFE_MARGIN, Math.max(4, (this.width - 180) / 2));
        int availableWidth = Math.max(1, this.width - horizontalMargin * 2);
        int width = Math.min(availableWidth, PANEL_MAX_WIDTH);
        int x = (this.width - width) / 2;
        int titleY = Math.max(10, SAFE_MARGIN);
        int panelY = titleY + 20;
        int desiredPanelHeight = OUTER_PADDING * 2 + ROW_HEIGHT * 6;
        int maximumPanelHeight = Math.max(ROW_HEIGHT + OUTER_PADDING * 2,
                this.height - SAFE_MARGIN - BUTTON_HEIGHT - panelY - 10);
        int panelHeight = Math.min(desiredPanelHeight, maximumPanelHeight);
        int buttonY = Math.min(this.height - SAFE_MARGIN - BUTTON_HEIGHT, panelY + panelHeight + 8);
        int listX = x + OUTER_PADDING;
        int listY = panelY + OUTER_PADDING;
        int listWidth = width - OUTER_PADDING * 2;
        int listHeight = Math.max(ROW_HEIGHT, panelHeight - OUTER_PADDING * 2);
        int closeX = x + width - BUTTON_WIDTH;
        int unloadX = listX + listWidth - UNLOAD_WIDTH - 3;
        return new Layout(x, titleY, panelY, width, panelHeight, listX, listY, listWidth, listHeight, buttonY, unloadX, closeX);
    }

    private int getVisibleRowCount(Layout layout) {
        return Math.max(1, layout.listHeight() / ROW_HEIGHT);
    }

    private void drawText(GuiGraphics guiGraphics, String text, double x, double y, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) TEXT_SCALE, (float) TEXT_SCALE, 1.0F);
        guiGraphics.drawString(this.textRenderer, text, (int) Math.round(x / TEXT_SCALE), (int) Math.round(y / TEXT_SCALE), color, shadow);
        guiGraphics.pose().popPose();
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, Math.max(0, maxLength - 1)) + "...";
    }

    private record Layout(int x, int titleY, int panelY, int width, int panelHeight, int listX, int listY, int listWidth, int listHeight, int buttonY, int unloadX, int closeX) {
    }
}

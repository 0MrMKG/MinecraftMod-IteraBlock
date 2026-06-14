package com.iterablock.client.gui;

import java.util.List;

import com.iterablock.client.Lang;
import com.iterablock.client.template.LoadedLitematicManager;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiLoadedLitematicList extends GuiBase {
    private static final int SAFE_MARGIN = 18;
    private static final int ROW_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 58;
    private static final int BUTTON_HEIGHT = 14;
    private static final double TEXT_SCALE = 0.62;
    private static final int ACCENT = 0xFFE1C76A;
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
        guiGraphics.fill(0, 0, this.width, this.height, 0x78000000);
    }

    @Override
    protected void drawTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void drawContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Layout layout = this.createLayout();
        List<LoadedLitematicManager.Entry> entries = LoadedLitematicManager.getEntries();

        this.syncSelection(entries, layout);
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.loaded_list.title"), layout.x(), layout.titleY(), 0xFFF3EECF, true);
        this.drawPanel(guiGraphics, layout);
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

        if (this.isInside(mouseX, mouseY, layout.x(), layout.listY(), layout.width(), layout.listHeight())) {
            int index = this.scrollOffset + (mouseY - layout.listY()) / ROW_HEIGHT;

            if (index >= 0 && index < entries.size()) {
                this.selectedIndex = index;
                LoadedLitematicManager.select(entries.get(index));
                return true;
            }
        }

        if (this.isInside(mouseX, mouseY, layout.unloadX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            this.unloadSelected(entries);
            return true;
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

        if (this.isInside(mouseX, mouseY, layout.x(), layout.listY(), layout.width(), layout.listHeight())) {
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
        guiGraphics.fill(layout.x(), layout.panelY(), layout.x() + layout.width(), layout.panelY() + layout.panelHeight(), 0x7A0B1012);
        guiGraphics.fill(layout.x(), layout.panelY(), layout.x() + layout.width(), layout.panelY() + 1, 0x885B6969);
        guiGraphics.fill(layout.x(), layout.listY() - 1, layout.x() + layout.width(), layout.listY(), 0x553D5558);
        guiGraphics.fill(layout.x(), layout.listY() - 1, layout.x() + 24, layout.listY() + 1, ACCENT);
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.loaded_list.header"), layout.x() + 7, layout.panelY() + 7, TEXT, false);
    }

    private void drawRows(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout, List<LoadedLitematicManager.Entry> entries) {
        if (entries.isEmpty()) {
            this.drawText(guiGraphics, Lang.tr("iterablock.gui.loaded_list.empty"), layout.x() + 8, layout.listY() + 8, MUTED, false);
            return;
        }

        int rows = this.getVisibleRowCount(layout);

        for (int i = 0; i < rows && i + this.scrollOffset < entries.size(); i++) {
            int index = i + this.scrollOffset;
            LoadedLitematicManager.Entry entry = entries.get(index);
            int y = layout.listY() + i * ROW_HEIGHT;
            boolean hovered = this.isInside(mouseX, mouseY, layout.x() + 4, y, layout.width() - 8, ROW_HEIGHT);
            boolean selected = entry == LoadedLitematicManager.selectedEntry || index == this.selectedIndex;
            int fill = selected ? 0x664D5C41 : hovered ? 0x44273033 : 0x22101719;

            guiGraphics.fill(layout.x() + 4, y, layout.x() + layout.width() - 4, y + ROW_HEIGHT - 1, fill);

            if (selected) {
                guiGraphics.fill(layout.x() + 4, y, layout.x() + 6, y + ROW_HEIGHT - 1, ACCENT);
            }

            this.drawText(guiGraphics, this.truncate(entry.displayName(), Math.max(12, layout.width() / 5)), layout.x() + 12, y + 5, selected ? 0xFFFFF1B0 : TEXT, false);
        }
    }

    private void drawButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        this.drawButton(guiGraphics, layout.unloadX(), layout.buttonY(), Lang.tr("iterablock.gui.loaded_list.unload"), this.isInside(mouseX, mouseY, layout.unloadX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT));
        this.drawButton(guiGraphics, layout.closeX(), layout.buttonY(), Lang.tr("iterablock.gui.button.back"), this.isInside(mouseX, mouseY, layout.closeX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT));
    }

    private void drawButton(GuiGraphics guiGraphics, int x, int y, String label, boolean hovered) {
        this.drawSimpleButtonBox(guiGraphics, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, hovered);
        guiGraphics.fill(x + 3, y + BUTTON_HEIGHT - 2, x + (hovered ? BUTTON_WIDTH - 4 : 10), y + BUTTON_HEIGHT, ACCENT);
        this.drawText(guiGraphics, label, x + 6, y + 4, hovered ? 0xFFFFF1B0 : TEXT, false);
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

    private void unloadSelected(List<LoadedLitematicManager.Entry> entries) {
        LoadedLitematicManager.Entry entry = this.getSelectedEntry(entries);

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

    private LoadedLitematicManager.Entry getSelectedEntry(List<LoadedLitematicManager.Entry> entries) {
        if (this.selectedIndex >= 0 && this.selectedIndex < entries.size()) {
            return entries.get(this.selectedIndex);
        }

        return LoadedLitematicManager.selectedEntry;
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
        int width = Math.min(this.width - SAFE_MARGIN * 2, 360);
        int x = (this.width - width) / 2;
        int titleY = Math.max(10, SAFE_MARGIN);
        int panelY = titleY + 20;
        int buttonY = this.height - SAFE_MARGIN - BUTTON_HEIGHT;
        int panelHeight = Math.max(80, buttonY - panelY - 8);
        int listY = panelY + 20;
        int listHeight = Math.max(40, panelY + panelHeight - listY - 8);
        int closeX = x + width - BUTTON_WIDTH;
        int unloadX = closeX - BUTTON_WIDTH - 7;
        return new Layout(x, titleY, panelY, width, panelHeight, listY, listHeight, buttonY, unloadX, closeX);
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

    private record Layout(int x, int titleY, int panelY, int width, int panelHeight, int listY, int listHeight, int buttonY, int unloadX, int closeX) {
    }
}

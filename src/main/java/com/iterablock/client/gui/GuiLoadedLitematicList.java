package com.iterablock.client.gui;

import java.util.List;

import com.iterablock.client.Lang;
import com.iterablock.client.gui.settings.GuiSettingsControls;
import com.iterablock.client.gui.settings.SettingsMenuCommonRenderer;
import com.iterablock.client.template.LoadedLitematicManager;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiLoadedLitematicList extends GuiBase {
    private static final int SAFE_MARGIN = 22;
    private static final int PANEL_MAX_WIDTH = 260;
    private static final int SECTION_HEIGHT = 18;
    private static final int LIST_PADDING = 6;
    private static final int ROW_HEIGHT = 20;
    private static final int UNLOAD_WIDTH = 42;
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
        SettingsMenuCommonRenderer.drawBackground(guiGraphics, this.width, this.height);
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
        SettingsMenuCommonRenderer.drawTitle(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.loaded_list.title"), layout.x(), layout.titleY());
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
        SettingsMenuCommonRenderer.drawPanel(guiGraphics, layout.x(), layout.panelY(), layout.width(), layout.panelHeight(), layout.bodyY());
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.loaded_list.header"), layout.x() + 8, layout.panelY() + 5, 0xFFF3EECF, false);
    }

    private void drawRows(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout, List<LoadedLitematicManager.Entry> entries) {
        if (entries.isEmpty()) {
            String emptyText = Lang.tr("iterablock.gui.loaded_list.empty");
            int emptyX = layout.listX() + Math.max(0, (layout.listWidth() - (int) Math.ceil(this.textRenderer.width(emptyText) * TEXT_SCALE)) / 2);
            this.drawText(guiGraphics, emptyText, emptyX, layout.listY() + Math.max(8, layout.listHeight() / 2 - 4), MUTED, false);
            return;
        }

        int rows = this.getVisibleRowCount(layout);

        for (int i = 0; i < rows && i + this.scrollOffset < entries.size(); i++) {
            int index = i + this.scrollOffset;
            LoadedLitematicManager.Entry entry = entries.get(index);
            int y = layout.listY() + i * ROW_HEIGHT;
            boolean hovered = this.isInside(mouseX, mouseY, layout.listX(), y + 2, layout.listWidth(), ROW_HEIGHT - 4);
            boolean unloadHovered = this.isInside(mouseX, mouseY, layout.unloadX(), y + 2, UNLOAD_WIDTH, 14);
            boolean selected = entry == LoadedLitematicManager.selectedEntry || index == this.selectedIndex;
            int nameWidth = layout.unloadX() - layout.listX() - 6;
            int textColor = selected ? 0xFFFFF1B0 : TEXT;

            if (selected) {
                guiGraphics.fill(layout.listX(), y + 3, layout.listX() + 2, y + ROW_HEIGHT - 3, 0xFFF3C6D3);
            }
            GuiSettingsControls.drawValueField(guiGraphics, this.textRenderer,
                    this.fitText(entry.displayName(), Math.max(1, nameWidth - 8)),
                    layout.listX() + 4, y + 2, Math.max(1, nameWidth - 4), 14,
                    selected ? 1.0 : hovered ? 0.65 : 0.0, textColor);
            GuiSettingsControls.drawButton(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.loaded_list.unload"),
                    layout.unloadX(), y + 2, UNLOAD_WIDTH, 14, unloadHovered ? 1.0 : 0.0);
        }

        this.drawScrollbar(guiGraphics, layout, entries.size());
    }

    private void drawButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        GuiSettingsControls.drawButton(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.button.back"),
                layout.closeX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT,
                this.isInside(mouseX, mouseY, layout.closeX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT) ? 1.0 : 0.0);
    }

    private void drawScrollbar(GuiGraphics guiGraphics, Layout layout, int totalRows) {
        int visibleRows = this.getVisibleRowCount(layout);
        if (totalRows <= visibleRows) {
            return;
        }

        int trackX = layout.x() + layout.width() - 5;
        int trackY = layout.listY() + 3;
        int trackHeight = Math.max(1, layout.listHeight() - 6);
        int thumbHeight = Math.max(14, trackHeight * visibleRows / totalRows);
        int maxOffset = Math.max(1, totalRows - visibleRows);
        int thumbY = trackY + (trackHeight - thumbHeight) * this.scrollOffset / maxOffset;
        guiGraphics.fill(trackX, trackY, trackX + 1, trackY + trackHeight, 0x556B777A);
        guiGraphics.fill(trackX - 1, thumbY, trackX + 2, thumbY + thumbHeight, 0xB0B0B8BA);
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
        int desiredPanelHeight = SECTION_HEIGHT + LIST_PADDING * 2 + ROW_HEIGHT * 6;
        int maximumPanelHeight = Math.max(SECTION_HEIGHT + ROW_HEIGHT + LIST_PADDING * 2,
                this.height - SAFE_MARGIN - BUTTON_HEIGHT - panelY - 10);
        int panelHeight = Math.min(desiredPanelHeight, maximumPanelHeight);
        int buttonY = Math.min(this.height - SAFE_MARGIN - BUTTON_HEIGHT, panelY + panelHeight + 8);
        int bodyY = panelY + SECTION_HEIGHT;
        int listX = x + LIST_PADDING;
        int listY = bodyY + LIST_PADDING;
        int listWidth = width - LIST_PADDING * 2;
        int listHeight = Math.max(ROW_HEIGHT, panelHeight - SECTION_HEIGHT - LIST_PADDING * 2);
        int closeX = x + width - BUTTON_WIDTH;
        int unloadX = listX + listWidth - UNLOAD_WIDTH;
        return new Layout(x, titleY, panelY, width, panelHeight, bodyY, listX, listY, listWidth, listHeight, buttonY, unloadX, closeX);
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

    private String fitText(String text, int maxWidth) {
        if (this.textRenderer.width(text) * TEXT_SCALE <= maxWidth) {
            return text;
        }

        String suffix = "...";
        String result = text;
        while (!result.isEmpty() && (this.textRenderer.width(result) + this.textRenderer.width(suffix)) * TEXT_SCALE > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isEmpty() ? suffix : result + suffix;
    }

    private record Layout(int x, int titleY, int panelY, int width, int panelHeight, int bodyY,
                          int listX, int listY, int listWidth, int listHeight, int buttonY, int unloadX, int closeX) {
    }
}

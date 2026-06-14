package com.iterablock.client.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.litematica.LitematicaSchematicInfo;
import com.iterablock.client.litematica.LitematicaSchematicReader;
import com.iterablock.client.hotkeys.VanillaKeyMappings;
import com.iterablock.client.template.LoadedLitematicManager;
import com.iterablock.client.template.TemplateSelection;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

public class GuiIteraBlockMainMenu extends GuiBase {
    private static final int SAFE_MARGIN = 18;
    private static final int TOP_HEIGHT = 58;
    private static final int ROW_HEIGHT = 14;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BOTTOM_BUTTON_WIDTH = 52;
    private static final double TEXT_SCALE = 0.62;
    private static final double TITLE_SCALE = 0.72;
    private static final double HOVER_SPEED = 9.0;
    private static final int ACCENT = 0xFFE1C76A;
    private static final int TEXT = 0xFFDDE8E8;
    private static final int MUTED = 0xFF91A0A3;

    private final List<BrowserEntry> entries = new ArrayList<>();
    private final double[] buttonHover = new double[ButtonAction.values().length];
    private double[] rowHover = new double[0];
    private Path rootDirectory;
    private Path currentDirectory;
    private Path selected;
    private LitematicaSchematicInfo selectedLitematicaInfo;
    private String selectedParseError;
    private String searchText = "";
    private String statusText = "";
    private boolean searchFocused;
    private boolean draggingScrollbar;
    private int scrollOffset;
    private long lastFrameNanos;
    private final boolean returnToMainMenu;

    public GuiIteraBlockMainMenu() {
        this(false);
    }

    public GuiIteraBlockMainMenu(boolean returnToMainMenu) {
        this.returnToMainMenu = returnToMainMenu;
        this.setTitle(Lang.tr("iterablock.gui.file_browser.title"));
        this.rootDirectory = BuilderHelperClientConfig.getLitematicPath();
        this.currentDirectory = this.rootDirectory;
        this.refreshDirectory();
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
        this.updateScrollbarDrag(mouseY, layout);
        this.updateAnimations(mouseX, mouseY, layout);

        this.drawText(guiGraphics, Lang.tr("iterablock.gui.file_browser.title"), layout.x(), layout.y(), 0xFFF3EECF, true, TITLE_SCALE);
        this.drawPathBar(guiGraphics, layout);
        this.drawTopControls(guiGraphics, mouseX, mouseY, layout);
        this.drawPanels(guiGraphics, mouseX, mouseY, layout);
        this.drawBottomButtons(guiGraphics, mouseX, mouseY, layout);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        Layout layout = this.createLayout();

        if (this.handleButtonClick(mouseX, mouseY, layout)) {
            return true;
        }

        if (this.isInsideScrollbar(mouseX, mouseY, layout)) {
            this.searchFocused = false;
            this.draggingScrollbar = true;
            this.setScrollFromMouseY(mouseY, layout);
            return true;
        }

        if (this.isInside(mouseX, mouseY, layout.searchX(), layout.controlY(), layout.searchWidth(), BUTTON_HEIGHT)) {
            this.searchFocused = true;
            return true;
        }

        this.searchFocused = false;

        if (this.isInside(mouseX, mouseY, layout.listX(), layout.listY(), layout.listWidth(), layout.listHeight())) {
            int index = this.getEntryIndexAtVisibleRow((mouseY - layout.listY()) / ROW_HEIGHT, layout);

            if (index >= 0 && index < this.entries.size()) {
                this.openEntry(this.entries.get(index));
                return true;
            }
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseScrolled(int mouseX, int mouseY, double amount, double amountHorizontal) {
        Layout layout = this.createLayout();

        if (this.isInsideBrowserScrollArea(mouseX, mouseY, layout)) {
            this.scrollRows((int) -Math.signum(amount) * 3, layout);
            return true;
        }

        return super.onMouseScrolled(mouseX, mouseY, amount, amountHorizontal);
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }

        return super.onMouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (this.searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !this.searchText.isEmpty()) {
                this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
                this.refreshDirectory();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                this.searchText = "";
                this.refreshDirectory();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.searchFocused = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || (!this.searchFocused && VanillaKeyMappings.matchesOpenFiles(keyCode, scanCode))) {
            if (this.searchFocused) {
                this.searchFocused = false;
                return true;
            }

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
        if (this.searchFocused && character >= 32 && character != 127 && this.searchText.length() < 64) {
            this.searchText += character;
            this.refreshDirectory();
            return true;
        }

        return super.onCharTyped(character, modifiers);
    }

    private void drawPathBar(GuiGraphics guiGraphics, Layout layout) {
        int y = layout.y() + 13;
        guiGraphics.fill(layout.x(), y, layout.x() + layout.width(), y + 12, 0x6611171A);
        guiGraphics.fill(layout.x(), y + 11, layout.x() + layout.width(), y + 12, 0x663D5558);
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.file_browser.path", this.truncateMiddle(this.formatPath(this.currentDirectory), 108)), layout.x() + 5, y + 3, 0xFFB8DCE8, false, TEXT_SCALE);
    }

    private void drawTopControls(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        int border = this.searchFocused ? 0xCCB8DCE8 : 0x884B5A5C;
        guiGraphics.fill(layout.searchX() - 1, layout.controlY() - 1, layout.searchX() + layout.searchWidth() + 1, layout.controlY() + BUTTON_HEIGHT + 1, border);
        guiGraphics.fill(layout.searchX(), layout.controlY(), layout.searchX() + layout.searchWidth(), layout.controlY() + BUTTON_HEIGHT, 0xAA101719);
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.file_browser.search"), layout.x(), layout.controlY() + 4, TEXT, false, TEXT_SCALE);
        this.drawText(guiGraphics, this.searchText + this.getCaret(), layout.searchX() + 5, layout.controlY() + 4, 0xFFEAFBFF, false, TEXT_SCALE);
    }

    private void drawPanels(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        this.drawPanel(guiGraphics, layout.listX(), layout.listY() - 15, layout.listWidth(), layout.listHeight() + 15, Lang.tr("iterablock.gui.file_browser.panel.files"));
        this.drawPanel(guiGraphics, layout.detailX(), layout.listY() - 15, layout.detailWidth(), layout.listHeight() + 15, Lang.tr("iterablock.gui.file_browser.panel.details"));
        this.drawEntries(guiGraphics, mouseX, mouseY, layout);
        this.drawDetails(guiGraphics, layout);
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, String title) {
        guiGraphics.fill(x, y, x + width, y + height, 0x7A0B1012);
        guiGraphics.fill(x, y, x + width, y + 1, 0x885B6969);
        guiGraphics.fill(x, y + 13, x + width, y + 14, 0x553D5558);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0x66050607);
        guiGraphics.fill(x, y + 13, x + 22, y + 15, ACCENT);
        this.drawText(guiGraphics, title, x + 6, y + 4, 0xFFF3EECF, false, TEXT_SCALE);
    }

    private void drawEntries(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        if (this.entries.isEmpty()) {
            this.drawText(guiGraphics, Lang.tr("iterablock.gui.file_browser.no_matches"), layout.listX() + 8, layout.listY() + 8, MUTED, false, TEXT_SCALE);
            return;
        }

        int visibleRows = this.getVisibleRowCount(layout);

        for (int i = 0; i < visibleRows; i++) {
            int entryIndex = this.getEntryIndexAtVisibleRow(i, layout);
            if (entryIndex < 0 || entryIndex >= this.entries.size()) {
                continue;
            }

            BrowserEntry entry = this.entries.get(entryIndex);
            int rowY = layout.listY() + i * ROW_HEIGHT;
            double hover = this.easeOutCubic(this.rowHover[entryIndex]);
            boolean selectedEntry = entry.path().equals(this.selected);
            int fill = selectedEntry ? 0x664D5C41 : this.withAlpha(this.blendRgb(0x101719, 0x273033, hover), 0.22 + hover * 0.18);

            guiGraphics.fill(layout.listX() + 4, rowY, layout.listX() + layout.listWidth() - 6, rowY + ROW_HEIGHT - 1, fill);
            if (selectedEntry) {
                guiGraphics.fill(layout.listX() + 4, rowY, layout.listX() + 6, rowY + ROW_HEIGHT - 1, ACCENT);
            }

            this.drawEntryIcon(guiGraphics, entry, layout.listX() + 10, rowY + 4, selectedEntry || hover > 0.02);
            this.drawText(guiGraphics, this.truncate(entry.name(), Math.max(10, (layout.listWidth() - 42) / 4)), layout.listX() + 24, rowY + 4, entry.color(selectedEntry, hover), false, TEXT_SCALE);
        }

        this.drawScrollbar(guiGraphics, layout);
    }

    private void drawEntryIcon(GuiGraphics guiGraphics, BrowserEntry entry, int x, int y, boolean active) {
        GuiTextures.drawIcon(guiGraphics, this.getEntryIconName(entry), x - 1, y - 2, 11);
    }

    private String getEntryIconName(BrowserEntry entry) {
        if (entry.type() == EntryType.PARENT) {
            return "parent";
        }

        if (entry.type() == EntryType.DIRECTORY) {
            return "folder";
        }

        String name = entry.path().getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".litematic") || name.endsWith(".litematica")) {
            return "file_litematic";
        }

        if (name.endsWith(".nbt")) {
            return "file_nbt";
        }

        return "file_generic";
    }

    private void drawDetails(GuiGraphics guiGraphics, Layout layout) {
        int x = layout.detailX() + 8;
        int y = layout.listY() + 4;

        if (this.selected == null) {
            this.drawText(guiGraphics, Lang.tr("iterablock.gui.file_browser.selected_none"), x, y, MUTED, false, TEXT_SCALE);
            this.drawPreviewPlaceholder(guiGraphics, layout, Lang.tr("iterablock.gui.file_browser.preview.empty"));
            return;
        }

        LitematicaSchematicInfo.Metadata metadata = this.selectedLitematicaInfo != null ? this.selectedLitematicaInfo.metadata() : null;
        this.drawDetailLine(guiGraphics, x, y, Lang.tr("iterablock.gui.file_browser.detail.name"), this.getSelectedDisplayName());
        this.drawDetailLine(guiGraphics, x, y + 14, Lang.tr("iterablock.gui.file_browser.detail.author"), metadata == null || metadata.author().isBlank() ? "-" : metadata.author());
        this.drawDetailLine(guiGraphics, x, y + 28, Lang.tr("iterablock.gui.file_browser.detail.regions"), metadata == null ? "-" : Integer.toString(metadata.regionCount()));
        this.drawDetailLine(guiGraphics, x, y + 42, Lang.tr("iterablock.gui.file_browser.detail.size"), metadata == null ? "-" : this.formatBlockPos(metadata.enclosingSize()));
        this.drawDetailLine(guiGraphics, x, y + 56, Lang.tr("iterablock.gui.file_browser.detail.path"), this.truncateMiddle(this.formatPath(this.selected), Math.max(28, layout.detailWidth() / 4)));

        if (this.selectedParseError != null) {
            this.drawText(guiGraphics, Lang.tr("iterablock.gui.file_browser.parse_error", this.selectedParseError), x, y + 72, 0xFFFF8888, false, TEXT_SCALE);
        } else if (!this.statusText.isEmpty()) {
            this.drawText(guiGraphics, this.statusText, x, y + 72, 0xFFB8DCE8, false, TEXT_SCALE);
        }

        this.drawPreviewPlaceholder(guiGraphics, layout, Lang.tr("iterablock.gui.file_browser.preview.placeholder"));
    }

    private void drawDetailLine(GuiGraphics guiGraphics, int x, int y, String name, String value) {
        this.drawText(guiGraphics, name, x, y, MUTED, false, TEXT_SCALE);
        this.drawText(guiGraphics, value, x + 48, y, TEXT, false, TEXT_SCALE);
    }

    private void drawPreviewPlaceholder(GuiGraphics guiGraphics, Layout layout, String text) {
        int previewX = layout.detailX() + 8;
        int previewY = layout.listY() + Math.max(82, (int) Math.round(layout.listHeight() * 0.38));
        int previewWidth = layout.detailWidth() - 16;
        int previewHeight = layout.listY() + layout.listHeight() - previewY - 8;

        if (previewHeight < 32) {
            return;
        }

        guiGraphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0x55101719);
        guiGraphics.fill(previewX, previewY, previewX + previewWidth, previewY + 1, 0x665B6969);
        guiGraphics.fill(previewX, previewY + previewHeight - 1, previewX + previewWidth, previewY + previewHeight, 0x66050607);
        this.drawText(guiGraphics, text, previewX + 8, previewY + 8, MUTED, false, TEXT_SCALE);
    }

    private void drawScrollbar(GuiGraphics guiGraphics, Layout layout) {
        int visibleRows = this.getScrollableVisibleRows(layout);
        int scrollableEntries = Math.max(0, this.entries.size() - this.getScrollableStartIndex());
        int maxOffset = this.getMaxScrollOffset(layout);

        if (maxOffset <= 0) {
            return;
        }

        int barX = this.getScrollbarX(layout);
        int trackY = layout.listY() + 3;
        int trackHeight = layout.listHeight() - 6;
        int thumbHeight = Math.max(16, trackHeight * visibleRows / Math.max(1, scrollableEntries));
        int thumbY = trackY + (trackHeight - thumbHeight) * this.scrollOffset / maxOffset;

        guiGraphics.fill(barX - 2, trackY, barX + 4, trackY + trackHeight, 0x33151A1D);
        guiGraphics.fill(barX, trackY, barX + 2, trackY + trackHeight, 0x66151A1D);
        guiGraphics.fill(barX - 1, thumbY, barX + 3, thumbY + thumbHeight, this.draggingScrollbar ? 0xDDCFEAFF : 0xAA6F8D78);
    }

    private void drawBottomButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        for (SmallButton button : this.getBottomButtons(layout)) {
            this.drawIndustrialButton(guiGraphics, button, mouseX, mouseY, false);
        }
    }

    private void drawIndustrialButton(GuiGraphics guiGraphics, SmallButton button, int mouseX, int mouseY, boolean compact) {
        double hover = this.easeOutCubic(this.buttonHover[button.action().ordinal()]);
        int offset = compact ? 0 : (int) Math.round(hover * 3.0);
        int x = button.x() + offset;
        int lineWidth = Math.max(4, (int) Math.round((button.width() - 6) * hover));

        this.drawSimpleButtonBox(guiGraphics, x, button.y(), button.width(), button.height(), hover);
        guiGraphics.fill(x + 3, button.y() + button.height() - 2, x + 3 + lineWidth, button.y() + button.height(), ACCENT);
        this.drawText(guiGraphics, button.label(), x + 6, button.y() + 4, hover > 0.02 ? 0xFFFFF1B0 : TEXT, false, TEXT_SCALE);
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

    private void updateAnimations(int mouseX, int mouseY, Layout layout) {
        long now = System.nanoTime();
        double deltaTime = this.lastFrameNanos == 0L ? 1.0 / 60.0 : (now - this.lastFrameNanos) / 1_000_000_000.0;
        this.lastFrameNanos = now;
        deltaTime = Math.max(0.0, Math.min(0.05, deltaTime));

        this.ensureRowHoverSize();

        for (int i = 0; i < this.rowHover.length; i++) {
            int visibleIndex = this.getVisibleRowForEntryIndex(i, layout);
            boolean hovered = visibleIndex >= 0 && visibleIndex < this.getVisibleRowCount(layout)
                    && this.isInside(mouseX, mouseY, layout.listX() + 4, layout.listY() + visibleIndex * ROW_HEIGHT, layout.listWidth() - 10, ROW_HEIGHT);
            this.rowHover[i] = this.approach(this.rowHover[i], hovered ? 1.0 : 0.0, HOVER_SPEED, deltaTime);
        }

        for (ButtonAction action : ButtonAction.values()) {
            this.buttonHover[action.ordinal()] = this.approach(this.buttonHover[action.ordinal()], 0.0, HOVER_SPEED, deltaTime);
        }

        for (SmallButton button : this.getAllButtons(layout)) {
            double target = button.contains(mouseX, mouseY) ? 1.0 : 0.0;
            int index = button.action().ordinal();
            this.buttonHover[index] = Math.max(this.buttonHover[index], this.approach(this.buttonHover[index], target, HOVER_SPEED, deltaTime));
        }
    }

    private boolean handleButtonClick(int mouseX, int mouseY, Layout layout) {
        for (SmallButton button : this.getBottomButtons(layout)) {
            if (!button.contains(mouseX, mouseY)) {
                continue;
            }

            this.searchFocused = false;
            switch (button.action()) {
                case HOME -> this.goHome();
                case RELOAD -> this.refreshDirectory();
                case LOAD -> this.loadSelected();
                case CANCEL -> this.returnToMainMenu();
            }

            return true;
        }

        return false;
    }

    private void refreshDirectory() {
        this.entries.clear();
        this.statusText = "";
        this.draggingScrollbar = false;

        try {
            Files.createDirectories(this.rootDirectory);

            if (!Files.isDirectory(this.currentDirectory)) {
                this.currentDirectory = this.rootDirectory;
            }

            if (!this.currentDirectory.equals(this.rootDirectory)) {
                Path parent = this.currentDirectory.getParent();

                if (parent != null && parent.startsWith(this.rootDirectory)) {
                    this.entries.add(new BrowserEntry(parent, EntryType.PARENT));
                }
            }

            List<BrowserEntry> children = new ArrayList<>();
            try (Stream<Path> stream = Files.list(this.currentDirectory)) {
                stream.filter(this::shouldShowEntry)
                        .map(path -> new BrowserEntry(path, Files.isDirectory(path) ? EntryType.DIRECTORY : EntryType.FILE))
                        .sorted(Comparator.comparing(BrowserEntry::sortGroup).thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)))
                        .forEach(children::add);
            }

            this.entries.addAll(children);
        } catch (IOException e) {
            this.statusText = Lang.tr("iterablock.gui.file_browser.read_error", e.getMessage());
        }

        if (this.selected != null && (this.selected.getParent() == null || !this.selected.getParent().equals(this.currentDirectory))) {
            this.clearSelected();
        }

        int maxOffset = this.getMaxScrollOffset(this.createLayout());
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));
        this.ensureRowHoverSize();
    }

    private void scrollRows(int rows, Layout layout) {
        int maxOffset = this.getMaxScrollOffset(layout);
        this.scrollOffset = Math.max(0, Math.min(maxOffset, this.scrollOffset + rows));
    }

    private boolean isInsideBrowserScrollArea(int mouseX, int mouseY, Layout layout) {
        return this.isInside(mouseX, mouseY, layout.x(), layout.listY(), layout.width(), layout.listHeight());
    }

    private boolean isInsideScrollbar(int mouseX, int mouseY, Layout layout) {
        return this.getMaxScrollOffset(layout) > 0
                && this.isInside(mouseX, mouseY, this.getScrollbarX(layout) - 3, layout.listY(), 10, layout.listHeight());
    }

    private int getScrollbarX(Layout layout) {
        return layout.listX() + layout.listWidth() - 5;
    }

    private void setScrollFromMouseY(int mouseY, Layout layout) {
        int visibleRows = this.getScrollableVisibleRows(layout);
        int maxOffset = this.getMaxScrollOffset(layout);

        if (maxOffset <= 0) {
            this.scrollOffset = 0;
            return;
        }

        int trackY = layout.listY() + 3;
        int trackHeight = layout.listHeight() - 6;
        int scrollableEntries = Math.max(0, this.entries.size() - this.getScrollableStartIndex());
        int thumbHeight = Math.max(16, trackHeight * visibleRows / Math.max(1, scrollableEntries));
        int usableHeight = Math.max(1, trackHeight - thumbHeight);
        int relativeY = Math.max(0, Math.min(usableHeight, mouseY - trackY - thumbHeight / 2));
        this.scrollOffset = Math.max(0, Math.min(maxOffset, Math.round(relativeY * maxOffset / (float) usableHeight)));
    }

    private boolean hasPinnedParentEntry() {
        return !this.entries.isEmpty() && this.entries.get(0).type() == EntryType.PARENT;
    }

    private int getScrollableStartIndex() {
        return this.hasPinnedParentEntry() ? 1 : 0;
    }

    private int getScrollableVisibleRows(Layout layout) {
        return Math.max(0, this.getVisibleRowCount(layout) - this.getScrollableStartIndex());
    }

    private int getMaxScrollOffset(Layout layout) {
        int scrollableEntries = Math.max(0, this.entries.size() - this.getScrollableStartIndex());
        return Math.max(0, scrollableEntries - this.getScrollableVisibleRows(layout));
    }

    private int getEntryIndexAtVisibleRow(int visibleRow, Layout layout) {
        if (visibleRow < 0 || visibleRow >= this.getVisibleRowCount(layout)) {
            return -1;
        }

        if (this.hasPinnedParentEntry()) {
            if (visibleRow == 0) {
                return 0;
            }

            return this.getScrollableStartIndex() + this.scrollOffset + visibleRow - 1;
        }

        return this.scrollOffset + visibleRow;
    }

    private int getVisibleRowForEntryIndex(int entryIndex, Layout layout) {
        if (this.hasPinnedParentEntry()) {
            if (entryIndex == 0) {
                return 0;
            }

            int visibleRow = entryIndex - this.getScrollableStartIndex() - this.scrollOffset + 1;
            return visibleRow >= 1 && visibleRow < this.getVisibleRowCount(layout) ? visibleRow : -1;
        }

        int visibleRow = entryIndex - this.scrollOffset;
        return visibleRow >= 0 && visibleRow < this.getVisibleRowCount(layout) ? visibleRow : -1;
    }

    private void updateScrollbarDrag(int mouseY, Layout layout) {
        if (!this.draggingScrollbar) {
            return;
        }

        long window = Minecraft.getInstance().getWindow().getWindow();

        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            this.draggingScrollbar = false;
            return;
        }

        this.setScrollFromMouseY(mouseY, layout);
    }

    private boolean shouldShowEntry(Path path) {
        String name = path.getFileName().toString();
        String query = this.searchText.toLowerCase(Locale.ROOT);

        if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)) {
            return false;
        }

        return Files.isDirectory(path) || isTemplateFile(path);
    }

    private void openEntry(BrowserEntry entry) {
        if (entry.type() == EntryType.PARENT || entry.type() == EntryType.DIRECTORY) {
            this.currentDirectory = entry.path().toAbsolutePath().normalize();
            this.clearSelected();
            this.scrollOffset = 0;
            this.refreshDirectory();
            return;
        }

        this.selected = entry.path();
        this.parseSelectedTemplate();
    }

    private void goHome() {
        this.currentDirectory = this.rootDirectory;
        this.clearSelected();
        this.scrollOffset = 0;
        this.refreshDirectory();
    }

    private void loadSelected() {
        if (this.selected == null || !Files.isRegularFile(this.selected)) {
            this.statusText = Lang.tr("iterablock.gui.file_browser.choose_file_first");
            return;
        }

        this.parseSelectedTemplate();

        if (this.selectedParseError != null) {
            this.statusText = Lang.tr("iterablock.gui.file_browser.cannot_load", this.selectedParseError);
            return;
        }

        TemplateSelection.load(this.selected, this.selectedLitematicaInfo, this.selectedParseError);

        if (this.selectedLitematicaInfo != null) {
            if (LoadedLitematicManager.load(this.selected, this.selectedLitematicaInfo) == null) {
                this.statusText = Lang.tr("iterablock.gui.file_browser.load_limit", LoadedLitematicManager.MAX_LOADED);
                return;
            }
        }

        this.statusText = Lang.tr("iterablock.gui.file_browser.loaded", this.selected.getFileName());
    }

    private void parseSelectedTemplate() {
        this.selectedLitematicaInfo = null;
        this.selectedParseError = null;
        this.statusText = "";

        if (this.selected != null && isLitematicaFile(this.selected)) {
            try {
                this.selectedLitematicaInfo = LitematicaSchematicReader.read(this.selected);
            } catch (IOException | RuntimeException e) {
                this.selectedParseError = e.getMessage();
            }
        }
    }

    private void clearSelected() {
        this.selected = null;
        this.selectedLitematicaInfo = null;
        this.selectedParseError = null;
        this.statusText = "";
    }

    private List<SmallButton> getAllButtons(Layout layout) {
        return this.getBottomButtons(layout);
    }

    private List<SmallButton> getBottomButtons(Layout layout) {
        int y = layout.bottomY();
        int x = layout.x();
        int rightX = layout.x() + layout.width() - BOTTOM_BUTTON_WIDTH * 2 - 8;
        List<SmallButton> buttons = new ArrayList<>();
        buttons.add(new SmallButton(x, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT, Lang.tr("iterablock.gui.button.reload"), ButtonAction.RELOAD));
        buttons.add(new SmallButton(x + BOTTOM_BUTTON_WIDTH + 6, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT, Lang.tr("iterablock.gui.button.home"), ButtonAction.HOME));
        buttons.add(new SmallButton(rightX, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT, Lang.tr("iterablock.gui.button.load"), ButtonAction.LOAD));
        buttons.add(new SmallButton(rightX + BOTTOM_BUTTON_WIDTH + 6, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT, Lang.tr("iterablock.gui.button.back"), ButtonAction.CANCEL));
        return buttons;
    }

    private void returnToMainMenu() {
        this.closeGui(true);
        GuiBase.openGui(new GuiBuilderHelperMainMenu());
    }

    private Layout createLayout() {
        int x = SAFE_MARGIN;
        int y = 8;
        int width = Math.max(280, this.width - SAFE_MARGIN * 2);
        int controlY = y + 27;
        int bottomY = this.height - 4 - BUTTON_HEIGHT;
        int listY = y + TOP_HEIGHT;
        int listHeight = Math.max(72, bottomY - listY - 2);
        int gap = 10;
        int listWidth = Math.max(150, (int) Math.round(width * 0.47));
        int detailWidth = Math.max(120, width - listWidth - gap);
        int detailX = x + listWidth + gap;
        int searchX = x + 36;
        int searchWidth = Math.max(120, width - 36);
        return new Layout(x, y, width, controlY, searchX, searchWidth, listY, listHeight, listWidth, detailX, detailWidth, bottomY);
    }

    private int getVisibleRowCount(Layout layout) {
        return Math.max(1, layout.listHeight() / ROW_HEIGHT);
    }

    private String formatPath(Path path) {
        return path == null ? "" : path.toString().replace('\\', '/');
    }

    private String formatBlockPos(BlockPos pos) {
        return pos.getX() + " x " + pos.getY() + " x " + pos.getZ();
    }

    private String getSelectedDisplayName() {
        if (this.selectedLitematicaInfo != null) {
            String metadataName = this.selectedLitematicaInfo.metadata().name();

            if (metadataName != null && !metadataName.isBlank() && !metadataName.equalsIgnoreCase("Unnamed")) {
                return metadataName;
            }
        }

        return this.selected != null ? this.selected.getFileName().toString() : "";
    }

    private void ensureRowHoverSize() {
        if (this.rowHover.length == this.entries.size()) {
            return;
        }

        double[] next = new double[this.entries.size()];
        System.arraycopy(this.rowHover, 0, next, 0, Math.min(this.rowHover.length, next.length));
        this.rowHover = next;
    }

    private String getCaret() {
        return this.searchFocused && System.currentTimeMillis() / 450L % 2L == 0L ? "_" : "";
    }

    private void drawText(GuiGraphics guiGraphics, String text, double x, double y, int color, boolean shadow, double scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) scale, (float) scale, 1.0F);
        guiGraphics.drawString(this.textRenderer, text, (int) Math.round(x / scale), (int) Math.round(y / scale), color, shadow);
        guiGraphics.pose().popPose();
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

    private double approach(double current, double target, double speed, double deltaTime) {
        double step = speed * deltaTime;
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    private double easeOutCubic(double value) {
        double inverse = 1.0 - value;
        return 1.0 - inverse * inverse * inverse;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }

        return text.substring(0, Math.max(0, maxLength - 1)) + "...";
    }

    private String truncateMiddle(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }

        int keep = Math.max(4, (maxLength - 3) / 2);
        return text.substring(0, keep) + "..." + text.substring(text.length() - keep);
    }

    private static boolean isTemplateFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".nbt") || name.endsWith(".litematic") || name.endsWith(".litematica");
    }

    private static boolean isLitematicaFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".litematic") || name.endsWith(".litematica");
    }

    private enum EntryType {
        PARENT,
        DIRECTORY,
        FILE
    }

    private record BrowserEntry(Path path, EntryType type) {
        String name() {
            return this.type == EntryType.PARENT ? ".." : this.path.getFileName().toString();
        }

        int color(boolean selected, double hover) {
            if (selected) {
                return 0xFFFFF1B0;
            }

            return switch (this.type) {
                case PARENT -> 0xFFB8DCE8;
                case DIRECTORY -> hover > 0.02 ? 0xFFCFEAFF : 0xFF9FD0FF;
                case FILE -> hover > 0.02 ? 0xFFFFFFFF : 0xFFDDE8E8;
            };
        }

        int sortGroup() {
            return switch (this.type) {
                case PARENT -> 0;
                case DIRECTORY -> 1;
                case FILE -> 2;
            };
        }
    }

    private enum ButtonAction {
        HOME,
        RELOAD,
        LOAD,
        CANCEL
    }

    private record SmallButton(int x, int y, int width, int height, String label, ButtonAction action) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record Layout(int x, int y, int width, int controlY, int searchX, int searchWidth, int listY, int listHeight, int listWidth, int detailX, int detailWidth, int bottomY) {
        int listX() {
            return this.x;
        }
    }
}

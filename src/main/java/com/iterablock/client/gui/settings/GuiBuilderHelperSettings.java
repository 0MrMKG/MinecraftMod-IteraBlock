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
import com.iterablock.client.gui.GuiBuilderHelperMainMenu;
import com.iterablock.client.hotkeys.Hotkeys;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiBuilderHelperSettings extends GuiBase {
    private static final int SAFE_MARGIN = 22;
    private static final int TOP_Y = 28;
    private static final int TAB_HEIGHT = 20;
    private static final int ROW_HEIGHT = 26;
    private static final int ROW_GAP = 5;
    private static final int VALUE_WIDTH = 138;
    private static final int RESET_WIDTH = 48;
    private static final double TEXT_SCALE = 0.56;
    private static final double HOVER_SPEED = 8.0;
    private static final int ACCENT = 0xFFE1C76A;
    private static final int GREEN = 0xFF76D18A;
    private static final int RED = 0xFFE27676;
    private static final int TEXT = 0xFFDDE8E8;
    private static final int MUTED = 0xFF91A0A3;

    private final List<ConfigEntry> entries = new ArrayList<>();
    private final double[] tabHover = new double[Category.values().length];
    private double[] valueHover = new double[0];
    private double[] resetHover = new double[0];
    private Category category = Category.GENERAL;
    private ConfigEntry editingEntry;
    private int scrollOffset;
    private long lastFrameNanos;
    private final boolean returnToMainMenu;

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
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        Layout layout = this.createLayout();
        int tabX = layout.x();

        for (Category tab : Category.values()) {
            int tabWidth = this.getTabWidth(tab);

            if (this.isInside(mouseX, mouseY, tabX, layout.tabY(), tabWidth, TAB_HEIGHT)) {
                this.category = tab;
                this.scrollOffset = 0;
                this.editingEntry = null;
                return true;
            }

            tabX += tabWidth + 3;
        }

        for (RowPlacement row : this.getVisibleRows(layout)) {
            ConfigEntry entry = row.entry();

            if (this.isInside(mouseX, mouseY, row.valueX(), row.y() + 4, row.valueWidth(), 18)) {
                this.handleValueClick(entry);
                return true;
            }

            if (this.isInside(mouseX, mouseY, row.resetX(), row.y() + 5, RESET_WIDTH, 16)) {
                entry.reset();
                this.editingEntry = null;
                this.saveConfig();
                return true;
            }
        }

        this.editingEntry = null;
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
            double hover = this.easeOutCubic(this.tabHover[tab.ordinal()]);
            int fill = this.withAlpha(this.blendRgb(0x111719, 0x273033, Math.max(hover, selected ? 0.7 : 0.0)), selected ? 0.76 : 0.58 + hover * 0.16);
            int lineWidth = selected ? width - 8 : (int) Math.round((width - 8) * hover);

            guiGraphics.fill(x, layout.tabY(), x + width, layout.tabY() + TAB_HEIGHT, fill);
            guiGraphics.fill(x, layout.tabY() + TAB_HEIGHT - 2, x + 4 + lineWidth, layout.tabY() + TAB_HEIGHT, ACCENT);
            this.drawText(guiGraphics, tab.title(), x + 8, layout.tabY() + 7, selected || hover > 0.02 ? 0xFFFFF1B0 : TEXT);
            x += width + 3;
        }
    }

    private void drawListPanel(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        guiGraphics.fill(layout.x(), layout.listY(), layout.x() + layout.width(), layout.listY() + layout.listHeight(), 0x7C0B1012);
        guiGraphics.fill(layout.x(), layout.listY(), layout.x() + layout.width(), layout.listY() + 1, 0x885B6969);

        for (RowPlacement row : this.getVisibleRows(layout)) {
            this.drawRow(guiGraphics, mouseX, mouseY, row);
        }

        this.drawScrollbar(guiGraphics, layout);
    }

    private void drawRow(GuiGraphics guiGraphics, int mouseX, int mouseY, RowPlacement row) {
        ConfigEntry entry = row.entry();
        double valueHoverEase = this.easeOutCubic(this.valueHover[entry.index]);
        double resetHoverEase = this.easeOutCubic(this.resetHover[entry.index]);
        int y = row.y();
        int valueX = row.valueX() + (int) Math.round(valueHoverEase * 3.0);
        int valueColor = this.withAlpha(this.blendRgb(0x171E20, 0x2D3838, valueHoverEase), 0.78);
        int resetColor = this.withAlpha(this.blendRgb(0x121719, 0x2A2E30, resetHoverEase), 0.72);
        int valueTextColor = entry.type == EntryType.BOOLEAN ? (entry.booleanValue() ? GREEN : RED) : TEXT;

        guiGraphics.fill(row.x(), y, row.x() + row.width(), y + ROW_HEIGHT, 0x33151A1D);
        guiGraphics.fill(row.x(), y, row.x() + 2, y + ROW_HEIGHT, 0x806F8D78);
        this.drawText(guiGraphics, entry.label(), row.x() + 10, y + 10, TEXT);

        guiGraphics.fill(valueX, y + 4, valueX + row.valueWidth(), y + 22, valueColor);
        guiGraphics.fill(valueX, y + 4, valueX + Math.max(3, (int) Math.round(row.valueWidth() * 0.22 * valueHoverEase)), y + 6, ACCENT);
        this.drawText(guiGraphics, entry.displayValue(this.editingEntry == entry), valueX + 8, y + 10, valueTextColor);

        guiGraphics.fill(row.resetX(), y + 5, row.resetX() + RESET_WIDTH, y + 21, resetColor);
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.settings.reset"), row.resetX() + 8, y + 10, resetHoverEase > 0.02 ? 0xFFFFF1B0 : MUTED);
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

        guiGraphics.fill(barX, trackY, barX + 2, trackY + trackHeight, 0x66151A1D);
        guiGraphics.fill(barX - 1, thumbY, barX + 3, thumbY + thumbHeight, 0xAA6F8D78);
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
            tabX += width + 3;
        }

        for (ConfigEntry entry : this.entries) {
            this.valueHover[entry.index] = this.approach(this.valueHover[entry.index], 0.0, HOVER_SPEED, deltaTime);
            this.resetHover[entry.index] = this.approach(this.resetHover[entry.index], 0.0, HOVER_SPEED, deltaTime);
        }

        for (RowPlacement row : this.getVisibleRows(layout)) {
            ConfigEntry entry = row.entry();
            double valueTarget = this.isInside(mouseX, mouseY, row.valueX(), row.y() + 4, row.valueWidth(), 18) ? 1.0 : 0.0;
            double resetTarget = this.isInside(mouseX, mouseY, row.resetX(), row.y() + 5, RESET_WIDTH, 16) ? 1.0 : 0.0;
            this.valueHover[entry.index] = this.approach(this.valueHover[entry.index], valueTarget, HOVER_SPEED, deltaTime);
            this.resetHover[entry.index] = this.approach(this.resetHover[entry.index], resetTarget, HOVER_SPEED, deltaTime);
        }
    }

    private void handleValueClick(ConfigEntry entry) {
        if (entry.type == EntryType.BOOLEAN) {
            entry.value = Boolean.toString(!entry.booleanValue());
            this.editingEntry = null;
            this.saveConfig();
        } else if (entry.type == EntryType.ENUM) {
            entry.cycleEnum();
            this.editingEntry = null;
            this.saveConfig();
        } else if (entry.type == EntryType.TEXT) {
            this.editingEntry = entry;
        }
    }

    private List<RowPlacement> getVisibleRows(Layout layout) {
        List<ConfigEntry> categoryEntries = this.getEntriesForCategory();
        List<RowPlacement> rows = new ArrayList<>();
        int visibleRows = this.getVisibleRowCount(layout);
        int labelWidth = Math.max(120, layout.width() - VALUE_WIDTH - RESET_WIDTH - 48);
        int valueX = layout.x() + labelWidth + 18;
        int resetX = layout.x() + layout.width() - RESET_WIDTH - 12;
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
        return this.getScaledStringWidth(tab.title()) + 14;
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
        int listY = y + 38;
        int listHeight = Math.max(80, this.height - listY - SAFE_MARGIN);
        return new Layout(x, y - 16, y, listY, width, listHeight);
    }

    private void createEntries() {
        this.entries.clear();
        this.add(Category.GENERAL, "templatePath", "iterablock.gui.settings.option.template_path", EntryType.TEXT, Minecraft.getInstance().gameDirectory.toPath().resolve("schematics").toAbsolutePath().toString());
        this.add(Category.GENERAL, "autoCreateDirs", "iterablock.gui.settings.option.auto_create_dirs", EntryType.BOOLEAN, "true");
        this.add(Category.GENERAL, "languageMode", "iterablock.gui.settings.option.language_mode", EntryType.ENUM, "auto", "auto", "zh_cn", "en_us");
        this.add(Category.UI_HUD, "showToolHud", "iterablock.gui.settings.option.show_tool_hud", EntryType.BOOLEAN, "true");
        this.add(Category.UI_HUD, "showCurrentLitematic", "iterablock.gui.settings.option.show_current_litematic", EntryType.BOOLEAN, "true");
        this.add(Category.UI_HUD, "hudAnchor", "iterablock.gui.settings.option.hud_anchor", EntryType.ENUM, "top_left", "top_left", "top_right", "bottom_left", "bottom_right");
        this.add(Category.UI_HUD, "hudScale", "iterablock.gui.settings.option.hud_scale", EntryType.ENUM, "normal", "small", "normal", "large");
        this.add(Category.VISUALS, "darkenMenus", "iterablock.gui.settings.option.darken_menus", EntryType.BOOLEAN, "true");
        this.add(Category.VISUALS, "radialAnimation", "iterablock.gui.settings.option.radial_animation", EntryType.BOOLEAN, "true");
        this.add(Category.VISUALS, "accentColor", "iterablock.gui.settings.option.accent_color", EntryType.ENUM, "gray_green", "gray_green", "yellow", "blue");
        this.add(Category.HOTKEYS, "openFilesKey", "iterablock.gui.settings.keybind.open_files", EntryType.TEXT, Hotkeys.OPEN_MAIN_MENU.getKeybind().getKeysDisplayString());
        this.add(Category.HOTKEYS, "openRadialKey", "iterablock.gui.settings.keybind.open_radial", EntryType.TEXT, Hotkeys.OPEN_RADIAL_MENU.getKeybind().getKeysDisplayString());
        this.add(Category.HOTKEYS, "openMainMenuKey", "iterablock.gui.settings.keybind.open_main_menu", EntryType.TEXT, Hotkeys.OPEN_SETTINGS_MENU.getKeybind().getKeysDisplayString());
        this.add(Category.HOTKEYS, "placeProjectionKey", "iterablock.gui.settings.keybind.place_projection", EntryType.TEXT, "Y");
        this.add(Category.HOTKEYS, "rotateProjectionKey", "iterablock.gui.settings.keybind.rotate_projection", EntryType.TEXT, "R");
        this.add(Category.LITEMATIC, "defaultLoadMode", "iterablock.gui.settings.option.default_load_mode", EntryType.ENUM, "select", "select", "preview", "silent");
        this.add(Category.LITEMATIC, "keepSelectionAfterLoad", "iterablock.gui.settings.option.keep_selection", EntryType.BOOLEAN, "true");
        this.add(Category.LITEMATIC, "scanSubdirectories", "iterablock.gui.settings.option.scan_subdirectories", EntryType.BOOLEAN, "false");
        this.add(Category.LITEMATIC, "placementRange", "iterablock.gui.settings.option.placement_range", EntryType.TEXT, "100");
        this.add(Category.LITEMATIC, "linearArrayRenderLimit", "iterablock.gui.settings.option.linear_array_render_limit", EntryType.TEXT, "5");
        this.add(Category.DEBUG, "showDebugHud", "iterablock.gui.settings.option.show_debug_hud", EntryType.BOOLEAN, "false");
        this.add(Category.DEBUG, "verboseLog", "iterablock.gui.settings.option.verbose_log", EntryType.BOOLEAN, "false");
        this.add(Category.DEBUG, "debugOverlayMode", "iterablock.gui.settings.option.debug_overlay", EntryType.ENUM, "off", "off", "minimal", "full");
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
        }
    }

    private void saveConfig() {
        Properties properties = new Properties();

        for (ConfigEntry entry : this.entries) {
            properties.setProperty(entry.key, entry.value);
        }

        Path path = this.getConfigPath();

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

    private enum Category {
        GENERAL("iterablock.gui.settings.tab.general"),
        UI_HUD("iterablock.gui.settings.tab.ui_hud"),
        VISUALS("iterablock.gui.settings.tab.visuals"),
        HOTKEYS("iterablock.gui.settings.tab.hotkeys"),
        LITEMATIC("iterablock.gui.settings.tab.litematic"),
        DEBUG("iterablock.gui.settings.tab.debug");

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
        ENUM
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

        private String displayValue(boolean editing) {
            if (this.type == EntryType.BOOLEAN) {
                return Lang.tr(this.booleanValue() ? "iterablock.gui.settings.value.true" : "iterablock.gui.settings.value.false");
            }

            return editing ? this.value + "_" : this.value;
        }
    }

    private record Layout(int x, int titleY, int tabY, int listY, int width, int listHeight) {
    }

    private record RowPlacement(ConfigEntry entry, int x, int y, int width, int valueX, int valueWidth, int resetX) {
    }
}

package com.iterablock.client.gui;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiBezierCurveConfig extends GuiBase {
    private static final int SAFE_MARGIN = 22;
    private static final int PANEL_MAX_WIDTH = 260;
    private static final int SECTION_HEIGHT = 18;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_GAP = 2;
    private static final int VALUE_WIDTH = 96;
    private static final int CONTROL_HEIGHT = 14;
    private static final int RESET_WIDTH = 42;
    private static final int BACK_WIDTH = 58;
    private static final int BACK_HEIGHT = 16;
    private static final double TEXT_SCALE = 0.52;
    private static final int TITLE_COLOR = 0xFFF3EECF;
    private static final int TEXT_COLOR = 0xFFDDE8E8;
    private static final int ACTIVE_COLOR = 0xFFFFFFB8;
    private static final int ACCENT = 0xFFF3C6D3;

    private final boolean returnToMainMenu;
    private Field editingField;
    private String editingValue = "";

    public GuiBezierCurveConfig() {
        this(false);
    }

    public GuiBezierCurveConfig(boolean returnToMainMenu) {
        this.returnToMainMenu = returnToMainMenu;
        this.setTitle(Lang.tr("iterablock.gui.bezier_config.title"));
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

        this.drawText(guiGraphics, Lang.tr("iterablock.gui.bezier_config.title"), layout.x(), layout.titleY(), TITLE_COLOR, true);
        this.drawPanel(guiGraphics, layout);
        this.drawRows(guiGraphics, layout);
        this.drawBackButton(guiGraphics, mouseX, mouseY, layout);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            Layout layout = this.createLayout();

            if (this.isInside(mouseX, mouseY, layout.backX(), layout.backY(), BACK_WIDTH, BACK_HEIGHT)) {
                this.commitEditingValue();
                this.returnToMainMenu();
                return true;
            }

            for (Field field : Field.values()) {
                Row row = this.getRow(layout, field.ordinal());

                if (this.isInside(mouseX, mouseY, row.resetX(), row.y() + 1, RESET_WIDTH, CONTROL_HEIGHT)) {
                    this.commitEditingValue();
                    this.resetField(field);
                    return true;
                }

                if (this.isInside(mouseX, mouseY, row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT)) {
                    if (field.booleanField()) {
                        this.commitEditingValue();
                        BuilderHelperClientConfig.setBezierPlaceNbtMode(!BuilderHelperClientConfig.isBezierPlaceNbtMode());
                    } else {
                        this.startEditing(field);
                    }

                    return true;
                }
            }
        }

        this.commitEditingValue();
        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (this.editingField != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !this.editingValue.isEmpty()) {
                this.editingValue = this.editingValue.substring(0, this.editingValue.length() - 1);
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                this.editingValue = "";
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.commitEditingValue();
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.commitEditingValue();
            this.returnToMainMenu();
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(char charIn, int modifiers) {
        if (this.editingField == null) {
            return super.onCharTyped(charIn, modifiers);
        }

        if (charIn >= '0' && charIn <= '9') {
            this.editingValue += charIn;
        }

        return true;
    }

    private void drawPanel(GuiGraphics guiGraphics, Layout layout) {
        guiGraphics.fill(layout.x(), layout.panelY(), layout.x() + layout.width(), layout.panelY() + layout.panelHeight(), 0x7A0B1012);
        guiGraphics.fill(layout.x(), layout.panelY(), layout.x() + layout.width(), layout.panelY() + 1, 0xBFFFFFFF);
        guiGraphics.fill(layout.x(), layout.panelY(), layout.x() + 1, layout.panelY() + layout.panelHeight(), 0x66FFFFFF);
        guiGraphics.fill(layout.x() + layout.width() - 1, layout.panelY(), layout.x() + layout.width(), layout.panelY() + layout.panelHeight(), 0x66FFFFFF);
        guiGraphics.fill(layout.x(), layout.panelY() + layout.panelHeight() - 1, layout.x() + layout.width(), layout.panelY() + layout.panelHeight(), 0x66FFFFFF);
        guiGraphics.fill(layout.x(), layout.panelY() + SECTION_HEIGHT, layout.x() + layout.width(), layout.panelY() + SECTION_HEIGHT + 1, 0x553D5558);
        guiGraphics.fill(layout.x(), layout.panelY() + SECTION_HEIGHT - 2, layout.x() + 52, layout.panelY() + SECTION_HEIGHT, ACCENT);
        this.drawCenteredText(guiGraphics, Lang.tr("iterablock.gui.bezier_config.section.basic"), layout.x(), layout.panelY(), 86, SECTION_HEIGHT, TEXT_COLOR);
    }

    private void drawRows(GuiGraphics guiGraphics, Layout layout) {
        for (Field field : Field.values()) {
            Row row = this.getRow(layout, field.ordinal());
            this.drawConfigRow(guiGraphics, row, Lang.tr(field.labelKey()), this.getFieldDisplayValue(field));
        }
    }

    private void drawConfigRow(GuiGraphics guiGraphics, Row row, String label, String value) {
        int controlY = row.y() + 1;
        this.drawSimpleButton(guiGraphics, row.x(), controlY, row.labelWidth(), CONTROL_HEIGHT,
                this.fitText(label, row.labelWidth() - 8), false);
        this.drawSimpleButton(guiGraphics, row.valueX(), controlY, row.valueWidth(), CONTROL_HEIGHT, value, false);
        this.drawSimpleButton(guiGraphics, row.resetX(), controlY, RESET_WIDTH, CONTROL_HEIGHT,
                Lang.tr("iterablock.gui.settings.reset"), false);
    }

    private void drawBackButton(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        boolean hovered = this.isInside(mouseX, mouseY, layout.backX(), layout.backY(), BACK_WIDTH, BACK_HEIGHT);
        this.drawSimpleButton(guiGraphics, layout.backX(), layout.backY(), BACK_WIDTH, BACK_HEIGHT, Lang.tr("iterablock.gui.button.back"), hovered);
    }

    private void drawSimpleButton(GuiGraphics guiGraphics, int x, int y, int width, int height, String label, boolean hovered) {
        int fill = hovered ? 0xAA5E6666 : 0x8A4A5050;
        int border = hovered ? 0xE8FFFFFF : 0xBFFFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
        guiGraphics.fill(x + 3, y + height - 2, x + (hovered ? width - 4 : 10), y + height, ACCENT);
        this.drawCenteredText(guiGraphics, label, x, y, width, height, hovered ? ACTIVE_COLOR : TEXT_COLOR);
    }

    private Row getRow(Layout layout, int index) {
        int y = layout.listY() + index * (ROW_HEIGHT + ROW_GAP);
        int rowX = layout.x() + 6;
        int resetX = layout.x() + layout.width() - RESET_WIDTH - 6;
        int labelWidth = Math.max(52, Math.min(VALUE_WIDTH, (resetX - rowX) / 2 - 4));
        int valueX = rowX + labelWidth + 6;
        int valueWidth = Math.max(42, resetX - valueX - 6);
        return new Row(rowX, y, resetX - rowX, labelWidth, valueX, valueWidth, resetX);
    }

    private void startEditing(Field field) {
        if (this.editingField == field || field.booleanField()) {
            return;
        }

        this.commitEditingValue();
        this.editingField = field;
        this.editingValue = Integer.toString(this.getFieldValue(field));
    }

    private void commitEditingValue() {
        if (this.editingField == null) {
            return;
        }

        int value = this.parseEditingValue(this.editingValue, this.getFieldValue(this.editingField));

        switch (this.editingField) {
            case PLACEMENT_PRECISION -> BuilderHelperClientConfig.setBezierPlacementPrecision(value);
            case PLACEMENT_WIDTH -> BuilderHelperClientConfig.setBezierPlacementWidth(value);
            case CONTROL_POINT_COUNT -> BuilderHelperClientConfig.setBezierControlPointCount(value);
            case PLACE_NBT_MODE -> {
            }
        }

        this.editingField = null;
        this.editingValue = "";
    }

    private String getFieldDisplayValue(Field field) {
        if (this.editingField == field) {
            return this.editingValue + "_";
        }

        if (field == Field.PLACE_NBT_MODE) {
            return Boolean.toString(BuilderHelperClientConfig.isBezierPlaceNbtMode());
        }

        return Integer.toString(this.getFieldValue(field));
    }

    private int getFieldValue(Field field) {
        return switch (field) {
            case PLACEMENT_PRECISION -> BuilderHelperClientConfig.getBezierPlacementPrecision();
            case PLACEMENT_WIDTH -> BuilderHelperClientConfig.getBezierPlacementWidth();
            case CONTROL_POINT_COUNT -> BuilderHelperClientConfig.getBezierControlPointCount();
            case PLACE_NBT_MODE -> BuilderHelperClientConfig.isBezierPlaceNbtMode() ? 1 : 0;
        };
    }

    private int parseEditingValue(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void resetField(Field field) {
        switch (field) {
            case PLACEMENT_PRECISION -> BuilderHelperClientConfig.setBezierPlacementPrecision(BuilderHelperClientConfig.DEFAULT_BEZIER_PLACEMENT_PRECISION);
            case PLACEMENT_WIDTH -> BuilderHelperClientConfig.setBezierPlacementWidth(BuilderHelperClientConfig.DEFAULT_BEZIER_PLACEMENT_WIDTH);
            case CONTROL_POINT_COUNT -> BuilderHelperClientConfig.setBezierControlPointCount(BuilderHelperClientConfig.DEFAULT_BEZIER_CONTROL_POINT_COUNT);
            case PLACE_NBT_MODE -> BuilderHelperClientConfig.setBezierPlaceNbtMode(BuilderHelperClientConfig.DEFAULT_BEZIER_PLACE_NBT_MODE);
        }
    }

    private Layout createLayout() {
        int horizontalMargin = Math.min(SAFE_MARGIN, Math.max(4, (this.width - 180) / 2));
        int availableWidth = Math.max(1, this.width - horizontalMargin * 2);
        int width = Math.min(availableWidth, PANEL_MAX_WIDTH);
        int x = (this.width - width) / 2;
        int titleY = Math.max(10, SAFE_MARGIN);
        int panelY = titleY + 20;
        int desiredPanelHeight = SECTION_HEIGHT + 8 + Field.values().length * ROW_HEIGHT
                + (Field.values().length - 1) * ROW_GAP + 8;
        int maximumPanelHeight = Math.max(SECTION_HEIGHT + ROW_HEIGHT + 16,
                this.height - SAFE_MARGIN - BACK_HEIGHT - panelY - 10);
        int panelHeight = Math.min(desiredPanelHeight, maximumPanelHeight);
        int listY = panelY + SECTION_HEIGHT + 8;
        int backY = Math.min(this.height - SAFE_MARGIN - BACK_HEIGHT, panelY + panelHeight + 8);
        int backX = x + width - BACK_WIDTH;
        return new Layout(x, titleY, panelY, width, panelHeight, listY, backX, backY);
    }

    private void returnToMainMenu() {
        this.closeGui(true);
        if (this.returnToMainMenu) {
            GuiBase.openGui(new GuiBuilderHelperMainMenu());
        }
    }

    private void drawText(GuiGraphics guiGraphics, String text, double x, double y, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) TEXT_SCALE, (float) TEXT_SCALE, 1.0F);
        guiGraphics.drawString(this.textRenderer, text, (int) Math.round(x / TEXT_SCALE), (int) Math.round(y / TEXT_SCALE), color, shadow);
        guiGraphics.pose().popPose();
    }

    private void drawCenteredText(GuiGraphics guiGraphics, String text, int x, int y, int width, int height, int color) {
        int textX = x + (int) Math.round((width - this.textRenderer.width(text) * TEXT_SCALE) / 2.0);
        int textY = y + (int) Math.round((height - 8.0 * TEXT_SCALE) / 2.0);
        this.drawText(guiGraphics, text, textX, textY, color, false);
    }

    private String fitText(String text, int maxWidth) {
        if (this.textRenderer.width(text) * TEXT_SCALE <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = (int) Math.ceil(this.textRenderer.width(suffix) * TEXT_SCALE);
        if (suffixWidth > maxWidth) {
            return "";
        }
        String result = text;

        while (!result.isEmpty() && this.textRenderer.width(result) * TEXT_SCALE + suffixWidth > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }

        return result.isEmpty() ? suffix : result + suffix;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int x, int titleY, int panelY, int width, int panelHeight, int listY, int backX, int backY) {
    }

    private record Row(int x, int y, int width, int labelWidth, int valueX, int valueWidth, int resetX) {
    }

    private enum Field {
        PLACEMENT_PRECISION("iterablock.gui.bezier_config.option.placement_precision", false),
        PLACEMENT_WIDTH("iterablock.gui.bezier_config.option.placement_width", false),
        CONTROL_POINT_COUNT("iterablock.gui.bezier_config.option.control_point_count", false),
        PLACE_NBT_MODE("iterablock.gui.bezier_config.option.place_nbt_mode", true);

        private final String labelKey;
        private final boolean booleanField;

        Field(String labelKey, boolean booleanField) {
            this.labelKey = labelKey;
            this.booleanField = booleanField;
        }

        private String labelKey() {
            return this.labelKey;
        }

        private boolean booleanField() {
            return this.booleanField;
        }
    }
}

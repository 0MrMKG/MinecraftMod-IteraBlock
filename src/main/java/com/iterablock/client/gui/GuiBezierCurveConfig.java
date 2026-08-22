package com.iterablock.client.gui;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.gui.settings.GuiSettingsControls;
import com.iterablock.client.gui.settings.SettingsMenuCommonRenderer;

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
    private static final int TEXT_COLOR = 0xFFDDE8E8;
    private static final double[] HIDDEN_TAB_INDICATOR = new double[SettingsMenuCommonRenderer.INDICATOR_SEGMENTS];

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
        SettingsMenuCommonRenderer.drawBackground(guiGraphics, this.width, this.height);
    }

    @Override
    protected void drawTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void drawContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Layout layout = this.createLayout();

        SettingsMenuCommonRenderer.drawTitle(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.bezier_config.title"), layout.x(), layout.titleY());
        this.drawPanel(guiGraphics, layout);
        this.drawRows(guiGraphics, mouseX, mouseY, layout);
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
        SettingsMenuCommonRenderer.drawPanel(guiGraphics, layout.x(), layout.panelY(), layout.width(), layout.panelHeight(),
                layout.panelY() + SECTION_HEIGHT);
        SettingsMenuCommonRenderer.drawTab(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.bezier_config.section.basic"),
                layout.x(), layout.panelY(), layout.width(), SECTION_HEIGHT, true, false, 0.0, HIDDEN_TAB_INDICATOR);
    }

    private void drawRows(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        for (Field field : Field.values()) {
            Row row = this.getRow(layout, field.ordinal());
            this.drawConfigRow(guiGraphics, mouseX, mouseY, row, Lang.tr(field.labelKey()), this.getFieldDisplayValue(field));
        }
    }

    private void drawConfigRow(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row, String label, String value) {
        int controlY = row.y() + 1;
        GuiSettingsControls.drawLabel(guiGraphics, this.textRenderer, this.fitText(label, row.labelWidth() - 8),
                row.x(), controlY, row.labelWidth(), CONTROL_HEIGHT);
        GuiSettingsControls.drawValueField(guiGraphics, this.textRenderer, value, row.valueX(), controlY,
                row.valueWidth(), CONTROL_HEIGHT,
                this.isInside(mouseX, mouseY, row.valueX(), controlY, row.valueWidth(), CONTROL_HEIGHT) ? 1.0 : 0.0,
                TEXT_COLOR);
        GuiSettingsControls.drawButton(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.settings.reset"),
                row.resetX(), controlY, RESET_WIDTH, CONTROL_HEIGHT,
                this.isInside(mouseX, mouseY, row.resetX(), controlY, RESET_WIDTH, CONTROL_HEIGHT) ? 1.0 : 0.0);
    }

    private void drawBackButton(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        boolean hovered = this.isInside(mouseX, mouseY, layout.backX(), layout.backY(), BACK_WIDTH, BACK_HEIGHT);
        GuiSettingsControls.drawButton(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.button.back"),
                layout.backX(), layout.backY(), BACK_WIDTH, BACK_HEIGHT, hovered ? 1.0 : 0.0);
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
        return GuiSettingsControls.contains(mouseX, mouseY, x, y, width, height);
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

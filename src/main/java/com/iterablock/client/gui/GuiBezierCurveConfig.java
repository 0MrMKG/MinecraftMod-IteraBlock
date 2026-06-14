package com.iterablock.client.gui;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiBezierCurveConfig extends GuiBase {
    private static final int SAFE_MARGIN = 22;
    private static final int ROW_HEIGHT = 22;
    private static final int ROW_GAP = 4;
    private static final int VALUE_WIDTH = 112;
    private static final int BACK_WIDTH = 58;
    private static final int BACK_HEIGHT = 16;
    private static final double TEXT_SCALE = 0.58;
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

                if (this.isInside(mouseX, mouseY, row.valueX(), row.y() + 3, VALUE_WIDTH, 16)) {
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
        guiGraphics.fill(layout.x(), layout.panelY() + 25, layout.x() + layout.width(), layout.panelY() + 26, 0x553D5558);
        guiGraphics.fill(layout.x(), layout.panelY() + 25, layout.x() + 52, layout.panelY() + 27, ACCENT);
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.bezier_config.section.basic"), layout.x() + 8, layout.panelY() + 9, TEXT_COLOR, false);
    }

    private void drawRows(GuiGraphics guiGraphics, Layout layout) {
        for (Field field : Field.values()) {
            Row row = this.getRow(layout, field.ordinal());
            this.drawConfigRow(guiGraphics, row, Lang.tr(field.labelKey()), this.getFieldDisplayValue(field));
        }
    }

    private void drawConfigRow(GuiGraphics guiGraphics, Row row, String label, String value) {
        guiGraphics.fill(row.x(), row.y(), row.x() + row.width(), row.y() + ROW_HEIGHT, 0x22101719);
        guiGraphics.fill(row.x(), row.y(), row.x() + 2, row.y() + ROW_HEIGHT, 0x66786A75);
        this.drawText(guiGraphics, label, row.x() + 10, row.y() + 8, TEXT_COLOR, false);

        guiGraphics.fill(row.valueX(), row.y() + 3, row.valueX() + VALUE_WIDTH, row.y() + 19, 0x8A4A5050);
        guiGraphics.fill(row.valueX(), row.y() + 3, row.valueX() + VALUE_WIDTH, row.y() + 4, 0xBFFFFFFF);
        guiGraphics.fill(row.valueX(), row.y() + 18, row.valueX() + VALUE_WIDTH, row.y() + 19, 0xBFFFFFFF);
        guiGraphics.fill(row.valueX(), row.y() + 3, row.valueX() + 1, row.y() + 19, 0xBFFFFFFF);
        guiGraphics.fill(row.valueX() + VALUE_WIDTH - 1, row.y() + 3, row.valueX() + VALUE_WIDTH, row.y() + 19, 0xBFFFFFFF);
        this.drawCenteredText(guiGraphics, value, row.valueX(), row.y() + 3, VALUE_WIDTH, 16, TEXT_COLOR);
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
        int valueX = layout.x() + layout.width() - VALUE_WIDTH - 10;
        return new Row(layout.x() + 6, y, layout.width() - 12, valueX);
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

    private Layout createLayout() {
        int width = Math.min(this.width - SAFE_MARGIN * 2, 420);
        int x = (this.width - width) / 2;
        int titleY = Math.max(10, SAFE_MARGIN);
        int panelY = titleY + 20;
        int backY = this.height - SAFE_MARGIN - BACK_HEIGHT;
        int panelHeight = Math.max(120, backY - panelY - 10);
        int listY = panelY + 34;
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

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int x, int titleY, int panelY, int width, int panelHeight, int listY, int backX, int backY) {
    }

    private record Row(int x, int y, int width, int valueX) {
    }

    private enum Field {
        PLACEMENT_PRECISION("iterablock.gui.bezier_config.option.placement_precision", false),
        PLACEMENT_WIDTH("iterablock.gui.bezier_config.option.placement_width", false),
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

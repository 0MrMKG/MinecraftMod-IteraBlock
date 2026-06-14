package com.iterablock.client.gui;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiRandomPlacementConfig extends GuiBase {
    private static final int SAFE_MARGIN = 22;
    private static final int ROW_HEIGHT = 22;
    private static final int ROW_GAP = 4;
    private static final int VALUE_WIDTH = 112;
    private static final int BACK_WIDTH = 58;
    private static final int BACK_HEIGHT = 16;
    private static final double TEXT_SCALE = 0.58;
    private static final int TITLE_COLOR = 0xFFF3EECF;
    private static final int TEXT_COLOR = 0xFFDDE8E8;
    private static final int MUTED_COLOR = 0xFF91A0A3;
    private static final int ACTIVE_COLOR = 0xFFFFFFB8;
    private static final int ACCENT = 0xFF7BCFA3;

    private final boolean returnToMainMenu;
    private Field editingField;
    private String editingValue = "";
    private boolean draggingRotationSlider;

    public GuiRandomPlacementConfig() {
        this(false);
    }

    public GuiRandomPlacementConfig(boolean returnToMainMenu) {
        this.returnToMainMenu = returnToMainMenu;
        this.setTitle(Lang.tr("iterablock.gui.random_config.title"));
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

        this.drawText(guiGraphics, Lang.tr("iterablock.gui.random_config.title"), layout.x(), layout.titleY(), TITLE_COLOR, true);
        this.drawPanel(guiGraphics, layout);
        this.updateDragging(mouseX);
        this.drawRows(guiGraphics, mouseX, mouseY, layout);
        this.drawBackButton(guiGraphics, mouseX, mouseY, layout);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        Layout layout = this.createLayout();

        if (this.isInside(mouseX, mouseY, layout.backX(), layout.backY(), BACK_WIDTH, BACK_HEIGHT)) {
            this.commitEditingValue();
            this.returnToMainMenu();
            return true;
        }

        for (Field field : Field.values()) {
            Row row = this.getRow(layout, field.ordinal());

            if (field == Field.ROTATION_CHANCE && this.isInside(mouseX, mouseY, row.valueX(), row.y() + 5, VALUE_WIDTH, 12)) {
                this.commitEditingValue();
                this.draggingRotationSlider = true;
                this.setRotationChanceFromMouse(row, mouseX);
                return true;
            }

            if (this.isHeightField(field)) {
                if (this.isInside(mouseX, mouseY, this.getMinusButtonX(row), row.y() + 3, 16, 16)) {
                    this.commitEditingValue();
                    this.adjustField(field, -1);
                    return true;
                }

                if (this.isInside(mouseX, mouseY, this.getPlusButtonX(row), row.y() + 3, 16, 16)) {
                    this.commitEditingValue();
                    this.adjustField(field, 1);
                    return true;
                }
            }

            if (this.isInside(mouseX, mouseY, row.valueX(), row.y() + 3, VALUE_WIDTH, 16)) {
                if (field == Field.ROTATION_CHANCE) {
                    return true;
                }

                this.startEditing(field);
                return true;
            }
        }

        this.commitEditingValue();
        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.draggingRotationSlider) {
            this.draggingRotationSlider = false;
            return true;
        }

        return super.onMouseReleased(mouseX, mouseY, mouseButton);
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
            this.draggingRotationSlider = false;
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

        if ((charIn >= '0' && charIn <= '9') || (charIn == '-' && this.editingValue.isEmpty() && this.editingField.allowsNegative())) {
            this.editingValue += charIn;
            return true;
        }

        return true;
    }

    private void drawPanel(GuiGraphics guiGraphics, Layout layout) {
        guiGraphics.fill(layout.x(), layout.panelY(), layout.x() + layout.width(), layout.panelY() + layout.panelHeight(), 0x7A0B1012);
        guiGraphics.fill(layout.x(), layout.panelY(), layout.x() + layout.width(), layout.panelY() + 1, 0xBFFFFFFF);
        guiGraphics.fill(layout.x(), layout.panelY() + 25, layout.x() + layout.width(), layout.panelY() + 26, 0x553D5558);
        guiGraphics.fill(layout.x(), layout.panelY() + 25, layout.x() + 52, layout.panelY() + 27, ACCENT);
        this.drawText(guiGraphics, Lang.tr("iterablock.gui.random_config.section.basic"), layout.x() + 8, layout.panelY() + 9, TEXT_COLOR, false);
    }

    private void drawRows(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        for (Field field : Field.values()) {
            Row row = this.getRow(layout, field.ordinal());

            if (field == Field.ROTATION_CHANCE) {
                this.drawSliderRow(guiGraphics, mouseX, mouseY, row, Lang.tr(field.labelKey()), BuilderHelperClientConfig.getRandomPlacementRotationChance());
            } else if (this.isHeightField(field)) {
                this.drawStepperRow(guiGraphics, mouseX, mouseY, row, Lang.tr(field.labelKey()), this.getFieldDisplayValue(field));
            } else {
                this.drawConfigRow(guiGraphics, mouseX, mouseY, row, Lang.tr(field.labelKey()), this.getFieldDisplayValue(field), true);
            }
        }
    }

    private void drawConfigRow(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row, String label, String value, boolean clickable) {
        boolean hovered = clickable && this.isInside(mouseX, mouseY, row.valueX(), row.y() + 3, VALUE_WIDTH, 16);
        int fill = hovered ? 0xAA5E6666 : 0x8A4A5050;
        int border = hovered ? 0xE8FFFFFF : 0xBFFFFFFF;

        guiGraphics.fill(row.x(), row.y(), row.x() + row.width(), row.y() + ROW_HEIGHT, 0x22101719);
        guiGraphics.fill(row.x(), row.y(), row.x() + 2, row.y() + ROW_HEIGHT, 0x668CAEA8);
        this.drawText(guiGraphics, label, row.x() + 10, row.y() + 8, TEXT_COLOR, false);

        guiGraphics.fill(row.valueX(), row.y() + 3, row.valueX() + VALUE_WIDTH, row.y() + 19, fill);
        guiGraphics.fill(row.valueX(), row.y() + 3, row.valueX() + VALUE_WIDTH, row.y() + 4, border);
        guiGraphics.fill(row.valueX(), row.y() + 18, row.valueX() + VALUE_WIDTH, row.y() + 19, border);
        guiGraphics.fill(row.valueX(), row.y() + 3, row.valueX() + 1, row.y() + 19, border);
        guiGraphics.fill(row.valueX() + VALUE_WIDTH - 1, row.y() + 3, row.valueX() + VALUE_WIDTH, row.y() + 19, border);
        this.drawCenteredText(guiGraphics, value, row.valueX(), row.y() + 3, VALUE_WIDTH, 16, hovered ? ACTIVE_COLOR : TEXT_COLOR);
    }

    private void drawStepperRow(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row, String label, String value) {
        guiGraphics.fill(row.x(), row.y(), row.x() + row.width(), row.y() + ROW_HEIGHT, 0x22101719);
        guiGraphics.fill(row.x(), row.y(), row.x() + 2, row.y() + ROW_HEIGHT, 0x668CAEA8);
        this.drawText(guiGraphics, label, row.x() + 10, row.y() + 8, TEXT_COLOR, false);

        this.drawSmallButton(guiGraphics, this.getMinusButtonX(row), row.y() + 3, "-1", this.isInside(mouseX, mouseY, this.getMinusButtonX(row), row.y() + 3, 16, 16));
        this.drawSmallButton(guiGraphics, this.getPlusButtonX(row), row.y() + 3, "+1", this.isInside(mouseX, mouseY, this.getPlusButtonX(row), row.y() + 3, 16, 16));
        this.drawValueField(guiGraphics, this.getStepperValueX(row), row.y() + 3, this.getStepperValueWidth(), 16, value, this.isInside(mouseX, mouseY, this.getStepperValueX(row), row.y() + 3, this.getStepperValueWidth(), 16));
    }

    private void drawSliderRow(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row, String label, int value) {
        boolean hovered = this.isInside(mouseX, mouseY, row.valueX(), row.y() + 5, VALUE_WIDTH, 12);
        int trackX = row.valueX();
        int trackY = row.y() + 10;
        int trackWidth = VALUE_WIDTH;
        int fillWidth = Math.round(trackWidth * value / 100.0F);
        int knobX = trackX + fillWidth;

        guiGraphics.fill(row.x(), row.y(), row.x() + row.width(), row.y() + ROW_HEIGHT, 0x22101719);
        guiGraphics.fill(row.x(), row.y(), row.x() + 2, row.y() + ROW_HEIGHT, 0x668CAEA8);
        this.drawText(guiGraphics, label, row.x() + 10, row.y() + 8, TEXT_COLOR, false);

        guiGraphics.fill(trackX, trackY, trackX + trackWidth, trackY + 3, hovered || this.draggingRotationSlider ? 0xAA5E6666 : 0x8A4A5050);
        guiGraphics.fill(trackX, trackY, trackX + fillWidth, trackY + 3, ACCENT);
        guiGraphics.fill(knobX - 1, trackY - 3, knobX + 2, trackY + 6, hovered || this.draggingRotationSlider ? 0xFFFFFFFF : 0xFFDDE8E8);
        this.drawText(guiGraphics, value + "%", trackX + trackWidth + 6, row.y() + 8, hovered || this.draggingRotationSlider ? ACTIVE_COLOR : TEXT_COLOR, false);
    }

    private void drawSmallButton(GuiGraphics guiGraphics, int x, int y, String label, boolean hovered) {
        this.drawSimpleButton(guiGraphics, x, y, 16, 16, label, hovered);
    }

    private void drawValueField(GuiGraphics guiGraphics, int x, int y, int width, int height, String value, boolean hovered) {
        int fill = hovered ? 0xAA5E6666 : 0x8A4A5050;
        int border = hovered ? 0xE8FFFFFF : 0xBFFFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
        this.drawCenteredText(guiGraphics, value, x, y, width, height, hovered ? ACTIVE_COLOR : TEXT_COLOR);
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

    private void startEditing(Field field) {
        if (this.editingField == field) {
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
            case RADIUS -> BuilderHelperClientConfig.setRandomPlacementRadius(value);
            case HEIGHT_MIN -> BuilderHelperClientConfig.setRandomPlacementHeightMin(value);
            case HEIGHT_MAX -> BuilderHelperClientConfig.setRandomPlacementHeightMax(value);
            case COUNT -> BuilderHelperClientConfig.setRandomPlacementCount(value);
            case ROTATION_CHANCE -> BuilderHelperClientConfig.setRandomPlacementRotationChance(value);
        }

        this.editingField = null;
        this.editingValue = "";
    }

    private int parseEditingValue(String value, int fallback) {
        if (value == null || value.isBlank() || value.equals("-")) {
            return fallback;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String getFieldDisplayValue(Field field) {
        if (this.editingField == field) {
            return this.editingValue + "_";
        }

        return Integer.toString(this.getFieldValue(field));
    }

    private int getFieldValue(Field field) {
        return switch (field) {
            case RADIUS -> BuilderHelperClientConfig.getRandomPlacementRadius();
            case HEIGHT_MIN -> BuilderHelperClientConfig.getRandomPlacementHeightMin();
            case HEIGHT_MAX -> BuilderHelperClientConfig.getRandomPlacementHeightMax();
            case COUNT -> BuilderHelperClientConfig.getRandomPlacementCount();
            case ROTATION_CHANCE -> BuilderHelperClientConfig.getRandomPlacementRotationChance();
        };
    }

    private Row getRow(Layout layout, int index) {
        int y = layout.listY() + index * (ROW_HEIGHT + ROW_GAP);
        int valueX = layout.x() + layout.width() - VALUE_WIDTH - 10;
        return new Row(layout.x() + 6, y, layout.width() - 12, valueX);
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

    private void updateDragging(int mouseX) {
        if (!this.draggingRotationSlider) {
            return;
        }

        if (GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            this.draggingRotationSlider = false;
            return;
        }

        this.setRotationChanceFromMouse(this.getRow(this.createLayout(), Field.ROTATION_CHANCE.ordinal()), mouseX);
    }

    private void setRotationChanceFromMouse(Row row, int mouseX) {
        double progress = (mouseX - row.valueX()) / (double) VALUE_WIDTH;
        int value = (int) Math.round(this.clamp(progress, 0.0, 1.0) * 100.0);
        BuilderHelperClientConfig.setRandomPlacementRotationChance(value);
    }

    private void adjustField(Field field, int amount) {
        int value = this.getFieldValue(field) + amount;

        switch (field) {
            case HEIGHT_MIN -> BuilderHelperClientConfig.setRandomPlacementHeightMin(value);
            case HEIGHT_MAX -> BuilderHelperClientConfig.setRandomPlacementHeightMax(value);
            default -> {
            }
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

    private boolean isHeightField(Field field) {
        return field == Field.HEIGHT_MIN || field == Field.HEIGHT_MAX;
    }

    private int getMinusButtonX(Row row) {
        return row.valueX();
    }

    private int getStepperValueX(Row row) {
        return row.valueX() + 20;
    }

    private int getStepperValueWidth() {
        return VALUE_WIDTH - 40;
    }

    private int getPlusButtonX(Row row) {
        return row.valueX() + VALUE_WIDTH - 16;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Layout(int x, int titleY, int panelY, int width, int panelHeight, int listY, int backX, int backY) {
    }

    private record Row(int x, int y, int width, int valueX) {
    }

    private enum Field {
        RADIUS("iterablock.gui.random_config.option.radius", false),
        HEIGHT_MIN("iterablock.gui.random_config.option.height_min", true),
        HEIGHT_MAX("iterablock.gui.random_config.option.height_max", true),
        COUNT("iterablock.gui.random_config.option.count", false),
        ROTATION_CHANCE("iterablock.gui.random_config.option.rotation_chance", false);

        private final String labelKey;
        private final boolean allowsNegative;

        Field(String labelKey, boolean allowsNegative) {
            this.labelKey = labelKey;
            this.allowsNegative = allowsNegative;
        }

        private String labelKey() {
            return this.labelKey;
        }

        private boolean allowsNegative() {
            return this.allowsNegative;
        }
    }
}

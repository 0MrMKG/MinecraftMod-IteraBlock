package com.iterablock.client.gui;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiRandomPlacementConfig extends GuiBase {
    private static final int SAFE_MARGIN = 22;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_GAP = 2;
    private static final int TAB_HEIGHT = 18;
    private static final int PREFERRED_TAB_WIDTH = 88;
    private static final int PREFERRED_LABEL_WIDTH = 100;
    private static final int PREFERRED_VALUE_WIDTH = 96;
    private static final int SLIDER_LABEL_WIDTH = 24;
    private static final int CONTROL_HEIGHT = 14;
    private static final int STEPPER_BUTTON_WIDTH = 14;
    private static final int RESET_WIDTH = 42;
    private static final int BACK_WIDTH = 58;
    private static final int BACK_HEIGHT = 16;
    private static final double TEXT_SCALE = 0.52;
    private static final int TITLE_COLOR = 0xFFF3EECF;
    private static final int TEXT_COLOR = 0xFFDDE8E8;
    private static final int MUTED_COLOR = 0xFF91A0A3;
    private static final int FRAME_RGB = 0xF4F7F7;

    private final boolean returnToMainMenu;
    private Field editingField;
    private String editingValue = "";
    private boolean draggingRotationSlider;
    private boolean draggingBlockReplaceSlider;
    private RandomConfigTab selectedTab = RandomConfigTab.RANDOM_NBT;

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
        this.drawTabs(guiGraphics, mouseX, mouseY, layout);
        this.updateDragging(mouseX);
        if (this.selectedTab == RandomConfigTab.RANDOM_NBT) {
            this.drawRows(guiGraphics, mouseX, mouseY, layout);
        } else {
            this.drawBlockConfig(guiGraphics, mouseX, mouseY, layout);
        }
        this.drawBackButton(guiGraphics, mouseX, mouseY, layout);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        Layout layout = this.createLayout();

        for (RandomConfigTab tab : RandomConfigTab.values()) {
            if (this.isInside(mouseX, mouseY, layout.tabX(tab), layout.tabY(), layout.tabWidth(tab), TAB_HEIGHT)) {
                this.commitEditingValue();
                this.draggingRotationSlider = false;
                this.draggingBlockReplaceSlider = false;
                this.selectedTab = tab;
                return true;
            }
        }

        if (this.isInside(mouseX, mouseY, layout.backX(), layout.backY(), BACK_WIDTH, BACK_HEIGHT)) {
            this.commitEditingValue();
            this.returnToMainMenu();
            return true;
        }

        if (this.selectedTab == RandomConfigTab.RANDOM_BLOCK) {
            Row replaceChanceRow = this.getRow(layout, 0);
            if (this.isInside(mouseX, mouseY, replaceChanceRow.valueX(), replaceChanceRow.y() + 1,
                    replaceChanceRow.valueWidth(), CONTROL_HEIGHT)) {
                this.draggingBlockReplaceSlider = true;
                this.setBlockReplaceChanceFromMouse(replaceChanceRow, mouseX);
                return true;
            }

            Row includeAirRow = this.getRow(layout, 1);
            if (this.isInside(mouseX, mouseY, includeAirRow.valueX(), includeAirRow.y() + 1,
                    includeAirRow.valueWidth(), CONTROL_HEIGHT)) {
                BuilderHelperClientConfig.setRandomAreaIncludeAir(!BuilderHelperClientConfig.isRandomAreaIncludeAir());
                return true;
            }

            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        for (Field field : Field.values()) {
            Row row = this.getRow(layout, field.ordinal());

            if (this.isInside(mouseX, mouseY, row.resetX(), row.y() + 1, RESET_WIDTH, CONTROL_HEIGHT)) {
                this.commitEditingValue();
                this.resetField(field);
                return true;
            }

            if (field == Field.ROTATION_CHANCE && this.isInside(mouseX, mouseY, row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT)) {
                this.commitEditingValue();
                this.draggingRotationSlider = true;
                this.setRotationChanceFromMouse(row, mouseX);
                return true;
            }

            if (this.isHeightField(field)) {
                if (this.isInside(mouseX, mouseY, this.getMinusButtonX(row), row.y() + 1, STEPPER_BUTTON_WIDTH, CONTROL_HEIGHT)) {
                    this.commitEditingValue();
                    this.adjustField(field, -1);
                    return true;
                }

                if (this.isInside(mouseX, mouseY, this.getPlusButtonX(row), row.y() + 1, STEPPER_BUTTON_WIDTH, CONTROL_HEIGHT)) {
                    this.commitEditingValue();
                    this.adjustField(field, 1);
                    return true;
                }
            }

            if (this.isInside(mouseX, mouseY, row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT)) {
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
        if (mouseButton == 0 && (this.draggingRotationSlider || this.draggingBlockReplaceSlider)) {
            this.draggingRotationSlider = false;
            this.draggingBlockReplaceSlider = false;
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
            this.draggingBlockReplaceSlider = false;
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
        guiGraphics.fill(layout.x(), layout.panelY(), layout.x() + 1, layout.panelY() + layout.panelHeight(), 0x66FFFFFF);
        guiGraphics.fill(layout.x() + layout.width() - 1, layout.panelY(), layout.x() + layout.width(), layout.panelY() + layout.panelHeight(), 0x66FFFFFF);
        guiGraphics.fill(layout.x(), layout.panelY() + layout.panelHeight() - 1, layout.x() + layout.width(), layout.panelY() + layout.panelHeight(), 0x66FFFFFF);
        guiGraphics.fill(layout.x(), layout.bodyY(), layout.x() + layout.width(), layout.bodyY() + 1, 0x553D5558);
    }

    private void drawTabs(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        for (RandomConfigTab tab : RandomConfigTab.values()) {
            int x = layout.tabX(tab);
            boolean selected = this.selectedTab == tab;
            boolean hovered = this.isInside(mouseX, mouseY, x, layout.tabY(), layout.tabWidth(tab), TAB_HEIGHT);
            int fill = selected
                    ? 0x7A0B1012
                    : this.withAlpha(this.blendRgb(0x111719, 0x273033, hovered ? 1.0F : 0.0F), hovered ? 0.74F : 0.58F);
            int frameColor = this.withAlpha(FRAME_RGB, selected ? 0.62F : hovered ? 0.50F : 0.26F);
            int textColor = selected || hovered ? 0xFFFFF1B0 : TEXT_COLOR;

            guiGraphics.fill(x, layout.tabY(), x + layout.tabWidth(tab), layout.tabY() + TAB_HEIGHT, fill);
            guiGraphics.fill(x, layout.tabY(), x + layout.tabWidth(tab), layout.tabY() + 1, frameColor);
            guiGraphics.fill(x, layout.tabY(), x + 1, layout.tabY() + TAB_HEIGHT, frameColor);
            guiGraphics.fill(x + layout.tabWidth(tab) - 1, layout.tabY(), x + layout.tabWidth(tab), layout.tabY() + TAB_HEIGHT, frameColor);
            if (!selected) {
                guiGraphics.fill(x, layout.tabY() + TAB_HEIGHT - 1, x + layout.tabWidth(tab), layout.tabY() + TAB_HEIGHT, frameColor);
            }

            String label = this.fitText(Lang.tr(tab.labelKey()), layout.tabWidth(tab) - 6);
            this.drawCenteredText(guiGraphics, label, x, layout.tabY(), layout.tabWidth(tab), TAB_HEIGHT, textColor);
        }

        // The selected tab shares the body fill, so its lower edge visually opens into the list.
        int selectedX = layout.tabX(this.selectedTab);
        guiGraphics.fill(selectedX + 1, layout.tabY() + TAB_HEIGHT - 1,
                selectedX + layout.tabWidth(this.selectedTab) - 1, layout.tabY() + TAB_HEIGHT + 1, 0x7A0B1012);
    }

    private void drawBlockConfig(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        Row replaceChanceRow = this.getRow(layout, 0);
        this.drawBlockReplaceSlider(guiGraphics, mouseX, mouseY, replaceChanceRow);

        Row includeAirRow = this.getRow(layout, 1);
        boolean includeAir = BuilderHelperClientConfig.isRandomAreaIncludeAir();
        this.drawLabelField(guiGraphics, includeAirRow, "包含空气");
        this.drawValueField(guiGraphics, includeAirRow.valueX(), includeAirRow.y() + 1,
                includeAirRow.valueWidth(), CONTROL_HEIGHT, includeAir ? "开启" : "关闭",
                this.isInside(mouseX, mouseY, includeAirRow.valueX(), includeAirRow.y() + 1,
                        includeAirRow.valueWidth(), CONTROL_HEIGHT));
    }

    private void drawBlockReplaceSlider(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row) {
        int value = BuilderHelperClientConfig.getRandomAreaReplaceChance();
        boolean hovered = this.isInside(mouseX, mouseY, row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT);
        int trackX = row.valueX();
        int trackY = row.y() + 7;
        int trackWidth = this.getRotationTrackWidth(row);
        int fillWidth = Math.round(trackWidth * value / 100.0F);
        int knobX = trackX + fillWidth;

        this.drawLabelField(guiGraphics, row, "放置方块替换比例");
        guiGraphics.fill(trackX, trackY, trackX + trackWidth, trackY + 3,
                hovered || this.draggingBlockReplaceSlider ? 0xAA5E6666 : 0x8A4A5050);
        guiGraphics.fill(trackX, trackY, trackX + fillWidth, trackY + 3, 0xBFFFFFFF);
        guiGraphics.fill(knobX - 1, trackY - 3, knobX + 2, trackY + 6,
                hovered || this.draggingBlockReplaceSlider ? 0xFFFFFFFF : 0xFFDDE8E8);
        this.drawCenteredText(guiGraphics, value + "%", trackX + trackWidth + 4, row.y(), SLIDER_LABEL_WIDTH,
                ROW_HEIGHT, TEXT_COLOR);
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
        boolean hovered = clickable && this.isInside(mouseX, mouseY, row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT);
        this.drawLabelField(guiGraphics, row, label);
        this.drawValueField(guiGraphics, row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT, value, hovered);
        this.drawResetButton(guiGraphics, mouseX, mouseY, row);
    }

    private void drawStepperRow(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row, String label, String value) {
        this.drawLabelField(guiGraphics, row, label);

        this.drawSmallButton(guiGraphics, this.getMinusButtonX(row), row.y() + 1, "-1", this.isInside(mouseX, mouseY, this.getMinusButtonX(row), row.y() + 1, STEPPER_BUTTON_WIDTH, CONTROL_HEIGHT));
        this.drawSmallButton(guiGraphics, this.getPlusButtonX(row), row.y() + 1, "+1", this.isInside(mouseX, mouseY, this.getPlusButtonX(row), row.y() + 1, STEPPER_BUTTON_WIDTH, CONTROL_HEIGHT));
        this.drawValueField(guiGraphics, this.getStepperValueX(row), row.y() + 1, this.getStepperValueWidth(row), CONTROL_HEIGHT, value, this.isInside(mouseX, mouseY, this.getStepperValueX(row), row.y() + 1, this.getStepperValueWidth(row), CONTROL_HEIGHT));
        this.drawResetButton(guiGraphics, mouseX, mouseY, row);
    }

    private void drawSliderRow(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row, String label, int value) {
        boolean hovered = this.isInside(mouseX, mouseY, row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT);
        int trackX = row.valueX();
        int trackY = row.y() + 7;
        int trackWidth = this.getRotationTrackWidth(row);
        int fillWidth = Math.round(trackWidth * value / 100.0F);
        int knobX = trackX + fillWidth;

        this.drawLabelField(guiGraphics, row, label);

        guiGraphics.fill(trackX, trackY, trackX + trackWidth, trackY + 3, hovered || this.draggingRotationSlider ? 0xAA5E6666 : 0x8A4A5050);
        guiGraphics.fill(trackX, trackY, trackX + fillWidth, trackY + 3, 0xBFFFFFFF);
        guiGraphics.fill(knobX - 1, trackY - 3, knobX + 2, trackY + 6, hovered || this.draggingRotationSlider ? 0xFFFFFFFF : 0xFFDDE8E8);
        this.drawCenteredText(guiGraphics, value + "%", trackX + trackWidth + 4, row.y(), SLIDER_LABEL_WIDTH,
                ROW_HEIGHT, TEXT_COLOR);
        this.drawResetButton(guiGraphics, mouseX, mouseY, row);
    }

    private void drawLabelField(GuiGraphics guiGraphics, Row row, String label) {
        this.drawValueField(guiGraphics, row.x(), row.y() + 1, row.labelWidth(), CONTROL_HEIGHT,
                this.fitText(label, row.labelWidth() - 8), false);
    }

    private void drawResetButton(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row) {
        boolean hovered = this.isInside(mouseX, mouseY, row.resetX(), row.y() + 1, RESET_WIDTH, CONTROL_HEIGHT);
        this.drawSimpleButton(guiGraphics, row.resetX(), row.y() + 1, RESET_WIDTH, CONTROL_HEIGHT,
                Lang.tr("iterablock.gui.settings.reset"), hovered);
    }

    private void drawSmallButton(GuiGraphics guiGraphics, int x, int y, String label, boolean hovered) {
        this.drawSimpleButton(guiGraphics, x, y, STEPPER_BUTTON_WIDTH, CONTROL_HEIGHT, label, hovered);
    }

    private void drawValueField(GuiGraphics guiGraphics, int x, int y, int width, int height, String value, boolean hovered) {
        int fill = hovered ? 0xAA5E6666 : 0x8A4A5050;
        int border = hovered ? 0xE8FFFFFF : 0xBFFFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
        this.drawCenteredText(guiGraphics, value, x, y, width, height, TEXT_COLOR);
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
        this.drawCenteredText(guiGraphics, label, x, y, width, height, 0xFFFFFFFF);
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
        int rowX = layout.x() + 8;
        int resetX = layout.x() + layout.width() - RESET_WIDTH - 8;
        int labelWidth = Math.max(52, Math.min(PREFERRED_LABEL_WIDTH, resetX - rowX - 64));
        int valueX = rowX + labelWidth + 6;
        int valueWidth = Math.max(42, resetX - valueX - 6);
        return new Row(rowX, y, resetX - rowX, labelWidth, valueX, valueWidth, resetX);
    }

    private Layout createLayout() {
        int horizontalMargin = Math.min(SAFE_MARGIN, Math.max(4, (this.width - 180) / 2));
        int availableWidth = Math.max(1, this.width - horizontalMargin * 2);
        int width = Math.min(availableWidth, 260);
        int x = (this.width - width) / 2;
        int titleY = Math.max(10, SAFE_MARGIN);
        int panelY = titleY + 20;
        int desiredPanelHeight = TAB_HEIGHT + 8 + Field.values().length * ROW_HEIGHT
                + (Field.values().length - 1) * ROW_GAP + 8;
        int maximumPanelHeight = Math.max(TAB_HEIGHT + 16,
                this.height - SAFE_MARGIN - BACK_HEIGHT - panelY - 10);
        int panelHeight = Math.min(desiredPanelHeight, maximumPanelHeight);
        int listY = panelY + TAB_HEIGHT + 8;
        int backY = Math.min(this.height - SAFE_MARGIN - BACK_HEIGHT, panelY + panelHeight + 8);
        int backX = x + width - BACK_WIDTH;
        int tabWidth = Math.max(1, Math.min(PREFERRED_TAB_WIDTH, width / RandomConfigTab.values().length));
        int valueWidth = Math.max(72, Math.min(PREFERRED_VALUE_WIDTH, width / 3));
        return new Layout(x, titleY, panelY, width, panelHeight, listY, panelY,
                tabWidth, valueWidth, backX, backY);
    }

    private void returnToMainMenu() {
        this.closeGui(true);
        if (this.returnToMainMenu) {
            GuiBase.openGui(new GuiBuilderHelperMainMenu());
        }
    }

    private void updateDragging(int mouseX) {
        if (!this.draggingRotationSlider && !this.draggingBlockReplaceSlider) {
            return;
        }

        if (GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            this.draggingRotationSlider = false;
            this.draggingBlockReplaceSlider = false;
            return;
        }

        Layout layout = this.createLayout();
        if (this.draggingRotationSlider) {
            this.setRotationChanceFromMouse(this.getRow(layout, Field.ROTATION_CHANCE.ordinal()), mouseX);
        }
        if (this.draggingBlockReplaceSlider) {
            this.setBlockReplaceChanceFromMouse(this.getRow(layout, 0), mouseX);
        }
    }

    private void setRotationChanceFromMouse(Row row, int mouseX) {
        double progress = (mouseX - row.valueX()) / (double) this.getRotationTrackWidth(row);
        int value = (int) Math.round(this.clamp(progress, 0.0, 1.0) * 100.0);
        BuilderHelperClientConfig.setRandomPlacementRotationChance(value);
    }

    private void setBlockReplaceChanceFromMouse(Row row, int mouseX) {
        double progress = (mouseX - row.valueX()) / (double) this.getRotationTrackWidth(row);
        int value = (int) Math.round(this.clamp(progress, 0.0, 1.0) * 100.0);
        BuilderHelperClientConfig.setRandomAreaReplaceChance(value);
    }

    private int getRotationTrackWidth(Row row) {
        return Math.max(24, row.valueWidth() - SLIDER_LABEL_WIDTH - 4);
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

    private void resetField(Field field) {
        switch (field) {
            case RADIUS -> BuilderHelperClientConfig.setRandomPlacementRadius(BuilderHelperClientConfig.DEFAULT_RANDOM_PLACEMENT_RADIUS);
            case HEIGHT_MIN -> BuilderHelperClientConfig.setRandomPlacementHeightMin(BuilderHelperClientConfig.DEFAULT_RANDOM_PLACEMENT_HEIGHT_MIN);
            case HEIGHT_MAX -> BuilderHelperClientConfig.setRandomPlacementHeightMax(BuilderHelperClientConfig.DEFAULT_RANDOM_PLACEMENT_HEIGHT_MAX);
            case COUNT -> BuilderHelperClientConfig.setRandomPlacementCount(BuilderHelperClientConfig.DEFAULT_RANDOM_PLACEMENT_COUNT);
            case ROTATION_CHANCE -> BuilderHelperClientConfig.setRandomPlacementRotationChance(BuilderHelperClientConfig.DEFAULT_RANDOM_PLACEMENT_ROTATION_CHANCE);
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

    private boolean isHeightField(Field field) {
        return field == Field.HEIGHT_MIN || field == Field.HEIGHT_MAX;
    }

    private int getMinusButtonX(Row row) {
        return row.valueX();
    }

    private int getStepperValueX(Row row) {
        return row.valueX() + STEPPER_BUTTON_WIDTH + 4;
    }

    private int getStepperValueWidth(Row row) {
        return Math.max(20, row.valueWidth() - STEPPER_BUTTON_WIDTH * 2 - 8);
    }

    private int getPlusButtonX(Row row) {
        return row.valueX() + row.valueWidth() - STEPPER_BUTTON_WIDTH;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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

    private record Layout(int x, int titleY, int panelY, int width, int panelHeight, int listY, int tabY,
                          int tabWidth, int valueWidth, int backX, int backY) {
        private int bodyY() {
            return this.panelY + TAB_HEIGHT;
        }

        private int tabX(RandomConfigTab tab) {
            return this.x + tab.ordinal() * this.tabWidth;
        }

        private int tabWidth(RandomConfigTab tab) {
            return this.tabWidth;
        }
    }

    private record Row(int x, int y, int width, int labelWidth, int valueX, int valueWidth, int resetX) {
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

    private enum RandomConfigTab {
        RANDOM_BLOCK("iterablock.gui.random_config.tab.block"),
        RANDOM_NBT("iterablock.gui.random_config.tab.nbt");

        private final String labelKey;

        RandomConfigTab(String labelKey) {
            this.labelKey = labelKey;
        }

        private String labelKey() {
            return this.labelKey;
        }
    }
}

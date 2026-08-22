package com.iterablock.client.gui;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.gui.settings.GuiSettingsControls;
import com.iterablock.client.gui.settings.SettingsMenuCommonRenderer;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiRandomPlacementConfig extends GuiBase {
    private static final int SAFE_MARGIN = 22;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_GAP = 2;
    private static final int TAB_HEIGHT = 18;
    private static final int PREFERRED_LABEL_WIDTH = 100;
    private static final int PREFERRED_VALUE_WIDTH = 96;
    private static final int CONTROL_HEIGHT = 14;
    private static final int STEPPER_BUTTON_WIDTH = 14;
    private static final int RESET_WIDTH = 42;
    private static final int BACK_WIDTH = 58;
    private static final int BACK_HEIGHT = 16;
    private static final double TEXT_SCALE = 0.52;
    private static final double HOVER_SPEED = 8.0;
    private static final int TEXT_COLOR = 0xFFDDE8E8;

    private final boolean returnToMainMenu;
    private final double[] tabHover = new double[RandomConfigTab.values().length];
    private final double[][] tabIndicator = new double[RandomConfigTab.values().length][SettingsMenuCommonRenderer.INDICATOR_SEGMENTS];
    private final double[][] tabIndicatorVelocity = new double[RandomConfigTab.values().length][SettingsMenuCommonRenderer.INDICATOR_SEGMENTS];
    private Field editingField;
    private String editingValue = "";
    private boolean draggingRotationSlider;
    private boolean draggingBlockReplaceSlider;
    private RandomConfigTab selectedTab = RandomConfigTab.RANDOM_BLOCK;
    private long lastFrameNanos;

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
        SettingsMenuCommonRenderer.drawBackground(guiGraphics, this.width, this.height);
    }

    @Override
    protected void drawTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void drawContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Layout layout = this.createLayout();
        this.updateTabAnimations(mouseX, mouseY, layout);

        SettingsMenuCommonRenderer.drawTitle(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.random_config.title"), layout.x(), layout.titleY());
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
        SettingsMenuCommonRenderer.drawPanel(guiGraphics, layout.x(), layout.panelY(), layout.width(), layout.panelHeight(), layout.bodyY());
    }

    private void drawTabs(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        for (RandomConfigTab tab : RandomConfigTab.values()) {
            int x = layout.tabX(tab);
            boolean selected = this.selectedTab == tab;
            boolean hovered = this.isInside(mouseX, mouseY, x, layout.tabY(), layout.tabWidth(tab), TAB_HEIGHT);
            boolean pressed = hovered && this.isLeftMouseDown();
            SettingsMenuCommonRenderer.drawTab(guiGraphics, this.textRenderer, Lang.tr(tab.labelKey()),
                    x, layout.tabY(), layout.tabWidth(tab), TAB_HEIGHT, selected, pressed,
                    this.easeOutCubic(this.tabHover[tab.ordinal()]), this.tabIndicator[tab.ordinal()]);
        }
    }

    private void updateTabAnimations(int mouseX, int mouseY, Layout layout) {
        long now = System.nanoTime();
        double deltaTime = this.lastFrameNanos == 0L ? 1.0 / 60.0 : (now - this.lastFrameNanos) / 1_000_000_000.0;
        this.lastFrameNanos = now;
        deltaTime = Math.max(0.0, Math.min(0.05, deltaTime));

        for (RandomConfigTab tab : RandomConfigTab.values()) {
            int index = tab.ordinal();
            double target = this.isInside(mouseX, mouseY, layout.tabX(tab), layout.tabY(), layout.tabWidth(tab), TAB_HEIGHT)
                    ? 1.0
                    : 0.0;
            this.tabHover[index] = this.approach(this.tabHover[index], target, HOVER_SPEED, deltaTime);
            SettingsMenuCommonRenderer.updateTabIndicatorPid(this.tabIndicator[index], this.tabIndicatorVelocity[index],
                    tab == this.selectedTab ? SettingsMenuCommonRenderer.TabIndicatorState.SELECTED
                            : target > 0.0 ? SettingsMenuCommonRenderer.TabIndicatorState.HOVERED
                            : SettingsMenuCommonRenderer.TabIndicatorState.NORMAL,
                    deltaTime);
        }
    }

    private void drawBlockConfig(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        Row replaceChanceRow = this.getRow(layout, 0);
        this.drawBlockReplaceSlider(guiGraphics, mouseX, mouseY, replaceChanceRow);

        Row includeAirRow = this.getRow(layout, 1);
        boolean includeAir = BuilderHelperClientConfig.isRandomAreaIncludeAir();
        GuiSettingsControls.drawLabel(guiGraphics, this.textRenderer, this.fitText("包含空气", includeAirRow.labelWidth() - 8),
                includeAirRow.x(), includeAirRow.y() + 1, includeAirRow.labelWidth(), CONTROL_HEIGHT);
        GuiSettingsControls.drawValueField(guiGraphics, this.textRenderer, includeAir ? "开启" : "关闭",
                includeAirRow.valueX(), includeAirRow.y() + 1, includeAirRow.valueWidth(), CONTROL_HEIGHT,
                this.isInside(mouseX, mouseY, includeAirRow.valueX(), includeAirRow.y() + 1,
                        includeAirRow.valueWidth(), CONTROL_HEIGHT) ? 1.0 : 0.0, TEXT_COLOR);
    }

    private void drawBlockReplaceSlider(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row) {
        int value = BuilderHelperClientConfig.getRandomAreaReplaceChance();
        boolean hovered = this.isInside(mouseX, mouseY, row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT);
        GuiSettingsControls.drawLabel(guiGraphics, this.textRenderer, this.fitText("放置方块替换比例", row.labelWidth() - 8),
                row.x(), row.y() + 1, row.labelWidth(), CONTROL_HEIGHT);
        GuiSettingsControls.drawSlider(guiGraphics, this.textRenderer, value, 0, 100,
                row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT,
                hovered || this.draggingBlockReplaceSlider ? 1.0 : 0.0);
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
        GuiSettingsControls.drawLabel(guiGraphics, this.textRenderer, this.fitText(label, row.labelWidth() - 8),
                row.x(), row.y() + 1, row.labelWidth(), CONTROL_HEIGHT);
        GuiSettingsControls.drawValueField(guiGraphics, this.textRenderer, value,
                row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT, hovered ? 1.0 : 0.0, TEXT_COLOR);
        this.drawResetButton(guiGraphics, mouseX, mouseY, row);
    }

    private void drawStepperRow(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row, String label, String value) {
        GuiSettingsControls.drawLabel(guiGraphics, this.textRenderer, this.fitText(label, row.labelWidth() - 8),
                row.x(), row.y() + 1, row.labelWidth(), CONTROL_HEIGHT);

        this.drawSmallButton(guiGraphics, this.getMinusButtonX(row), row.y() + 1, "-1", this.isInside(mouseX, mouseY, this.getMinusButtonX(row), row.y() + 1, STEPPER_BUTTON_WIDTH, CONTROL_HEIGHT));
        this.drawSmallButton(guiGraphics, this.getPlusButtonX(row), row.y() + 1, "+1", this.isInside(mouseX, mouseY, this.getPlusButtonX(row), row.y() + 1, STEPPER_BUTTON_WIDTH, CONTROL_HEIGHT));
        GuiSettingsControls.drawValueField(guiGraphics, this.textRenderer, value,
                this.getStepperValueX(row), row.y() + 1, this.getStepperValueWidth(row), CONTROL_HEIGHT,
                this.isInside(mouseX, mouseY, this.getStepperValueX(row), row.y() + 1, this.getStepperValueWidth(row), CONTROL_HEIGHT) ? 1.0 : 0.0,
                TEXT_COLOR);
        this.drawResetButton(guiGraphics, mouseX, mouseY, row);
    }

    private void drawSliderRow(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row, String label, int value) {
        boolean hovered = this.isInside(mouseX, mouseY, row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT);
        GuiSettingsControls.drawLabel(guiGraphics, this.textRenderer, this.fitText(label, row.labelWidth() - 8),
                row.x(), row.y() + 1, row.labelWidth(), CONTROL_HEIGHT);
        GuiSettingsControls.drawSlider(guiGraphics, this.textRenderer, value, 0, 100,
                row.valueX(), row.y() + 1, row.valueWidth(), CONTROL_HEIGHT,
                hovered || this.draggingRotationSlider ? 1.0 : 0.0);
        this.drawResetButton(guiGraphics, mouseX, mouseY, row);
    }

    private void drawResetButton(GuiGraphics guiGraphics, int mouseX, int mouseY, Row row) {
        boolean hovered = this.isInside(mouseX, mouseY, row.resetX(), row.y() + 1, RESET_WIDTH, CONTROL_HEIGHT);
        GuiSettingsControls.drawButton(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.settings.reset"),
                row.resetX(), row.y() + 1, RESET_WIDTH, CONTROL_HEIGHT, hovered ? 1.0 : 0.0);
    }

    private void drawSmallButton(GuiGraphics guiGraphics, int x, int y, String label, boolean hovered) {
        GuiSettingsControls.drawButton(guiGraphics, this.textRenderer, label,
                x, y, STEPPER_BUTTON_WIDTH, CONTROL_HEIGHT, hovered ? 1.0 : 0.0);
    }

    private void drawBackButton(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        boolean hovered = this.isInside(mouseX, mouseY, layout.backX(), layout.backY(), BACK_WIDTH, BACK_HEIGHT);
        GuiSettingsControls.drawButton(guiGraphics, this.textRenderer, Lang.tr("iterablock.gui.button.back"),
                layout.backX(), layout.backY(), BACK_WIDTH, BACK_HEIGHT, hovered ? 1.0 : 0.0);
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
        int tabWidth = Math.max(1, width / RandomConfigTab.values().length);
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
        BuilderHelperClientConfig.setRandomPlacementRotationChance(
                GuiSettingsControls.sliderValueFromMouse(mouseX, row.valueX(), row.valueWidth(), 0, 100));
    }

    private void setBlockReplaceChanceFromMouse(Row row, int mouseX) {
        BuilderHelperClientConfig.setRandomAreaReplaceChance(
                GuiSettingsControls.sliderValueFromMouse(mouseX, row.valueX(), row.valueWidth(), 0, 100));
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

    private double approach(double current, double target, double speed, double deltaTime) {
        double step = speed * deltaTime;
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    private double easeOutCubic(double value) {
        double inverse = 1.0 - value;
        return 1.0 - inverse * inverse * inverse;
    }

    private boolean isLeftMouseDown() {
        return GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return GuiSettingsControls.contains(mouseX, mouseY, x, y, width, height);
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

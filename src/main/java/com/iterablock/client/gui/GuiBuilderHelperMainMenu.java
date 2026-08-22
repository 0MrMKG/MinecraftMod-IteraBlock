package com.iterablock.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.iterablock.IteraBlock;
import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.gui.settings.GuiBuilderHelperSettings;
import com.iterablock.client.hotkeys.VanillaKeyMappings;
import com.iterablock.client.tool.SchematicPlacementState;
import com.iterablock.common.PlacementReplaceMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.glfw.GLFW;

public class GuiBuilderHelperMainMenu extends GuiBase {
    private static final int BASE_BUTTON_WIDTH = 252;
    private static final int BASE_BUTTON_HEIGHT = 38;
    private static final int BASE_COLUMN_GAP = 28;
    private static final int BASE_ROW_GAP = 8;
    private static final int BASE_GROUP_GAP = 16;
    private static final int BASE_SAFE_MARGIN = 34;
    private static final int BASE_AXIS_CARD_WIDTH = 70;
    private static final int BASE_AXIS_CARD_HEIGHT = 20;
    private static final int BASE_AXIS_GAP = 5;
    private static final int BASE_RESET_WIDTH = 58;
    private static final int BASE_RESET_HEIGHT = 16;
    private static final double HOVER_SPEED = 8.0;
    private static final double FEEDBACK_SPEED = 13.0;
    private static final double[] INDICATOR_EXPAND_SPEEDS = {13.0, 9.0, 5.5};
    private static final double[] INDICATOR_RETRACT_SPEEDS = {5.0, 8.0, 12.0};
    private static final int TITLE_COLOR = 0xFFF3EECF;
    private static final int TEXT_COLOR = 0xFFD8E3E3;
    private static final int ACTIVE_TEXT_COLOR = 0xFFFFFFB8;
    private static final int SIMPLE_BUTTON_FILL = 0x0F4A5050;
    private static final int SIMPLE_BUTTON_FILL_HOVER = 0x0F5E6666;
    private static final int SIMPLE_BUTTON_BORDER = 0xBFFFFFFF;
    private static final int SIMPLE_BUTTON_BORDER_HOVER = 0xE8FFFFFF;
    private static final int PANEL_FILL = 0x0F1A2225;
    private static final int PANEL_BORDER = 0xD9DCE6E4;
    private static final double PANEL_RADIUS = 9.0;
    private static final double BUTTON_RADIUS = 5.5;
    private static final int CORNER_SEGMENTS = 10;

    private final double[] hoverProgress = new double[MenuButton.values().length];
    private final double[][] indicatorProgress = new double[MenuButton.values().length][3];
    private final double[] minusHoverProgress = new double[SchematicPlacementState.Axis.values().length];
    private final double[] plusHoverProgress = new double[SchematicPlacementState.Axis.values().length];
    private final double[] minusPressProgress = new double[SchematicPlacementState.Axis.values().length];
    private final double[] plusPressProgress = new double[SchematicPlacementState.Axis.values().length];
    private final double[] valueFlashProgress = new double[SchematicPlacementState.Axis.values().length];
    private double resetHoverProgress;
    private double resetPressProgress;
    private double resetFlashProgress;
    private double backHoverProgress;
    private double backPressProgress;
    private long lastFrameNanos;
    private MenuButton selectedButton;

    public GuiBuilderHelperMainMenu() {
        this.setTitle(this.getMenuTitle());
    }

    @Override
    public void initGui() {
        this.clearElements();
    }

    @Override
    protected void drawScreenBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x0F808080);
    }

    @Override
    protected void drawTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void drawContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Layout layout = this.createLayout();
        List<ButtonPlacement> placements = this.getButtonPlacements(layout);

        this.updateHoverAnimations(mouseX, mouseY, placements);
        this.drawMainPanel(guiGraphics, layout);
        String title = this.getMenuTitle();
        int titleX = layout.panelX() + (int) Math.round((layout.panelWidth() - this.getStringWidth(title) * layout.titleScale()) / 2.0);
        this.drawScaledString(guiGraphics, title, titleX, layout.titleY() + 8, layout.titleScale(), TITLE_COLOR, true);

        for (ButtonPlacement placement : placements) {
            this.drawMenuButton(guiGraphics, mouseX, mouseY, placement, layout);
        }

        this.drawOverlapControls(guiGraphics, mouseX, mouseY, layout);
        this.drawBackButton(guiGraphics, layout);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        for (ButtonPlacement placement : this.getButtonPlacements(this.createLayout())) {
            if (placement.button().placeholder()) {
                continue;
            }

            if (this.isInside(mouseX, mouseY, placement.x(), placement.y(), placement.width(), placement.height())) {
                this.selectedButton = placement.button();
                this.runAction(placement.button());
                return true;
            }
        }

        Layout layout = this.createLayout();

        for (AxisStepperWidget stepper : this.getAxisSteppers(layout)) {
            int step = Screen.hasShiftDown() ? 10 : 1;

            if (this.isInside(mouseX, mouseY, stepper.minusX(), stepper.buttonY(), stepper.buttonSize(), stepper.buttonSize())) {
                this.adjustOverlap(stepper.axis(), -step);
                this.minusPressProgress[stepper.axis().ordinal()] = 1.0;
                return true;
            }

            if (this.isInside(mouseX, mouseY, stepper.plusX(), stepper.buttonY(), stepper.buttonSize(), stepper.buttonSize())) {
                this.adjustOverlap(stepper.axis(), step);
                this.plusPressProgress[stepper.axis().ordinal()] = 1.0;
                return true;
            }
        }

        ButtonWidget resetButton = this.getOverlapResetButton(layout);
        if (this.isInside(mouseX, mouseY, resetButton.x(), resetButton.y(), resetButton.width(), resetButton.height())) {
            SchematicPlacementState.resetOverlap();
            this.resetPressProgress = 1.0;
            this.resetFlashProgress = 1.0;

            for (SchematicPlacementState.Axis axis : SchematicPlacementState.Axis.values()) {
                this.valueFlashProgress[axis.ordinal()] = 1.0;
            }

            return true;
        }

        ButtonWidget backButton = this.getBackButton(layout);
        if (this.isInside(mouseX, mouseY, backButton.x(), backButton.y(), backButton.width(), backButton.height())) {
            this.backPressProgress = 1.0;
            this.closeGui(true);
            return true;
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseScrolled(int mouseX, int mouseY, double amount, double amountHorizontal) {
        if (amount == 0.0) {
            return false;
        }

        int step = Screen.hasShiftDown() ? 10 : 1;

        for (AxisStepperWidget stepper : this.getAxisSteppers(this.createLayout())) {
            if (this.isInside(mouseX, mouseY, stepper.x(), stepper.y(), stepper.width(), stepper.height())) {
                this.adjustOverlap(stepper.axis(), amount > 0.0 ? step : -step);
                return true;
            }
        }

        return super.onMouseScrolled(mouseX, mouseY, amount, amountHorizontal);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (VanillaKeyMappings.matchesOpenMainMenu(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.closeGui(true);
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    private void updateHoverAnimations(int mouseX, int mouseY, List<ButtonPlacement> placements) {
        long now = System.nanoTime();
        double deltaTime = this.lastFrameNanos == 0L ? 1.0 / 60.0 : (now - this.lastFrameNanos) / 1_000_000_000.0;
        this.lastFrameNanos = now;
        deltaTime = Math.max(0.0, Math.min(0.05, deltaTime));

        for (ButtonPlacement placement : placements) {
            int index = placement.button().ordinal();
            double target = !placement.button().placeholder() && this.isInside(mouseX, mouseY, placement.x(), placement.y(), placement.width(), placement.height()) ? 1.0 : 0.0;
            this.hoverProgress[index] = this.approach(this.hoverProgress[index], target, HOVER_SPEED, deltaTime);
            for (int segment = 0; segment < this.indicatorProgress[index].length; segment++) {
                double speed = target > this.indicatorProgress[index][segment]
                        ? INDICATOR_EXPAND_SPEEDS[segment]
                        : INDICATOR_RETRACT_SPEEDS[segment];
                this.indicatorProgress[index][segment] = this.approach(this.indicatorProgress[index][segment], target, speed, deltaTime);
            }
        }

        for (AxisStepperWidget stepper : this.getAxisSteppers(this.createLayout())) {
            int index = stepper.axis().ordinal();
            double minusTarget = this.isInside(mouseX, mouseY, stepper.minusX(), stepper.buttonY(), stepper.buttonSize(), stepper.buttonSize()) ? 1.0 : 0.0;
            double plusTarget = this.isInside(mouseX, mouseY, stepper.plusX(), stepper.buttonY(), stepper.buttonSize(), stepper.buttonSize()) ? 1.0 : 0.0;

            this.minusHoverProgress[index] = this.approach(this.minusHoverProgress[index], minusTarget, HOVER_SPEED, deltaTime);
            this.plusHoverProgress[index] = this.approach(this.plusHoverProgress[index], plusTarget, HOVER_SPEED, deltaTime);
            this.minusPressProgress[index] = this.approach(this.minusPressProgress[index], 0.0, FEEDBACK_SPEED, deltaTime);
            this.plusPressProgress[index] = this.approach(this.plusPressProgress[index], 0.0, FEEDBACK_SPEED, deltaTime);
            this.valueFlashProgress[index] = this.approach(this.valueFlashProgress[index], 0.0, FEEDBACK_SPEED, deltaTime);
        }

        ButtonWidget resetButton = this.getOverlapResetButton(this.createLayout());
        double resetTarget = this.isInside(mouseX, mouseY, resetButton.x(), resetButton.y(), resetButton.width(), resetButton.height()) ? 1.0 : 0.0;
        this.resetHoverProgress = this.approach(this.resetHoverProgress, resetTarget, HOVER_SPEED, deltaTime);
        this.resetPressProgress = this.approach(this.resetPressProgress, 0.0, FEEDBACK_SPEED, deltaTime);
        this.resetFlashProgress = this.approach(this.resetFlashProgress, 0.0, FEEDBACK_SPEED, deltaTime);

        ButtonWidget backButton = this.getBackButton(this.createLayout());
        double backTarget = this.isInside(mouseX, mouseY, backButton.x(), backButton.y(), backButton.width(), backButton.height()) ? 1.0 : 0.0;
        this.backHoverProgress = this.approach(this.backHoverProgress, backTarget, HOVER_SPEED, deltaTime);
        this.backPressProgress = this.approach(this.backPressProgress, 0.0, FEEDBACK_SPEED, deltaTime);
    }

    private void drawMainPanel(GuiGraphics guiGraphics, Layout layout) {
        this.drawRoundedBox(guiGraphics, layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight(),
                PANEL_RADIUS * layout.uiScale(), PANEL_FILL, PANEL_BORDER, Math.max(0.8, 1.15 * layout.uiScale()));
    }

    private void drawMenuButton(GuiGraphics guiGraphics, int mouseX, int mouseY, ButtonPlacement placement, Layout layout) {
        MenuButton button = placement.button();
        double hover = this.easeOutCubic(this.hoverProgress[button.ordinal()]);
        boolean selected = button == this.selectedButton;
        double active = Math.max(hover, selected ? 0.34 : 0.0);
        int offsetX = (int) Math.round(4.0 * hover * layout.uiScale());
        int x = placement.x() + offsetX;
        int y = placement.y();
        int width = placement.width();
        int height = placement.height();
        int iconSize = Math.max(19, (int) Math.round((placement.small() ? 24.0 : 31.2) * layout.uiScale()));
        int iconX = x + Math.max(7, (int) Math.round(10.0 * layout.uiScale()));
        int iconY = y + (height - iconSize) / 2;
        int textX = iconX + iconSize + Math.max(7, (int) Math.round(10.0 * layout.uiScale()));
        int textY = y + (height - (int) Math.round(8.0 * layout.textScale())) / 2;

        this.drawSimpleButtonBox(guiGraphics, x, y, width, height, active);

        if (button.placeholder()) {
            return;
        }

        this.drawMenuButtonIndicator(guiGraphics, x, y, width, layout, this.indicatorProgress[button.ordinal()]);
        this.drawIcon(guiGraphics, button.icon(), iconX, iconY, iconSize, active);
        this.drawScaledString(guiGraphics, this.getButtonLabel(button), textX, textY, layout.textScale(), active > 0.02 ? ACTIVE_TEXT_COLOR : TEXT_COLOR, true);
    }

    private void drawMenuButtonIndicator(GuiGraphics guiGraphics, int x, int y, int width, Layout layout, double[] progress) {
        int lineHeight = Math.max(3, (int) Math.round(3.5 * layout.uiScale()));
        int right = x + width - 1;
        int top = y + 1;
        int longLength = Math.max(42, (int) Math.round(92.0 * layout.uiScale()));
        int middleLength = Math.max(28, (int) Math.round(58.0 * layout.uiScale()));
        int shortLength = Math.max(16, (int) Math.round(30.0 * layout.uiScale()));

        this.drawIndicatorLine(guiGraphics, right, top, longLength, lineHeight, this.easeOutCubic(progress[0]), 0xFFF3C6D3);
        this.drawIndicatorLine(guiGraphics, right, top + lineHeight, middleLength, lineHeight, this.easeOutCubic(progress[1]), 0xFFBFE3D7);
        this.drawIndicatorLine(guiGraphics, right, top + lineHeight * 2, shortLength, lineHeight, this.easeOutCubic(progress[2]), 0xFFF6D6A8);
    }

    private void drawIndicatorLine(GuiGraphics guiGraphics, int right, int y, int fullLength, int height, double progress, int color) {
        int length = (int) Math.round(fullLength * progress);
        if (length <= 0) {
            return;
        }

        guiGraphics.fill(right - length, y, right, y + height, color);
    }

    private void drawIcon(GuiGraphics guiGraphics, IconType icon, int x, int y, int size, double active) {
        GuiTextures.drawIcon(guiGraphics, icon.textureName(), x, y, size);
    }

    private void drawOverlapControls(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        this.drawCenteredScaledString(guiGraphics, Lang.tr("iterablock.gui.main_menu.nbt_overlap"),
                layout.overlapLabelX(), layout.axisY(), layout.overlapLabelWidth(), layout.axisCardHeight(),
                layout.textScale(), TEXT_COLOR, true);

        for (AxisStepperWidget stepper : this.getAxisSteppers(layout)) {
            this.drawAxisStepper(guiGraphics, stepper, layout);
        }

        this.drawResetButton(guiGraphics, layout);
    }

    private void drawAxisStepper(GuiGraphics guiGraphics, AxisStepperWidget stepper, Layout layout) {
        int index = stepper.axis().ordinal();

        this.drawStepperButton(guiGraphics, stepper.minusX(), stepper.buttonY(), stepper.buttonSize(), "-", this.minusHoverProgress[index], this.minusPressProgress[index]);
        this.drawStepperButton(guiGraphics, stepper.plusX(), stepper.buttonY(), stepper.buttonSize(), "+", this.plusHoverProgress[index], this.plusPressProgress[index]);

        this.drawValueBox(guiGraphics, stepper.valueX(), stepper.valueY(), stepper.valueWidth(), stepper.valueHeight(), this.valueFlashProgress[index] * 0.75);

        String axisText = stepper.axis().name();
        this.drawCenteredScaledString(guiGraphics, axisText, stepper.labelX(), stepper.y(), stepper.labelWidth(), stepper.height(), layout.textScale(), TEXT_COLOR, true);

        String value = Integer.toString(SchematicPlacementState.getOverlap(stepper.axis()));
        this.drawCenteredScaledString(guiGraphics, value, stepper.valueX(), stepper.valueY(), stepper.valueWidth(), stepper.valueHeight(), layout.textScale(), TEXT_COLOR, true);
    }

    private void drawStepperButton(GuiGraphics guiGraphics, int x, int y, int size, String label, double hoverProgress, double pressProgress) {
        double hover = this.easeOutCubic(hoverProgress);
        int inset = pressProgress > 0.02 ? 1 : 0;
        int drawX = x + inset;
        int drawY = y + inset;
        int drawSize = size - inset * 2;

        this.drawSimpleButtonBox(guiGraphics, drawX, drawY, drawSize, drawSize, hover);
        this.drawStepperSymbol(guiGraphics, label, drawX, drawY, drawSize, hover > 0.02 ? ACTIVE_TEXT_COLOR : TEXT_COLOR);
    }

    private void drawStepperSymbol(GuiGraphics guiGraphics, String symbol, int x, int y, int size, int color) {
        int symbolX = x + (size - this.getStringWidth(symbol)) / 2 + 1;
        int symbolY = y + (size - 8) / 2 + ("-".equals(symbol) ? 2 : 0);
        guiGraphics.drawString(this.textRenderer, symbol, symbolX, symbolY, color, false);
    }

    private void drawResetButton(GuiGraphics guiGraphics, Layout layout) {
        ButtonWidget resetButton = this.getOverlapResetButton(layout);
        double hover = this.easeOutCubic(Math.max(this.resetHoverProgress, this.resetFlashProgress * 0.45));
        int inset = this.resetPressProgress > 0.02 ? 1 : 0;

        this.drawSimpleButtonBox(guiGraphics, resetButton.x() + inset, resetButton.y() + inset, resetButton.width() - inset * 2, resetButton.height() - inset * 2, hover);

        String label = "\u6062\u590d\u9ed8\u8ba4";
        this.drawCenteredScaledString(
                guiGraphics,
                label,
                resetButton.x() + inset,
                resetButton.y() + inset,
                resetButton.width() - inset * 2,
                resetButton.height() - inset * 2,
                Math.max(0.58, layout.textScale() * 0.86),
                hover > 0.02 ? ACTIVE_TEXT_COLOR : TEXT_COLOR,
                false);
    }

    private void drawBackButton(GuiGraphics guiGraphics, Layout layout) {
        ButtonWidget button = this.getBackButton(layout);
        double hover = this.easeOutCubic(this.backHoverProgress);
        int inset = this.backPressProgress > 0.02 ? 1 : 0;
        int x = button.x() + inset;
        int y = button.y() + inset;
        int width = button.width() - inset * 2;
        int height = button.height() - inset * 2;

        this.drawSimpleButtonBox(guiGraphics, x, y, width, height, hover);
        this.drawCenteredScaledString(guiGraphics, Lang.tr("iterablock.gui.button.back"), x, y, width, height,
                Math.max(0.58, layout.textScale() * 0.86), hover > 0.02 ? ACTIVE_TEXT_COLOR : TEXT_COLOR, false);
    }

    private void drawSimpleButtonBox(GuiGraphics guiGraphics, int x, int y, int width, int height, double hover) {
        int fill = this.blendColor(SIMPLE_BUTTON_FILL, SIMPLE_BUTTON_FILL_HOVER, hover);
        int border = this.blendColor(SIMPLE_BUTTON_BORDER, SIMPLE_BUTTON_BORDER_HOVER, hover);

        this.drawRoundedBox(guiGraphics, x, y, width, height, Math.min(BUTTON_RADIUS, height * 0.28), fill, border, 1.0);
    }

    private void drawValueBox(GuiGraphics guiGraphics, int x, int y, int width, int height, double hover) {
        int fill = this.blendColor(0xAA9EA5A5, 0xC8C2CACA, hover);
        int border = this.blendColor(0x99FFFFFF, 0xE8FFFFFF, hover);

        this.drawRoundedBox(guiGraphics, x, y, width, height, Math.min(BUTTON_RADIUS, height * 0.28), fill, border, 1.0);
    }

    private void drawCenteredScaledString(GuiGraphics guiGraphics, String text, int x, int y, int width, int height, double scale, int color, boolean shadow) {
        int textX = x + (int) Math.round((width - this.getStringWidth(text) * scale) / 2.0);
        int textY = y + (int) Math.round((height - 8.0 * scale) / 2.0);
        this.drawScaledString(guiGraphics, text, textX, textY, scale, color, shadow);
    }

    private void runAction(MenuButton button) {
        switch (button) {
            case LOADED_FILES -> GuiBase.openGui(new GuiLoadedLitematicList(true));
            case LOAD_FILE -> GuiBase.openGui(new GuiIteraBlockMainMenu(true));
            case CONFIG -> GuiBase.openGui(new GuiBuilderHelperSettings(true));
            case PLACEMENT_REPLACE_MODE -> {
                BuilderHelperClientConfig.setPlacementReplaceMode(BuilderHelperClientConfig.getPlacementReplaceMode().next());
                this.selectedButton = null;
            }
            case RANDOM_PLACEMENT_CONFIG -> GuiBase.openGui(new GuiRandomPlacementConfig(true));
            case BEZIER_CURVE_CONFIG -> GuiBase.openGui(new GuiBezierCurveConfig(true));
            default -> {
            }
        }
    }

    private void adjustOverlap(SchematicPlacementState.Axis axis, int amount) {
        if (amount == 0) {
            return;
        }

        SchematicPlacementState.adjustOverlap(axis, amount);
        this.valueFlashProgress[axis.ordinal()] = 1.0;
    }

    private String getButtonLabel(MenuButton button) {
        if (button != MenuButton.PLACEMENT_REPLACE_MODE) {
            return button.numberPrefix() + Lang.tr(button.labelKey());
        }

        PlacementReplaceMode mode = BuilderHelperClientConfig.getPlacementReplaceMode();
        String valueKey = switch (mode) {
            case REPLACE_ALL -> "iterablock.gui.main_menu.replace_mode.replace_all";
            case ONLY_REPLACE_AIR -> "iterablock.gui.main_menu.replace_mode.only_air";
        };

        return button.numberPrefix() + Lang.tr(button.labelKey(), Lang.tr(valueKey));
    }

    private String getMenuTitle() {
        return IteraBlock.MODID + " " + Lang.tr("iterablock.gui.main_menu.menu_suffix");
    }

    private Layout createLayout() {
        double uiScale = this.clamp(Math.min(this.width / 1920.0, this.height / 1080.0), 0.72, 1.0);
        int margin = Math.max(12, (int) Math.round(BASE_SAFE_MARGIN * uiScale));
        int columnGap = Math.max(14, (int) Math.round(BASE_COLUMN_GAP * uiScale));
        int rowGap = Math.max(5, (int) Math.round(BASE_ROW_GAP * uiScale));
        int groupGap = Math.max(14, (int) Math.round(BASE_GROUP_GAP * uiScale));
        int buttonHeight = Math.max(22, (int) Math.round(BASE_BUTTON_HEIGHT * uiScale));
        int axisCardHeight = Math.max(18, (int) Math.round(BASE_AXIS_CARD_HEIGHT * uiScale));
        int axisGap = Math.max(4, (int) Math.round(BASE_AXIS_GAP * uiScale));
        int resetHeight = Math.max(14, (int) Math.round(BASE_RESET_HEIGHT * uiScale));
        int resetWidth = Math.max(48, (int) Math.round(BASE_RESET_WIDTH * uiScale));
        int availableWidth = Math.max(120, this.width - margin * 2);
        int buttonWidth = Math.min((int) Math.round(BASE_BUTTON_WIDTH * uiScale), Math.max(80, (availableWidth - columnGap) / 2));
        int contentWidth = buttonWidth * 2 + columnGap;
        int axisCardWidth = Math.max(66, (int) Math.round(BASE_AXIS_CARD_WIDTH * uiScale));
        int startX = Math.max(margin, (this.width - contentWidth) / 2);
        int panelPadding = Math.max(9, (int) Math.round(14.0 * uiScale));
        int titleSpace = Math.max(19, (int) Math.round(27.0 * uiScale));
        int backGap = Math.max(8, (int) Math.round(12.0 * uiScale));
        int backHeight = Math.max(16, (int) Math.round(20.0 * uiScale));
        int backWidth = Math.max(52, (int) Math.round(72.0 * uiScale));
        int previewHeight = Math.max(7, (int) Math.round(9.0 * uiScale));
        int panelHeight = titleSpace + buttonHeight * 4 + rowGap * 3 + groupGap + axisCardHeight + 5
                + previewHeight + panelPadding * 2;
        int totalHeight = panelHeight + backGap + backHeight;
        // Keep the main panel and the external back button balanced vertically.
        int panelY = Math.max(8, (this.height - totalHeight) / 2);
        int topY = panelY + panelPadding + titleSpace;
        int panelX = startX - panelPadding;
        int panelWidth = contentWidth + panelPadding * 2;
        double textScale = Math.max(0.62, 0.82 * uiScale);

        int rowStep = buttonHeight + rowGap;
        int axisY = topY + buttonHeight * 4 + rowGap * 3 + groupGap;
        String overlapLabel = Lang.tr("iterablock.gui.main_menu.nbt_overlap");
        int overlapLabelWidth = Math.max(1, (int) Math.ceil(this.getStringWidth(overlapLabel) * textScale));
        int overlapLabelGap = Math.max(6, (int) Math.round(8.0 * uiScale));
        int resetGap = axisGap * 2;
        int overlapGroupWidth = overlapLabelWidth + overlapLabelGap + axisCardWidth * 3 + axisGap * 2 + resetGap + resetWidth;
        int overlapLabelX = panelX + (panelWidth - overlapGroupWidth) / 2;
        int axisStartX = overlapLabelX + overlapLabelWidth + overlapLabelGap;
        int previewY = axisY + axisCardHeight + 5;
        int resetY = axisY + Math.max(0, (axisCardHeight - resetHeight) / 2);
        int resetX = axisStartX + axisCardWidth * 3 + axisGap * 2 + resetGap;
        int backY = panelY + panelHeight + backGap;
        int backX = panelX + panelWidth - panelPadding - backWidth;

        return new Layout(uiScale, startX, topY, buttonWidth, buttonHeight, overlapLabelX, overlapLabelWidth,
                axisStartX, axisY, axisCardWidth,
                axisCardHeight, axisGap, previewY, resetX, resetY, resetWidth, resetHeight, backX, backY,
                backWidth, backHeight, panelX, panelY, panelWidth, panelHeight, columnGap, rowGap, groupGap,
                textScale, Math.max(0.78, 1.08 * uiScale));
    }

    private List<ButtonPlacement> getButtonPlacements(Layout layout) {
        List<ButtonPlacement> placements = new ArrayList<>();
        int leftX = layout.startX();
        int rightX = layout.startX() + layout.buttonWidth() + layout.columnGap();
        int topY = layout.topY();
        int rowStep = layout.buttonHeight() + layout.rowGap();

        placements.add(new ButtonPlacement(MenuButton.LOADED_FILES, leftX, topY, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.LOAD_FILE, rightX, topY, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.CONFIG, leftX, topY + rowStep, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.PLACEMENT_REPLACE_MODE, rightX, topY + rowStep, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.RANDOM_PLACEMENT_CONFIG, leftX, topY + rowStep * 2, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.BEZIER_CURVE_CONFIG, rightX, topY + rowStep * 2, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.EMPTY_LEFT, leftX, topY + rowStep * 3, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.EMPTY_RIGHT, rightX, topY + rowStep * 3, layout.buttonWidth(), layout.buttonHeight(), false));
        return placements;
    }

    private List<AxisStepperWidget> getAxisSteppers(Layout layout) {
        List<AxisStepperWidget> controls = new ArrayList<>();
        SchematicPlacementState.Axis[] axes = SchematicPlacementState.Axis.values();

        for (int i = 0; i < axes.length; i++) {
            int x = layout.axisStartX() + (layout.axisCardWidth() + layout.axisGap()) * i;
            int y = layout.axisY();
            controls.add(new AxisStepperWidget(axes[i], x, y, layout.axisCardWidth(), layout.axisCardHeight()));
        }

        return controls;
    }

    private ButtonWidget getOverlapResetButton(Layout layout) {
        return new ButtonWidget(layout.resetX(), layout.resetY(), layout.resetWidth(), layout.resetHeight());
    }

    private ButtonWidget getBackButton(Layout layout) {
        return new ButtonWidget(layout.backX(), layout.backY(), layout.backWidth(), layout.backHeight());
    }

    private void drawScaledString(GuiGraphics guiGraphics, String text, int x, int y, double scale, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) scale, (float) scale, 1.0F);
        guiGraphics.drawString(this.textRenderer, text, (int) Math.round(x / scale), (int) Math.round(y / scale), color, shadow);
        guiGraphics.pose().popPose();
    }

    private void drawRoundedBox(GuiGraphics guiGraphics, double x, double y, double width, double height,
                                double radius, int fillColor, int borderColor, double borderWidth) {
        if (width <= 0.0 || height <= 0.0) {
            return;
        }

        double safeRadius = Math.max(0.0, Math.min(radius, Math.min(width, height) * 0.5));
        double inset = Math.max(0.0, Math.min(borderWidth, Math.min(width, height) * 0.5));
        if (inset > 0.0 && ((borderColor >>> 24) & 0xFF) > 0) {
            this.drawRoundedRing(guiGraphics, x, y, width, height, safeRadius, inset, borderColor);
        }

        if (((fillColor >>> 24) & 0xFF) > 0 && width > inset * 2.0 && height > inset * 2.0) {
            this.drawRoundedRect(guiGraphics, x + inset, y + inset, width - inset * 2.0, height - inset * 2.0,
                    Math.max(0.0, safeRadius - inset), fillColor);
        }
    }

    private void drawRoundedRing(GuiGraphics guiGraphics, double x, double y, double width, double height,
                                 double radius, double thickness, int color) {
        double innerX = x + thickness;
        double innerY = y + thickness;
        double innerWidth = width - thickness * 2.0;
        double innerHeight = height - thickness * 2.0;
        if (innerWidth <= 0.0 || innerHeight <= 0.0) {
            this.drawRoundedRect(guiGraphics, x, y, width, height, radius, color);
            return;
        }

        List<double[]> outer = this.createRoundedPerimeter(x, y, width, height, radius);
        List<double[]> inner = this.createRoundedPerimeter(innerX, innerY, innerWidth, innerHeight,
                Math.max(0.0, radius - thickness));
        BufferBuilder buffer = this.beginMesh(VertexFormat.Mode.TRIANGLES);

        for (int i = 0; i < outer.size(); i++) {
            int next = (i + 1) % outer.size();
            double[] outerCurrent = outer.get(i);
            double[] outerNext = outer.get(next);
            double[] innerCurrent = inner.get(i);
            double[] innerNext = inner.get(next);
            this.addVertex(guiGraphics, buffer, outerCurrent[0], outerCurrent[1], color);
            this.addVertex(guiGraphics, buffer, outerNext[0], outerNext[1], color);
            this.addVertex(guiGraphics, buffer, innerNext[0], innerNext[1], color);
            this.addVertex(guiGraphics, buffer, outerCurrent[0], outerCurrent[1], color);
            this.addVertex(guiGraphics, buffer, innerNext[0], innerNext[1], color);
            this.addVertex(guiGraphics, buffer, innerCurrent[0], innerCurrent[1], color);
        }

        this.drawMesh(buffer);
    }

    private void drawRoundedRect(GuiGraphics guiGraphics, double x, double y, double width, double height,
                                 double radius, int color) {
        if (((color >>> 24) & 0xFF) == 0 || width <= 0.0 || height <= 0.0) {
            return;
        }

        List<double[]> perimeter = this.createRoundedPerimeter(x, y, width, height, radius);

        BufferBuilder buffer = this.beginMesh(VertexFormat.Mode.TRIANGLES);
        double centerX = x + width * 0.5;
        double centerY = y + height * 0.5;

        for (int i = 0; i < perimeter.size(); i++) {
            double[] current = perimeter.get(i);
            double[] next = perimeter.get((i + 1) % perimeter.size());
            this.addVertex(guiGraphics, buffer, centerX, centerY, color);
            this.addVertex(guiGraphics, buffer, current[0], current[1], color);
            this.addVertex(guiGraphics, buffer, next[0], next[1], color);
        }

        this.drawMesh(buffer);
    }

    private List<double[]> createRoundedPerimeter(double x, double y, double width, double height, double radius) {
        double safeRadius = Math.max(0.0, Math.min(radius, Math.min(width, height) * 0.5));
        List<double[]> perimeter = new ArrayList<>(4 * (CORNER_SEGMENTS + 1));
        this.addRoundedCorner(perimeter, x + width - safeRadius, y + safeRadius, safeRadius, -Math.PI * 0.5, 0.0);
        this.addRoundedCorner(perimeter, x + width - safeRadius, y + height - safeRadius, safeRadius, 0.0, Math.PI * 0.5);
        this.addRoundedCorner(perimeter, x + safeRadius, y + height - safeRadius, safeRadius, Math.PI * 0.5, Math.PI);
        this.addRoundedCorner(perimeter, x + safeRadius, y + safeRadius, safeRadius, Math.PI, Math.PI * 1.5);
        return perimeter;
    }

    private void addRoundedCorner(List<double[]> perimeter, double centerX, double centerY, double radius,
                                  double startAngle, double endAngle) {
        for (int i = 0; i <= CORNER_SEGMENTS; i++) {
            double angle = startAngle + (endAngle - startAngle) * i / CORNER_SEGMENTS;
            perimeter.add(new double[] {centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius});
        }
    }

    private BufferBuilder beginMesh(VertexFormat.Mode mode) {
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.applyModelViewMatrix();
        return Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
    }

    private void addVertex(GuiGraphics guiGraphics, BufferBuilder buffer, double x, double y, int color) {
        buffer.addVertex(guiGraphics.pose().last(), (float) x, (float) y, 0.0F).setColor(color);
    }

    private void drawMesh(BufferBuilder buffer) {
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
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
        double t = this.clamp(amount, 0.0, 1.0);
        int red = (int) Math.round(((first >> 16) & 0xFF) + (((second >> 16) & 0xFF) - ((first >> 16) & 0xFF)) * t);
        int green = (int) Math.round(((first >> 8) & 0xFF) + (((second >> 8) & 0xFF) - ((first >> 8) & 0xFF)) * t);
        int blue = (int) Math.round((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * t);
        return red << 16 | green << 8 | blue;
    }

    private int blendColor(int first, int second, double amount) {
        double t = this.clamp(amount, 0.0, 1.0);
        int alpha = (int) Math.round(((first >> 24) & 0xFF) + (((second >> 24) & 0xFF) - ((first >> 24) & 0xFF)) * t);
        int red = (int) Math.round(((first >> 16) & 0xFF) + (((second >> 16) & 0xFF) - ((first >> 16) & 0xFF)) * t);
        int green = (int) Math.round(((first >> 8) & 0xFF) + (((second >> 8) & 0xFF) - ((first >> 8) & 0xFF)) * t);
        int blue = (int) Math.round((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * t);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private int withAlpha(int rgb, double alpha) {
        int alphaByte = (int) Math.round(this.clamp(alpha, 0.0, 1.0) * 255.0);
        return alphaByte << 24 | rgb;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private enum MenuButton {
        LOADED_FILES(1, "iterablock.gui.main_menu.loaded_files", IconType.STACK),
        LOAD_FILE(2, "iterablock.gui.main_menu.load_file", IconType.FOLDER),
        CONFIG(3, "iterablock.gui.main_menu.config_button", IconType.CONFIG),
        PLACEMENT_REPLACE_MODE(4, "iterablock.gui.main_menu.replace_mode", IconType.CUSTOM),
        RANDOM_PLACEMENT_CONFIG(5, "iterablock.gui.main_menu.random_config", IconType.RANDOM_CONFIG),
        BEZIER_CURVE_CONFIG(6, "iterablock.gui.main_menu.bezier_config", IconType.BEZIER_CONFIG),
        EMPTY_LEFT(0, "", IconType.CUSTOM, true),
        EMPTY_RIGHT(0, "", IconType.CUSTOM, true);

        private final int number;
        private final String labelKey;
        private final IconType icon;
        private final boolean placeholder;

        MenuButton(int number, String labelKey, IconType icon) {
            this(number, labelKey, icon, false);
        }

        MenuButton(int number, String labelKey, IconType icon, boolean placeholder) {
            this.number = number;
            this.labelKey = labelKey;
            this.icon = icon;
            this.placeholder = placeholder;
        }

        private String numberPrefix() {
            return String.format("%02d  ", this.number);
        }

        private String labelKey() {
            return this.labelKey;
        }

        private IconType icon() {
            return this.icon;
        }

        private boolean placeholder() {
            return this.placeholder;
        }
    }

    private enum IconType {
        STACK("stack"),
        FOLDER("folder"),
        REGION("region"),
        CONFIG("config"),
        FILES("files"),
        RANDOM_CONFIG("random_config"),
        BEZIER_CONFIG("bezier_config"),
        CUSTOM("custom");

        private final String textureName;

        IconType(String textureName) {
            this.textureName = textureName;
        }

        private String textureName() {
            return this.textureName;
        }
    }

    private record Layout(double uiScale, int startX, int topY, int buttonWidth, int buttonHeight,
                          int overlapLabelX, int overlapLabelWidth,
                          int axisStartX, int axisY, int axisCardWidth, int axisCardHeight, int axisGap,
                          int previewY, int resetX, int resetY, int resetWidth, int resetHeight,
                          int backX, int backY, int backWidth, int backHeight,
                          int panelX, int panelY, int panelWidth, int panelHeight,
                          int columnGap, int rowGap, int groupGap, double textScale, double titleScale) {
        private int titleY() {
            return this.panelY + Math.max(7, (int) Math.round(10.0 * this.uiScale));
        }

    }

    private record ButtonPlacement(MenuButton button, int x, int y, int width, int height, boolean small) {
    }

    private record AxisStepperWidget(SchematicPlacementState.Axis axis, int x, int y, int width, int height) {
        private int buttonSize() {
            return Math.max(12, this.height - 6);
        }

        private int buttonY() {
            return this.y + (this.height - this.buttonSize()) / 2;
        }

        private int minusX() {
            return this.x + this.labelWidth() + 2;
        }

        private int plusX() {
            return this.x + this.width - this.buttonSize() - 3;
        }

        private int labelX() {
            return this.x + 4;
        }

        private int labelWidth() {
            return 13;
        }

        private int valueX() {
            return this.minusX() + this.buttonSize() + 3;
        }

        private int valueY() {
            return this.y + (this.height - this.valueHeight()) / 2;
        }

        private int valueWidth() {
            return Math.max(14, this.plusX() - this.valueX() - 3);
        }

        private int valueHeight() {
            return Math.max(12, this.height - 6);
        }

        private int textY() {
            return this.y + (this.height - 8) / 2;
        }
    }

    private record ButtonWidget(int x, int y, int width, int height) {
    }
}

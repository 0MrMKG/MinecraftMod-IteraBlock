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

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class GuiBuilderHelperMainMenu extends GuiBase {
    private static final int BASE_BUTTON_WIDTH = 252;
    private static final int BASE_BUTTON_HEIGHT = 38;
    private static final int BASE_COLUMN_GAP = 46;
    private static final int BASE_ROW_GAP = 10;
    private static final int BASE_GROUP_GAP = 28;
    private static final int BASE_SAFE_MARGIN = 34;
    private static final int BASE_AXIS_CARD_WIDTH = 70;
    private static final int BASE_AXIS_CARD_HEIGHT = 20;
    private static final int BASE_AXIS_GAP = 5;
    private static final int BASE_RESET_WIDTH = 58;
    private static final int BASE_RESET_HEIGHT = 16;
    private static final double HOVER_SPEED = 8.0;
    private static final double FEEDBACK_SPEED = 13.0;
    private static final int TITLE_COLOR = 0xFFF3EECF;
    private static final int TEXT_COLOR = 0xFFD8E3E3;
    private static final int ACTIVE_TEXT_COLOR = 0xFFFFFFB8;
    private static final int SIMPLE_BUTTON_FILL = 0x8A4A5050;
    private static final int SIMPLE_BUTTON_FILL_HOVER = 0xAA5E6666;
    private static final int SIMPLE_BUTTON_BORDER = 0xBFFFFFFF;
    private static final int SIMPLE_BUTTON_BORDER_HOVER = 0xE8FFFFFF;

    private final double[] hoverProgress = new double[MenuButton.values().length];
    private final double[] axisHoverProgress = new double[SchematicPlacementState.Axis.values().length];
    private final double[] minusHoverProgress = new double[SchematicPlacementState.Axis.values().length];
    private final double[] plusHoverProgress = new double[SchematicPlacementState.Axis.values().length];
    private final double[] minusPressProgress = new double[SchematicPlacementState.Axis.values().length];
    private final double[] plusPressProgress = new double[SchematicPlacementState.Axis.values().length];
    private final double[] valueFlashProgress = new double[SchematicPlacementState.Axis.values().length];
    private double resetHoverProgress;
    private double resetPressProgress;
    private double resetFlashProgress;
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
        guiGraphics.fill(0, 0, this.width, this.height, 0x72000000);
    }

    @Override
    protected void drawTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void drawContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Layout layout = this.createLayout();
        List<ButtonPlacement> placements = this.getButtonPlacements(layout);

        this.updateHoverAnimations(mouseX, mouseY, placements);
        this.drawScaledString(guiGraphics, this.getMenuTitle(), layout.startX(), layout.titleY(), layout.titleScale(), TITLE_COLOR, true);

        for (ButtonPlacement placement : placements) {
            this.drawMenuButton(guiGraphics, mouseX, mouseY, placement, layout);
        }

        this.drawOverlapControls(guiGraphics, mouseX, mouseY, layout);
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
        }

        for (AxisStepperWidget stepper : this.getAxisSteppers(this.createLayout())) {
            int index = stepper.axis().ordinal();
            double cardTarget = this.isInside(mouseX, mouseY, stepper.x(), stepper.y(), stepper.width(), stepper.height()) ? 1.0 : 0.0;
            double minusTarget = this.isInside(mouseX, mouseY, stepper.minusX(), stepper.buttonY(), stepper.buttonSize(), stepper.buttonSize()) ? 1.0 : 0.0;
            double plusTarget = this.isInside(mouseX, mouseY, stepper.plusX(), stepper.buttonY(), stepper.buttonSize(), stepper.buttonSize()) ? 1.0 : 0.0;

            this.axisHoverProgress[index] = this.approach(this.axisHoverProgress[index], cardTarget, HOVER_SPEED, deltaTime);
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

        this.drawMenuButtonIndicator(guiGraphics, x, y, width, layout, selected ? 1.0 : hover);
        this.drawIcon(guiGraphics, button.icon(), iconX, iconY, iconSize, active);
        this.drawScaledString(guiGraphics, this.getButtonLabel(button), textX, textY, layout.textScale(), active > 0.02 ? ACTIVE_TEXT_COLOR : TEXT_COLOR, true);
    }

    private void drawMenuButtonIndicator(GuiGraphics guiGraphics, int x, int y, int width, Layout layout, double progress) {
        double t = this.clamp(progress, 0.0, 1.0);
        int gap = Math.max(1, (int) Math.round(3.0 * layout.uiScale()));
        int lineHeight = Math.max(3, (int) Math.round(3.5 * layout.uiScale()));
        int right = x + width - 1;
        int top = y + 1;
        int longLength = Math.max(42, (int) Math.round(92.0 * layout.uiScale()));
        int middleLength = Math.max(28, (int) Math.round(58.0 * layout.uiScale()));
        int shortLength = Math.max(16, (int) Math.round(30.0 * layout.uiScale()));

        this.drawIndicatorLine(guiGraphics, right, top, longLength, lineHeight, this.delayedIndicatorProgress(t, 0.00), 0xFFF3C6D3);
        this.drawIndicatorLine(guiGraphics, right, top + lineHeight + gap, middleLength, lineHeight, this.delayedIndicatorProgress(t, 0.14), 0xFFBFE3D7);
        this.drawIndicatorLine(guiGraphics, right, top + (lineHeight + gap) * 2, shortLength, lineHeight, this.delayedIndicatorProgress(t, 0.28), 0xFFF6D6A8);
    }

    private void drawIndicatorLine(GuiGraphics guiGraphics, int right, int y, int fullLength, int height, double progress, int color) {
        int length = (int) Math.round(fullLength * progress);
        if (length <= 0) {
            return;
        }

        guiGraphics.fill(right - length, y, right, y + height, color);
    }

    private double delayedIndicatorProgress(double progress, double delay) {
        return this.easeOutCubic(this.clamp((progress - delay) / Math.max(0.001, 1.0 - delay), 0.0, 1.0));
    }

    private void drawIcon(GuiGraphics guiGraphics, IconType icon, int x, int y, int size, double active) {
        GuiTextures.drawIcon(guiGraphics, icon.textureName(), x, y, size);
    }

    private void drawOverlapControls(GuiGraphics guiGraphics, int mouseX, int mouseY, Layout layout) {
        for (AxisStepperWidget stepper : this.getAxisSteppers(layout)) {
            this.drawAxisStepper(guiGraphics, stepper, layout);
        }

        this.drawOffsetPreview(guiGraphics, layout);
        this.drawResetButton(guiGraphics, layout);
    }

    private void drawAxisStepper(GuiGraphics guiGraphics, AxisStepperWidget stepper, Layout layout) {
        int index = stepper.axis().ordinal();
        double hover = this.easeOutCubic(this.axisHoverProgress[index]);
        int labelColor = this.getAxisColor(stepper.axis(), hover);

        this.drawStepperButton(guiGraphics, stepper.minusX(), stepper.buttonY(), stepper.buttonSize(), "-", this.minusHoverProgress[index], this.minusPressProgress[index], layout);
        this.drawStepperButton(guiGraphics, stepper.plusX(), stepper.buttonY(), stepper.buttonSize(), "+", this.plusHoverProgress[index], this.plusPressProgress[index], layout);

        this.drawValueBox(guiGraphics, stepper.valueX(), stepper.valueY(), stepper.valueWidth(), stepper.valueHeight(), Math.max(hover * 0.45, this.valueFlashProgress[index] * 0.75));

        String axisText = stepper.axis().name();
        this.drawCenteredScaledString(guiGraphics, axisText, stepper.labelX(), stepper.y(), stepper.labelWidth(), stepper.height(), layout.textScale(), labelColor, true);

        String value = Integer.toString(SchematicPlacementState.getOverlap(stepper.axis()));
        this.drawCenteredScaledString(guiGraphics, value, stepper.valueX(), stepper.valueY(), stepper.valueWidth(), stepper.valueHeight(), layout.textScale(), ACTIVE_TEXT_COLOR, true);
    }

    private void drawStepperButton(GuiGraphics guiGraphics, int x, int y, int size, String label, double hoverProgress, double pressProgress, Layout layout) {
        double hover = this.easeOutCubic(hoverProgress);
        int inset = pressProgress > 0.02 ? 1 : 0;
        int drawX = x + inset;
        int drawY = y + inset;
        int drawSize = size - inset * 2;

        this.drawSimpleButtonBox(guiGraphics, drawX, drawY, drawSize, drawSize, hover);
        this.drawCenteredScaledString(guiGraphics, label, drawX, drawY, drawSize, drawSize, Math.max(0.66, layout.textScale()), hover > 0.02 ? ACTIVE_TEXT_COLOR : TEXT_COLOR, false);
    }

    private void drawOffsetPreview(GuiGraphics guiGraphics, Layout layout) {
        String text = "\u5f53\u524d\u504f\u79fb: X " + SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.X)
                + " / Y " + SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Y)
                + " / Z " + SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Z);
        this.drawScaledString(guiGraphics, text, layout.previewX(), layout.previewY(), Math.max(0.58, layout.textScale() * 0.82), 0xFF9FB3AE, false);
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

    private void drawSimpleButtonBox(GuiGraphics guiGraphics, int x, int y, int width, int height, double hover) {
        int fill = this.blendColor(SIMPLE_BUTTON_FILL, SIMPLE_BUTTON_FILL_HOVER, hover);
        int border = this.blendColor(SIMPLE_BUTTON_BORDER, SIMPLE_BUTTON_BORDER_HOVER, hover);

        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    private void drawValueBox(GuiGraphics guiGraphics, int x, int y, int width, int height, double hover) {
        int fill = this.blendColor(0xAA9EA5A5, 0xC8C2CACA, hover);
        int border = this.blendColor(0x99FFFFFF, 0xE8FFFFFF, hover);

        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
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
            case PLACEMENT_REPLACE_MODE -> BuilderHelperClientConfig.setPlacementReplaceMode(BuilderHelperClientConfig.getPlacementReplaceMode().next());
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

    private int getAxisColor(SchematicPlacementState.Axis axis, double hover) {
        int base = switch (axis) {
            case X -> 0xA8D8CE;
            case Y -> 0xB8D6A8;
            case Z -> 0xA8C8D8;
        };

        return this.withAlpha(this.blendRgb(base, 0xFFF3B0, hover), 0.96);
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
        int topY = Math.max(margin + 22, (int) Math.round(this.height * 0.16));
        int totalHeight = buttonHeight * 4 + rowGap * 3 + groupGap + axisCardHeight + 14;
        int maxTopY = Math.max(margin + 18, this.height - margin - totalHeight);

        if (topY > maxTopY) {
            topY = maxTopY;
        }

        int rowStep = buttonHeight + rowGap;
        int axisY = topY + rowStep * 4 + groupGap;
        int axisStartX = startX;
        int previewY = axisY + axisCardHeight + 5;
        int resetY = axisY + Math.max(0, (axisCardHeight - resetHeight) / 2);
        int resetX = startX + contentWidth - resetWidth;

        return new Layout(uiScale, startX, topY, buttonWidth, buttonHeight, axisStartX, axisY, axisCardWidth, axisCardHeight, axisGap, previewY, resetX, resetY, resetWidth, resetHeight, columnGap, rowGap, groupGap, Math.max(0.62, 0.82 * uiScale), Math.max(0.78, 1.08 * uiScale));
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

    private void drawScaledString(GuiGraphics guiGraphics, String text, int x, int y, double scale, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) scale, (float) scale, 1.0F);
        guiGraphics.drawString(this.textRenderer, text, (int) Math.round(x / scale), (int) Math.round(y / scale), color, shadow);
        guiGraphics.pose().popPose();
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

    private record Layout(double uiScale, int startX, int topY, int buttonWidth, int buttonHeight, int axisStartX, int axisY, int axisCardWidth, int axisCardHeight, int axisGap, int previewY, int resetX, int resetY, int resetWidth, int resetHeight, int columnGap, int rowGap, int groupGap, double textScale, double titleScale) {
        private int titleY() {
            return Math.max(8, this.topY - (int) Math.round(34.0 * this.uiScale));
        }

        private int previewX() {
            return this.axisStartX;
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

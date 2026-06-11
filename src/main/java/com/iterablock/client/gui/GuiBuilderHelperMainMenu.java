package com.iterablock.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.gui.settings.GuiBuilderHelperSettings;
import com.iterablock.common.PlacementReplaceMode;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class GuiBuilderHelperMainMenu extends GuiBase {
    private static final int BASE_BUTTON_WIDTH = 252;
    private static final int BASE_BUTTON_HEIGHT = 38;
    private static final int BASE_SMALL_BUTTON_HEIGHT = 28;
    private static final int BASE_COLUMN_GAP = 46;
    private static final int BASE_ROW_GAP = 10;
    private static final int BASE_GROUP_GAP = 28;
    private static final int BASE_SAFE_MARGIN = 34;
    private static final double HOVER_SPEED = 8.0;
    private static final int TITLE_COLOR = 0xFFF3EECF;
    private static final int TEXT_COLOR = 0xFFD8E3E3;
    private static final int ACTIVE_TEXT_COLOR = 0xFFFFFFB8;
    private static final int ACCENT_COLOR = 0xFF6F8D78;

    private final double[] hoverProgress = new double[MenuButton.values().length];
    private long lastFrameNanos;
    private MenuButton selectedButton = MenuButton.LOADED_FILES;

    public GuiBuilderHelperMainMenu() {
        this.setTitle(Lang.tr("iterablock.gui.main_menu.title"));
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
        this.drawScaledString(guiGraphics, Lang.tr("iterablock.gui.main_menu.title"), layout.startX(), layout.titleY(), layout.titleScale(), TITLE_COLOR, true);

        for (ButtonPlacement placement : placements) {
            this.drawMenuButton(guiGraphics, mouseX, mouseY, placement, layout);
        }
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        for (ButtonPlacement placement : this.getButtonPlacements(this.createLayout())) {
            if (this.isInside(mouseX, mouseY, placement.x(), placement.y(), placement.width(), placement.height())) {
                this.selectedButton = placement.button();
                this.runAction(placement.button());
                return true;
            }
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_O || keyCode == GLFW.GLFW_KEY_ESCAPE) {
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
            double target = this.isInside(mouseX, mouseY, placement.x(), placement.y(), placement.width(), placement.height()) ? 1.0 : 0.0;
            this.hoverProgress[index] = this.approach(this.hoverProgress[index], target, HOVER_SPEED, deltaTime);
        }
    }

    private void drawMenuButton(GuiGraphics guiGraphics, int mouseX, int mouseY, ButtonPlacement placement, Layout layout) {
        MenuButton button = placement.button();
        double hover = this.easeOutCubic(this.hoverProgress[button.ordinal()]);
        boolean selected = button == this.selectedButton || this.isSelectedPlacementMode(button);
        double active = Math.max(hover, selected ? 0.34 : 0.0);
        int offsetX = (int) Math.round(4.0 * hover * layout.uiScale());
        int x = placement.x() + offsetX;
        int y = placement.y();
        int width = placement.width();
        int height = placement.height();
        int background = this.withAlpha(this.blendRgb(0x151A1B, 0x2C3534, active), 0.76 + active * 0.12);
        int border = this.withAlpha(this.blendRgb(0x43504F, 0xAFC2B5, active), 0.74 + active * 0.20);
        int innerLine = this.withAlpha(this.blendRgb(0x111516, 0xDEEBDD, active), 0.20 + active * 0.28);
        int accentWidth = Math.max(2, (int) Math.round((5.0 + (width * 0.24 - 5.0) * hover) * layout.uiScale()));
        int iconSize = Math.max(8, (int) Math.round((placement.small() ? 10.0 : 13.0) * layout.uiScale()));
        int iconX = x + Math.max(7, (int) Math.round(10.0 * layout.uiScale()));
        int iconY = y + (height - iconSize) / 2;
        int textX = iconX + iconSize + Math.max(7, (int) Math.round(10.0 * layout.uiScale()));
        int textY = y + (height - (int) Math.round(8.0 * layout.textScale())) / 2;

        guiGraphics.fill(x, y, x + width, y + height, background);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0x99000000);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, innerLine);
        guiGraphics.fill(x + 1, y + 1, x + accentWidth, y + 3, ACCENT_COLOR);

        this.drawIcon(guiGraphics, button.icon(), iconX, iconY, iconSize, active);
        this.drawScaledString(guiGraphics, Lang.tr(button.labelKey()), textX, textY, layout.textScale(), active > 0.02 ? ACTIVE_TEXT_COLOR : TEXT_COLOR, true);
    }

    private void drawIcon(GuiGraphics guiGraphics, IconType icon, int x, int y, int size, double active) {
        int color = this.withAlpha(this.blendRgb(0xBFD4D6, 0xFFF3B0, active), 0.95);
        int shade = this.withAlpha(this.blendRgb(0x243436, 0x65725E, active), 0.95);

        guiGraphics.fill(x, y, x + size, y + size, 0xAA050607);

        switch (icon) {
            case STACK -> {
                guiGraphics.fill(x + size / 4, y + size / 5, x + size - 2, y + size / 2, shade);
                guiGraphics.fill(x + 2, y + size / 2 - 1, x + size - 3, y + size * 3 / 4, color);
                guiGraphics.fill(x + 1, y + size * 3 / 4 - 1, x + size - 4, y + size - 2, shade);
            }
            case FOLDER -> {
                guiGraphics.fill(x + 2, y + size / 4, x + size / 2, y + size / 2, color);
                guiGraphics.fill(x + 2, y + size / 2 - 1, x + size - 2, y + size - 2, shade);
                guiGraphics.fill(x + 3, y + size / 2, x + size - 3, y + size - 3, color);
            }
            case REGION -> {
                guiGraphics.fill(x + 3, y + 3, x + size - 3, y + 5, color);
                guiGraphics.fill(x + 3, y + size - 5, x + size - 3, y + size - 3, color);
                guiGraphics.fill(x + 3, y + 3, x + 5, y + size - 3, color);
                guiGraphics.fill(x + size - 5, y + 3, x + size - 3, y + size - 3, color);
            }
            case CONFIG -> {
                guiGraphics.fill(x + size / 2 - 1, y + 2, x + size / 2 + 2, y + size - 2, color);
                guiGraphics.fill(x + 2, y + size / 2 - 1, x + size - 2, y + size / 2 + 2, color);
                guiGraphics.fill(x + size / 3, y + size / 3, x + size * 2 / 3 + 1, y + size * 2 / 3 + 1, shade);
            }
            case FILES -> {
                guiGraphics.fill(x + 3, y + 2, x + size - 3, y + size - 2, shade);
                guiGraphics.fill(x + 5, y + 4, x + size - 4, y + 5, color);
                guiGraphics.fill(x + 5, y + 7, x + size - 4, y + 8, color);
                guiGraphics.fill(x + 5, y + 10, x + size - 5, y + 11, color);
            }
            case CUSTOM -> {
                guiGraphics.fill(x + 3, y + 3, x + size - 3, y + size - 3, shade);
                guiGraphics.fill(x + size / 3, y + size / 3, x + size * 2 / 3 + 1, y + size * 2 / 3 + 1, color);
            }
        }
    }

    private void runAction(MenuButton button) {
        switch (button) {
            case LOADED_FILES -> GuiBase.openGui(new GuiLoadedLitematicList(true));
            case LOAD_FILE -> GuiBase.openGui(new GuiIteraBlockMainMenu(true));
            case CONFIG -> GuiBase.openGui(new GuiBuilderHelperSettings(true));
            case PLACE_REPLACE_ALL -> BuilderHelperClientConfig.setPlacementReplaceMode(PlacementReplaceMode.REPLACE_ALL);
            case PLACE_ONLY_BLOCKS -> BuilderHelperClientConfig.setPlacementReplaceMode(PlacementReplaceMode.ONLY_REPLACE_BLOCKS);
            case PLACE_ONLY_AIR -> BuilderHelperClientConfig.setPlacementReplaceMode(PlacementReplaceMode.ONLY_REPLACE_AIR);
            default -> {
            }
        }
    }

    private boolean isSelectedPlacementMode(MenuButton button) {
        PlacementReplaceMode mode = BuilderHelperClientConfig.getPlacementReplaceMode();
        return switch (button) {
            case PLACE_REPLACE_ALL -> mode == PlacementReplaceMode.REPLACE_ALL;
            case PLACE_ONLY_BLOCKS -> mode == PlacementReplaceMode.ONLY_REPLACE_BLOCKS;
            case PLACE_ONLY_AIR -> mode == PlacementReplaceMode.ONLY_REPLACE_AIR;
            default -> false;
        };
    }

    private Layout createLayout() {
        double uiScale = this.clamp(Math.min(this.width / 1920.0, this.height / 1080.0), 0.72, 1.0);
        int margin = Math.max(12, (int) Math.round(BASE_SAFE_MARGIN * uiScale));
        int columnGap = Math.max(14, (int) Math.round(BASE_COLUMN_GAP * uiScale));
        int rowGap = Math.max(5, (int) Math.round(BASE_ROW_GAP * uiScale));
        int groupGap = Math.max(14, (int) Math.round(BASE_GROUP_GAP * uiScale));
        int buttonHeight = Math.max(22, (int) Math.round(BASE_BUTTON_HEIGHT * uiScale));
        int smallButtonHeight = Math.max(18, (int) Math.round(BASE_SMALL_BUTTON_HEIGHT * uiScale));
        int availableWidth = Math.max(120, this.width - margin * 2);
        int buttonWidth = Math.min((int) Math.round(BASE_BUTTON_WIDTH * uiScale), Math.max(80, (availableWidth - columnGap) / 2));
        int contentWidth = buttonWidth * 2 + columnGap;
        int startX = Math.max(margin, (this.width - contentWidth) / 2);
        int topY = Math.max(margin + 22, (int) Math.round(this.height * 0.16));
        int totalHeight = buttonHeight * 3 + rowGap * 2 + groupGap + smallButtonHeight;
        int maxTopY = Math.max(margin + 18, this.height - margin - totalHeight);

        if (topY > maxTopY) {
            topY = maxTopY;
        }

        return new Layout(uiScale, startX, topY, buttonWidth, buttonHeight, smallButtonHeight, columnGap, rowGap, groupGap, Math.max(0.62, 0.82 * uiScale), Math.max(0.78, 1.08 * uiScale));
    }

    private List<ButtonPlacement> getButtonPlacements(Layout layout) {
        List<ButtonPlacement> placements = new ArrayList<>();
        int leftX = layout.startX();
        int rightX = layout.startX() + layout.buttonWidth() + layout.columnGap();
        int topY = layout.topY();
        int rowStep = layout.buttonHeight() + layout.rowGap();
        int bottomY = topY + rowStep * 3 + layout.groupGap();
        int smallGap = Math.max(6, layout.rowGap());
        int smallWidth = Math.max(52, (layout.buttonWidth() * 2 + layout.columnGap() - smallGap * 2) / 3);

        placements.add(new ButtonPlacement(MenuButton.LOADED_FILES, leftX, topY, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.LOAD_FILE, leftX, topY + rowStep, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.REGION_EDITOR, leftX, topY + rowStep * 2, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.CONFIG, rightX, topY, layout.buttonWidth(), layout.buttonHeight(), false));
        placements.add(new ButtonPlacement(MenuButton.FILE_MANAGER, rightX, topY + rowStep, layout.buttonWidth(), layout.buttonHeight(), false));
        int modeY = topY + rowStep * 2;
        int modeGap = Math.max(4, layout.rowGap() / 2);
        int modeWidth = Math.max(48, (layout.buttonWidth() - modeGap * 2) / 3);
        placements.add(new ButtonPlacement(MenuButton.PLACE_REPLACE_ALL, rightX, modeY, modeWidth, layout.buttonHeight(), true));
        placements.add(new ButtonPlacement(MenuButton.PLACE_ONLY_BLOCKS, rightX + modeWidth + modeGap, modeY, modeWidth, layout.buttonHeight(), true));
        placements.add(new ButtonPlacement(MenuButton.PLACE_ONLY_AIR, rightX + (modeWidth + modeGap) * 2, modeY, modeWidth, layout.buttonHeight(), true));
        placements.add(new ButtonPlacement(MenuButton.CUSTOM_1, leftX, bottomY, smallWidth, layout.smallButtonHeight(), true));
        placements.add(new ButtonPlacement(MenuButton.CUSTOM_2, leftX + smallWidth + smallGap, bottomY, smallWidth, layout.smallButtonHeight(), true));
        placements.add(new ButtonPlacement(MenuButton.CUSTOM_3, leftX + (smallWidth + smallGap) * 2, bottomY, smallWidth, layout.smallButtonHeight(), true));
        return placements;
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
        LOADED_FILES("iterablock.gui.main_menu.loaded_files", IconType.STACK),
        LOAD_FILE("iterablock.gui.main_menu.load_file", IconType.FOLDER),
        REGION_EDITOR("iterablock.gui.main_menu.region_editor", IconType.REGION),
        CONFIG("iterablock.gui.main_menu.config_button", IconType.CONFIG),
        FILE_MANAGER("iterablock.gui.main_menu.file_manager", IconType.FILES),
        PLACE_REPLACE_ALL("iterablock.gui.main_menu.place_replace_all", IconType.CUSTOM),
        PLACE_ONLY_BLOCKS("iterablock.gui.main_menu.place_only_blocks", IconType.CUSTOM),
        PLACE_ONLY_AIR("iterablock.gui.main_menu.place_only_air", IconType.CUSTOM),
        CUSTOM_1("iterablock.gui.main_menu.custom_1", IconType.CUSTOM),
        CUSTOM_2("iterablock.gui.main_menu.custom_2", IconType.CUSTOM),
        CUSTOM_3("iterablock.gui.main_menu.custom_3", IconType.CUSTOM);

        private final String labelKey;
        private final IconType icon;

        MenuButton(String labelKey, IconType icon) {
            this.labelKey = labelKey;
            this.icon = icon;
        }

        private String labelKey() {
            return this.labelKey;
        }

        private IconType icon() {
            return this.icon;
        }
    }

    private enum IconType {
        STACK,
        FOLDER,
        REGION,
        CONFIG,
        FILES,
        CUSTOM
    }

    private record Layout(double uiScale, int startX, int topY, int buttonWidth, int buttonHeight, int smallButtonHeight, int columnGap, int rowGap, int groupGap, double textScale, double titleScale) {
        private int titleY() {
            return Math.max(8, this.topY - (int) Math.round(34.0 * this.uiScale));
        }
    }

    private record ButtonPlacement(MenuButton button, int x, int y, int width, int height, boolean small) {
    }
}

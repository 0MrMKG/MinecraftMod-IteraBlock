package com.iterablock.client.gui.settings;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared rendering and pointer helpers for configuration controls.
 */
public final class GuiSettingsControls {
    private static final double TEXT_SCALE = 0.52;
    private static final int TEXT_COLOR = 0xFFDDE8E8;
    private static final int BUTTON_FILL = 0x8A4A5050;
    private static final int BUTTON_FILL_HOVER = 0xAA5E6666;
    private static final int BUTTON_BORDER = 0xBFFFFFFF;
    private static final int BUTTON_BORDER_HOVER = 0xE8FFFFFF;
    private static final int VALUE_BORDER = 0xBF858D90;
    private static final int VALUE_BORDER_HOVER = 0xE8B0B8BA;
    private static final int SLIDER_TRACK = 0x665E6666;
    private static final int SLIDER_FILL = 0xD8FFFFFF;
    private static final int SLIDER_KNOB = 0xFFFFFFFF;
    private static final int SLIDER_INSET = 5;

    private GuiSettingsControls() {
    }

    public static boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static int sliderValueFromMouse(int mouseX, int x, int width, int minimum, int maximum) {
        int trackStart = sliderTrackStart(x);
        int trackWidth = sliderTrackWidth(width);
        float progress = Math.max(0.0F, Math.min(1.0F, (mouseX - trackStart) / (float) trackWidth));
        return Math.round(minimum + (maximum - minimum) * progress);
    }

    public static int sliderTrackStart(int x) {
        return x + SLIDER_INSET;
    }

    public static int sliderTrackWidth(int width) {
        return Math.max(1, width - SLIDER_INSET * 2);
    }

    public static void drawButton(GuiGraphics guiGraphics, Font font, String label, int x, int y, int width, int height, double hover) {
        drawBox(guiGraphics, x, y, width, height, hover, false);
        drawCenteredText(guiGraphics, font, label, x, y, width, height, 0xFFFFFFFF);
    }

    public static void drawLabel(GuiGraphics guiGraphics, Font font, String label, int x, int y, int width, int height) {
        drawBox(guiGraphics, x, y, width, height, 0.0, false);
        drawCenteredText(guiGraphics, font, label, x, y, width, height, TEXT_COLOR);
    }

    public static void drawValueField(GuiGraphics guiGraphics, Font font, String value, int x, int y, int width, int height, double hover, int textColor) {
        drawBox(guiGraphics, x, y, width, height, hover, true);
        drawCenteredText(guiGraphics, font, value, x, y, width, height, textColor);
    }

    public static void drawSlider(GuiGraphics guiGraphics, Font font, int value, int minimum, int maximum,
                                  int x, int y, int width, int height, double hover) {
        drawBox(guiGraphics, x, y, width, height, hover, true);

        int trackX = sliderTrackStart(x);
        int trackWidth = sliderTrackWidth(width);
        int trackY = y + height - 4;
        float progress = maximum == minimum ? 0.0F : Math.max(0.0F, Math.min(1.0F, (value - minimum) / (float) (maximum - minimum)));
        int knobX = trackX + Math.round(trackWidth * progress);

        guiGraphics.fill(trackX, trackY, trackX + trackWidth, trackY + 2, SLIDER_TRACK);
        guiGraphics.fill(trackX, trackY, knobX, trackY + 2, SLIDER_FILL);
        guiGraphics.fill(knobX - 1, trackY - 2, knobX + 2, trackY + 4, SLIDER_KNOB);
        drawCenteredText(guiGraphics, font, value + "%", x, y - 1, width, height - 2, TEXT_COLOR);
    }

    private static void drawBox(GuiGraphics guiGraphics, int x, int y, int width, int height, double hover, boolean valueField) {
        boolean hovered = hover > 0.02;
        int fill = hovered ? BUTTON_FILL_HOVER : BUTTON_FILL;
        int border = valueField
                ? hovered ? VALUE_BORDER_HOVER : VALUE_BORDER
                : hovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER;

        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    private static void drawCenteredText(GuiGraphics guiGraphics, Font font, String text, int x, int y, int width, int height, int color) {
        int textWidth = (int) Math.ceil(font.width(text) * TEXT_SCALE);
        int textX = x + Math.max(0, (width - textWidth) / 2);
        int textY = y + Math.max(0, (int) Math.round((height - 8.0 * TEXT_SCALE) / 2.0));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) TEXT_SCALE, (float) TEXT_SCALE, 1.0F);
        guiGraphics.drawString(font, text, (int) Math.round(textX / TEXT_SCALE), (int) Math.round(textY / TEXT_SCALE), color, false);
        guiGraphics.pose().popPose();
    }
}

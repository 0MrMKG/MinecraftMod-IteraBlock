package com.iterablock.client.gui.settings;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/** Shared shell renderer for all IteraBlock configuration screens and their tabs. */
public final class SettingsMenuCommonRenderer {
    public static final int INDICATOR_SEGMENTS = 3;
    private static final double TEXT_SCALE = 0.52;
    private static final int TITLE_COLOR = 0xFFF3EECF;
    private static final int TEXT_COLOR = 0xFFDDE8E8;
    private static final int FRAME_RGB = 0xF4F7F7;
    private static final int INDICATOR_TOP_RGB = 0xF3C6D3;
    private static final int INDICATOR_MIDDLE_RGB = 0xBFE3D7;
    private static final int INDICATOR_BOTTOM_RGB = 0xF6D6A8;
    private static final int PANEL_FILL = 0x7A0B1012;
    private static final int PANEL_TOP_BORDER = 0xBFFFFFFF;
    private static final int PANEL_SIDE_BORDER = 0x66FFFFFF;
    private static final int PANEL_DIVIDER = 0x553D5558;

    private SettingsMenuCommonRenderer() {
    }

    /**
     * Advances the shared tab-indicator spring.  Each segment has its own
     * response curve so the three indicator bars keep the staggered motion
     * used by the original 03 settings screen.
     */
    public static void updateTabIndicatorPid(double[] values, double[] velocities, TabIndicatorState state, double deltaTime) {
        double[] stiffness = {14.0, 24.0, 40.0};
        double[] damping = {6.8, 8.4, 10.0};

        for (int i = 0; i < INDICATOR_SEGMENTS; i++) {
            double target = getTabIndicatorTarget(state, i);
            double error = target - values[i];
            double acceleration = error * stiffness[i] - velocities[i] * damping[i];

            velocities[i] += acceleration * deltaTime;
            values[i] += velocities[i] * deltaTime;

            if (values[i] < 0.0 || values[i] > 1.0) {
                values[i] = Math.max(0.0, Math.min(1.0, values[i]));
                velocities[i] = 0.0;
            }
        }
    }

    public static void drawBackground(GuiGraphics guiGraphics, int width, int height) {
        guiGraphics.fill(0, 0, width, height, 0x76000000);
    }

    public static void drawTitle(GuiGraphics guiGraphics, Font font, String title, int x, int y) {
        drawText(guiGraphics, font, title, x, y + 10, TITLE_COLOR, true);
    }

    public static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int bodyY) {
        int bottom = y + height;
        guiGraphics.fill(x, y, x + width, bottom, PANEL_FILL);
        guiGraphics.fill(x, y, x + width, y + 1, PANEL_TOP_BORDER);
        guiGraphics.fill(x, y, x + 1, bottom, PANEL_SIDE_BORDER);
        guiGraphics.fill(x + width - 1, y, x + width, bottom, PANEL_SIDE_BORDER);
        guiGraphics.fill(x, bottom - 1, x + width, bottom, PANEL_SIDE_BORDER);
        guiGraphics.fill(x, bodyY, x + width, bodyY + 1, PANEL_DIVIDER);
    }

    public static void drawTab(GuiGraphics guiGraphics, Font font, String label, int x, int y, int width, int height,
                               boolean selected, boolean pressed, double hover, double[] indicatorProgress) {
        double clampedHover = Math.max(0.0, Math.min(1.0, hover));
        int fill = selected
                ? PANEL_FILL
                : withAlpha(blendRgb(0x111719, 0x273033, clampedHover), 0.58 + clampedHover * 0.16);

        guiGraphics.fill(x, y, x + width, y + height, fill);
        drawRectFrame(guiGraphics, x, y, width, height, selected ? 0.62 : 0.26 + clampedHover * 0.24);
        if (selected) {
            guiGraphics.fill(x + 1, y + height - 1, x + width - 1, y + height + 1, PANEL_FILL);
        }

        double[] progress = indicatorProgress == null
                ? new double[] {selected || clampedHover > 0.02 ? 1.0 : 0.45, selected || clampedHover > 0.02 ? 1.0 : 0.0, selected || clampedHover > 0.02 ? 1.0 : 0.0}
                : indicatorProgress;
        drawSegmentIndicator(guiGraphics, x + 3, y + height - 6, width - 6, progress, selected, pressed, clampedHover);
        drawCenteredText(guiGraphics, font, fitText(font, label, Math.max(1, width - 6)), x, y, width, height,
                selected || clampedHover > 0.02 ? 0xFFFFF1B0 : TEXT_COLOR);
    }

    private static void drawSegmentIndicator(GuiGraphics guiGraphics, int x, int y, int availableWidth, double[] progress,
                                             boolean selected, boolean pressed, double hover) {
        int shortLength = Math.max(5, Math.min(8, availableWidth));
        int middleLength = Math.max(shortLength + 5, Math.min(18, availableWidth));
        int longLength = Math.max(middleLength + 8, Math.min(36, availableWidth));
        int thickness = selected ? 2 : 1;
        double strongestProgress = Math.max(progress[0], Math.max(progress[1], progress[2]));
        double baseAlpha = selected ? 0.92 : pressed ? 0.55 : hover > 0.02 ? 0.34 + strongestProgress * 0.48 : 0.18;
        double compress = pressed ? 0.86 : 1.0;

        drawIndicatorSegment(guiGraphics, x, y, shortLength, thickness, progress[0] * compress, baseAlpha * 0.88, INDICATOR_TOP_RGB);
        drawIndicatorSegment(guiGraphics, x, y + 2, middleLength, thickness, progress[1] * compress, baseAlpha * 0.92, INDICATOR_MIDDLE_RGB);
        drawIndicatorSegment(guiGraphics, x, y + 4, longLength, thickness, progress[2] * compress, baseAlpha, INDICATOR_BOTTOM_RGB);
    }

    private static double getTabIndicatorTarget(TabIndicatorState state, int segment) {
        return switch (state) {
            case SELECTED, HOVERED -> 1.0;
            case PRESSED -> 0.86;
            case DISABLED -> segment == 0 ? 0.25 : 0.0;
            case NORMAL -> segment == 0 ? 0.45 : 0.0;
        };
    }

    private static void drawIndicatorSegment(GuiGraphics guiGraphics, int x, int y, int length, int thickness,
                                             double progress, double alpha, int color) {
        int visible = Math.max(0, (int) Math.round(length * Math.max(0.0, Math.min(1.0, progress))));
        if (visible <= 0) {
            return;
        }

        drawSmoothLine(guiGraphics, x, y + thickness * 0.5, x + visible, y + thickness * 0.5, thickness + 2.0, withAlpha(color, alpha * 0.18));
        drawSmoothLine(guiGraphics, x, y + thickness * 0.5, x + visible, y + thickness * 0.5, thickness, withAlpha(color, alpha));
    }

    private static void drawRectFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, double alpha) {
        int color = withAlpha(FRAME_RGB, alpha);
        int glow = withAlpha(FRAME_RGB, alpha * 0.05);
        int right = x + width;
        int bottom = y + height;

        drawRectRing(guiGraphics, x, y, right, bottom, 2.2, glow);
        drawRectRing(guiGraphics, x, y, right, bottom, 1.0, color);
    }

    private static void drawRectRing(GuiGraphics guiGraphics, double x, double y, double right, double bottom, double thickness, int color) {
        double innerX = x + thickness;
        double innerY = y + thickness;
        double innerRight = right - thickness;
        double innerBottom = bottom - thickness;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addQuad(buffer, guiGraphics, x, y, right, y, innerRight, innerY, innerX, innerY, color);
        addQuad(buffer, guiGraphics, right, y, right, bottom, innerRight, innerBottom, innerRight, innerY, color);
        addQuad(buffer, guiGraphics, right, bottom, x, bottom, innerX, innerBottom, innerRight, innerBottom, color);
        addQuad(buffer, guiGraphics, x, bottom, x, y, innerX, innerY, innerX, innerBottom, color);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void drawSmoothLine(GuiGraphics guiGraphics, double x1, double y1, double x2, double y2, double thickness, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.001) {
            return;
        }

        double normalX = -dy / length * thickness * 0.5;
        double normalY = dx / length * thickness * 0.5;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addQuad(buffer, guiGraphics, x1 + normalX, y1 + normalY, x2 + normalX, y2 + normalY,
                x2 - normalX, y2 - normalY, x1 - normalX, y1 - normalY, color);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void addQuad(BufferBuilder buffer, GuiGraphics guiGraphics, double x1, double y1, double x2, double y2,
                                double x3, double y3, double x4, double y4, int color) {
        buffer.addVertex(guiGraphics.pose().last(), (float) x1, (float) y1, 0.0F).setColor(color);
        buffer.addVertex(guiGraphics.pose().last(), (float) x2, (float) y2, 0.0F).setColor(color);
        buffer.addVertex(guiGraphics.pose().last(), (float) x3, (float) y3, 0.0F).setColor(color);
        buffer.addVertex(guiGraphics.pose().last(), (float) x4, (float) y4, 0.0F).setColor(color);
    }

    private static void drawCenteredText(GuiGraphics guiGraphics, Font font, String text, int x, int y, int width, int height, int color) {
        int textWidth = (int) Math.ceil(font.width(text) * TEXT_SCALE);
        int textX = x + Math.max(0, (width - textWidth) / 2);
        int textY = y + Math.max(0, (int) Math.round((height - 8.0 * TEXT_SCALE) / 2.0));
        drawText(guiGraphics, font, text, textX, textY, color, false);
    }

    private static void drawText(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) TEXT_SCALE, (float) TEXT_SCALE, 1.0F);
        guiGraphics.drawString(font, text, (int) Math.round(x / TEXT_SCALE), (int) Math.round(y / TEXT_SCALE), color, shadow);
        guiGraphics.pose().popPose();
    }

    private static String fitText(Font font, String text, int maxWidth) {
        if (font.width(text) * TEXT_SCALE <= maxWidth) {
            return text;
        }

        String suffix = "...";
        if (font.width(suffix) * TEXT_SCALE > maxWidth) {
            return "";
        }

        String result = text;
        while (!result.isEmpty() && font.width(result) * TEXT_SCALE + font.width(suffix) * TEXT_SCALE > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isEmpty() ? suffix : result + suffix;
    }

    private static int blendRgb(int first, int second, double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        int red = (int) Math.round(((first >> 16) & 0xFF) + (((second >> 16) & 0xFF) - ((first >> 16) & 0xFF)) * t);
        int green = (int) Math.round(((first >> 8) & 0xFF) + (((second >> 8) & 0xFF) - ((first >> 8) & 0xFF)) * t);
        int blue = (int) Math.round((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * t);
        return red << 16 | green << 8 | blue;
    }

    private static int withAlpha(int rgb, double alpha) {
        int alphaByte = (int) Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0);
        return alphaByte << 24 | rgb;
    }

    public enum TabIndicatorState {
        NORMAL,
        HOVERED,
        SELECTED,
        PRESSED,
        DISABLED
    }
}

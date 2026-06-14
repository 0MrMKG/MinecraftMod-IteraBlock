package com.iterablock.client.gui;

import java.util.List;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.hotkeys.VanillaKeyMappings;
import com.iterablock.client.template.LoadedLitematicManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.glfw.GLFW;

public class GuiRadialMenu extends GuiBase {
    private static final int INNER_RADIUS = 38;
    private static final int OUTER_RADIUS = 98;
    private static final int UNLOAD_OUTER_RADIUS = 122;
    private static final double OPEN_SECONDS = 0.3;
    private static final double CLOSE_SECONDS = 0.3;
    private static final double HOVER_SPEED = 7.5;
    private static final double UNLOAD_HOVER_SPEED = 9.0;
    private static final double PRESS_SPEED = 12.0;
    private static final double REMOVE_SPEED = 7.0;
    private static final int MIN_CIRCLE_SEGMENTS = 64;
    private static final int LARGE_CIRCLE_SEGMENTS = 128;
    private static final double TEXT_SCALE = 0.62;
    private static final int TEXT_LINE_HEIGHT = 6;
    private static final int TEXT_LINE_GAP = 2;
    private static final int[] ENTRY_COLORS = {
            0xE84A5F,
            0xF08A3C,
            0xE7C74A,
            0x55B86A,
            0x4C8FE8,
            0xF3B7C8
    };

    private double[] hoverProgress = new double[0];
    private double[] unloadHoverProgress = new double[0];
    private double[] pressProgress = new double[0];
    private double[] removeProgress = new double[0];
    private double openProgress;
    private long lastFrameNanos;
    private Hit hoveredHit = Hit.NONE;
    private Hit pressedHit = Hit.NONE;
    private LoadedLitematicManager.Entry removingEntry;
    private boolean releaseHandled;
    private boolean wasOpenKeyDown = true;
    private boolean centeredForClose;

    public GuiRadialMenu() {
        this.setTitle("IteraBlock");
    }

    @Override
    public void initGui() {
        this.centerMouseOnCrosshair();
    }

    @Override
    protected void drawScreenBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void drawTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void drawContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        List<LoadedLitematicManager.Entry> entries = LoadedLitematicManager.getEntries();
        this.ensureAnimationSize(entries.size());
        this.updateAnimations(mouseX, mouseY, entries);
        entries = LoadedLitematicManager.getEntries();
        this.ensureAnimationSize(entries.size());

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        double openEase = this.easeOutCubic(this.openProgress);
        double scale = this.getOpenScale();
        double innerRadius = Math.max(4.0, INNER_RADIUS * scale);
        double outerRadius = Math.max(innerRadius + 10.0, OUTER_RADIUS * scale);
        double unloadOuterRadius = Math.max(outerRadius + 8.0, UNLOAD_OUTER_RADIUS * scale);

        if (entries.isEmpty()) {
            this.drawEmptyState(guiGraphics, centerX, centerY, innerRadius, outerRadius, unloadOuterRadius, openEase);
            return;
        }

        this.drawSectors(guiGraphics, entries, centerX, centerY, innerRadius, outerRadius, unloadOuterRadius, openEase, scale);
        this.drawSelectedArrow(guiGraphics, entries, centerX, centerY, unloadOuterRadius, openEase, scale);
        this.drawCenter(guiGraphics, centerX, centerY, innerRadius, openEase);
        this.drawLabels(guiGraphics, entries, centerX, centerY, innerRadius, outerRadius, unloadOuterRadius, openEase, scale);
        this.drawCenterText(guiGraphics, centerX, centerY, openEase);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            List<LoadedLitematicManager.Entry> entries = LoadedLitematicManager.getEntries();
            Hit hit = this.getHitAt(mouseX, mouseY, this.getOpenScale(), entries);

            if (hit.type() != HitType.NONE) {
                this.pressedHit = hit;
                return true;
            }
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.pressedHit.type() != HitType.NONE) {
            Hit releasedHit = this.getHitAt(mouseX, mouseY, this.getOpenScale(), LoadedLitematicManager.getEntries());

            if (releasedHit.sameTarget(this.pressedHit)) {
                this.handleHit(releasedHit);

                if (releasedHit.type() == HitType.SELECT) {
                    this.closeRadialMenu();
                }

                return true;
            }

            this.pressedHit = Hit.NONE;
        }

        return super.onMouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.closeRadialMenu();
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    private void updateAnimations(int mouseX, int mouseY, List<LoadedLitematicManager.Entry> entries) {
        long now = System.nanoTime();
        double deltaTime = this.lastFrameNanos == 0L ? 1.0 / 60.0 : (now - this.lastFrameNanos) / 1_000_000_000.0;
        this.lastFrameNanos = now;
        deltaTime = Math.max(0.0, Math.min(0.05, deltaTime));

        boolean openKeyDown = this.isOpenKeyDown();
        this.hoveredHit = this.getHitAt(mouseX, mouseY, this.getOpenScale(), entries);

        if (openKeyDown) {
            this.releaseHandled = false;
            this.centeredForClose = false;
            this.openProgress = Math.min(1.0, this.openProgress + deltaTime / OPEN_SECONDS);
        } else {
            if (this.wasOpenKeyDown && !this.centeredForClose) {
                this.centerMouseOnCrosshair();
                this.centeredForClose = true;
            }

            if (!this.releaseHandled && this.hoveredHit.type() != HitType.NONE) {
                this.handleHit(this.hoveredHit);
                this.releaseHandled = true;
            }

            this.openProgress = Math.max(0.0, this.openProgress - deltaTime / CLOSE_SECONDS);

            if (this.openProgress <= 0.0) {
                this.closeRadialMenu();
                return;
            }
        }

        this.wasOpenKeyDown = openKeyDown;
        this.updateEntryAnimations(entries, deltaTime);
    }

    private void updateEntryAnimations(List<LoadedLitematicManager.Entry> entries, double deltaTime) {
        boolean leftMouseDown = this.isLeftMouseDown();

        if (!leftMouseDown) {
            this.pressedHit = Hit.NONE;
        }

        for (int i = 0; i < entries.size(); i++) {
            double hoverTarget = this.hoveredHit.type() == HitType.SELECT && this.hoveredHit.index() == i ? 1.0 : 0.0;
            double unloadTarget = this.hoveredHit.type() == HitType.UNLOAD && this.hoveredHit.index() == i ? 1.0 : 0.0;
            double pressTarget = this.pressedHit.index() == i && leftMouseDown ? 1.0 : 0.0;
            double removeTarget = entries.get(i) == this.removingEntry ? 1.0 : 0.0;

            this.hoverProgress[i] = this.approach(this.hoverProgress[i], hoverTarget, HOVER_SPEED, deltaTime);
            this.unloadHoverProgress[i] = this.approach(this.unloadHoverProgress[i], unloadTarget, UNLOAD_HOVER_SPEED, deltaTime);
            this.pressProgress[i] = this.approach(this.pressProgress[i], pressTarget, PRESS_SPEED, deltaTime);
            this.removeProgress[i] = this.approach(this.removeProgress[i], removeTarget, REMOVE_SPEED, deltaTime);

            if (this.removingEntry == entries.get(i) && this.removeProgress[i] >= 1.0) {
                LoadedLitematicManager.unload(this.removingEntry);
                this.removingEntry = null;
                this.pressedHit = Hit.NONE;
                this.hoveredHit = Hit.NONE;
                this.resetAnimations();
                break;
            }
        }
    }

    private void drawSectors(GuiGraphics guiGraphics, List<LoadedLitematicManager.Entry> entries, int centerX, int centerY, double innerRadius, double outerRadius, double unloadOuterRadius, double openEase, double scale) {
        if (this.hoverProgress.length < entries.size() || this.unloadHoverProgress.length < entries.size() || this.pressProgress.length < entries.size() || this.removeProgress.length < entries.size()) {
            this.ensureAnimationSize(entries.size());
        }

        for (int i = 0; i < entries.size(); i++) {
            LoadedLitematicManager.Entry entry = entries.get(i);
            double hover = this.hoverProgress[i];
            double unloadHover = this.unloadHoverProgress[i];
            double press = this.pressProgress[i];
            double remove = this.removeProgress[i];
            boolean selected = entry == LoadedLitematicManager.selectedEntry;
            double angle = this.getSectorCenterAngle(i, entries.size());
            double sectorAngle = Math.PI * 2.0 / entries.size();
            double startAngle = angle - sectorAngle * 0.5;
            double endAngle = angle + sectorAngle * 0.5;
            double alpha = (1.0 - remove) * openEase;
            double pressScale = 1.0 - press * 0.035 - remove * 0.20;
            double radialOffset = (hover * 7.0 - press * 3.0) * scale;
            double drawCenterX = centerX + Math.cos(angle) * radialOffset;
            double drawCenterY = centerY + Math.sin(angle) * radialOffset;
            double sectorInner = Math.max(2.0, (innerRadius + hover * 4.0 * scale - press * 2.0 * scale) * pressScale);
            double sectorOuter = Math.max(sectorInner + 6.0, (outerRadius + hover * 6.0 * scale) * pressScale);
            double unloadOuter = Math.max(sectorOuter + 6.0, (unloadOuterRadius + unloadHover * 8.0 * scale) * pressScale);
            int entryColor = selected ? this.getEntryColor(i) : this.getMutedEntryColor(i);
            double sectorAlpha = selected ? 0.90 : 0.60;
            int sectorColor = this.withAlpha(entryColor, Math.max(0.0, sectorAlpha - press * 0.06) * alpha);

            if (hover > 0.01 || selected) {
                this.drawSector(guiGraphics, drawCenterX, drawCenterY, Math.max(1.0, sectorInner - 3.0), sectorOuter + 5.0, startAngle, endAngle, this.withAlpha(entryColor, (hover * 0.18 + (selected ? 0.16 : 0.0)) * alpha));
            }

            this.drawSector(guiGraphics, drawCenterX, drawCenterY, sectorInner, sectorOuter, startAngle, endAngle, sectorColor);

            if (unloadHover > 0.01) {
                this.drawSector(guiGraphics, centerX, centerY, outerRadius + 1.0, unloadOuter, startAngle, endAngle, this.withAlpha(0xB4465A, (0.18 + unloadHover * 0.34) * alpha));
            } else {
                this.drawSector(guiGraphics, centerX, centerY, outerRadius + 1.0, unloadOuterRadius, startAngle, endAngle, this.withAlpha(0x7E2C38, 0.08 * alpha));
            }
        }
    }

    private void drawCenter(GuiGraphics guiGraphics, int centerX, int centerY, double innerRadius, double openEase) {
        double fillRadius = Math.max(4.0, innerRadius - 5.0);

        this.drawCircle(guiGraphics, centerX, centerY, fillRadius, this.withAlpha(0x0B1822, 0.78 * openEase));
        this.drawCircle(guiGraphics, centerX, centerY - Math.max(1.0, innerRadius / 5.0), Math.max(2.0, innerRadius / 2.0), this.withAlpha(0xFFFFFF, 0.09 * openEase));
    }

    private void drawEmptyState(GuiGraphics guiGraphics, int centerX, int centerY, double innerRadius, double outerRadius, double unloadOuterRadius, double openEase) {
        this.drawEmptyRing(guiGraphics, centerX, centerY, innerRadius, outerRadius, unloadOuterRadius, openEase);
        this.drawCenter(guiGraphics, centerX, centerY, innerRadius, openEase);
        this.drawCenteredTextBlock(guiGraphics, centerX, centerY, new String[] { Lang.tr("iterablock.gui.radial.no_loaded"), Lang.tr("iterablock.gui.radial.press_i") }, new int[] { this.withAlpha(0xEAFBFF, openEase), this.withAlpha(0xA7D9E6, openEase) });
    }

    private void drawEmptyRing(GuiGraphics guiGraphics, int centerX, int centerY, double innerRadius, double outerRadius, double unloadOuterRadius, double openEase) {
        this.drawSector(guiGraphics, centerX, centerY, innerRadius, outerRadius, 0.0, Math.PI * 2.0, this.withAlpha(0x0A1118, 0.36 * openEase));
        this.drawSector(guiGraphics, centerX, centerY, outerRadius + 1.0, unloadOuterRadius, 0.0, Math.PI * 2.0, this.withAlpha(0x102431, 0.18 * openEase));
    }

    private void drawLabels(GuiGraphics guiGraphics, List<LoadedLitematicManager.Entry> entries, int centerX, int centerY, double innerRadius, double outerRadius, double unloadOuterRadius, double openEase, double scale) {
        if (this.hoverProgress.length < entries.size() || this.unloadHoverProgress.length < entries.size() || this.pressProgress.length < entries.size() || this.removeProgress.length < entries.size()) {
            this.ensureAnimationSize(entries.size());
        }

        double baseLabelRadius = (innerRadius + outerRadius) / 2.0;

        for (int i = 0; i < entries.size(); i++) {
            double hover = this.hoverProgress[i];
            double unloadHover = this.unloadHoverProgress[i];
            double press = this.pressProgress[i];
            double remove = this.removeProgress[i];
            double angle = this.getSectorCenterAngle(i, entries.size());
            double labelRadius = baseLabelRadius + (hover * 7.0 - press * 3.0) * scale;
            String[] labelLines = this.wrapLabel(entries.get(i).displayName(), 10);
            int x = centerX + (int) Math.round(Math.cos(angle) * labelRadius);
            int y = centerY + (int) Math.round(Math.sin(angle) * labelRadius);
            boolean selected = entries.get(i) == LoadedLitematicManager.selectedEntry;
            double alpha = (0.62 + hover * 0.30 + (selected ? 0.18 : 0.0)) * (1.0 - remove) * openEase;

            this.drawCenteredTextBlock(guiGraphics, x, y, labelLines, this.withAlpha(selected ? 0xFFFFFF : 0xD9E3E6, alpha));

            if (unloadHover > 0.02) {
                String unload = Lang.tr("iterablock.gui.radial.unload");
                double unloadRadius = (outerRadius + unloadOuterRadius) / 2.0 + unloadHover * 5.0 * scale;
                int unloadX = centerX + (int) Math.round(Math.cos(angle) * unloadRadius);
                int unloadY = centerY + (int) Math.round(Math.sin(angle) * unloadRadius) - TEXT_LINE_HEIGHT / 2;
                this.drawCentered(guiGraphics, unload, unloadX, unloadY, this.withAlpha(0xFFD6DC, unloadHover * openEase));
            }
        }
    }

    private void drawCenterText(GuiGraphics guiGraphics, int centerX, int centerY, double openEase) {
        String[] lines;
        int[] colors;

        if (this.hoveredHit.type() == HitType.SELECT) {
            lines = this.withPrefixLine(Lang.tr("iterablock.gui.radial.select"), this.wrapLabel(this.hoveredHit.entry().displayName(), 13));
            colors = this.centerColors(lines.length, this.withAlpha(0xEAFBFF, openEase), this.withAlpha(0xFFFFFF, openEase));
        } else if (this.hoveredHit.type() == HitType.UNLOAD) {
            lines = this.withPrefixLine(Lang.tr("iterablock.gui.radial.unload"), this.wrapLabel(this.hoveredHit.entry().displayName(), 13));
            colors = this.centerColors(lines.length, this.withAlpha(0xFFD6DC, openEase), this.withAlpha(0xFFFFFF, openEase));
        } else if (LoadedLitematicManager.selectedEntry != null) {
            lines = this.wrapLabel(LoadedLitematicManager.selectedEntry.displayName(), 13);
            colors = this.centerColors(lines.length, this.withAlpha(0xEAFBFF, openEase), this.withAlpha(0xEAFBFF, openEase));
        } else {
            lines = new String[] { "IteraBlock" };
            colors = this.centerColors(lines.length, this.withAlpha(0xEAFBFF, openEase), this.withAlpha(0xEAFBFF, openEase));
        }

        this.drawCenterTextBackdrop(guiGraphics, centerX, centerY, lines, openEase);
        this.drawCenteredTextBlock(guiGraphics, centerX, centerY, lines, colors);
    }

    private Hit getHitAt(int mouseX, int mouseY, double scale, List<LoadedLitematicManager.Entry> entries) {
        if (entries.isEmpty()) {
            return Hit.NONE;
        }

        int dx = mouseX - this.width / 2;
        int dy = mouseY - this.height / 2;
        int innerRadius = Math.max(4, (int) Math.round(INNER_RADIUS * scale));
        int outerRadius = Math.max(innerRadius + 10, (int) Math.round(OUTER_RADIUS * scale));
        int unloadOuterRadius = Math.max(outerRadius + 8, (int) Math.round(UNLOAD_OUTER_RADIUS * scale));
        int distanceSquared = dx * dx + dy * dy;

        if (distanceSquared <= innerRadius * innerRadius || distanceSquared > unloadOuterRadius * unloadOuterRadius) {
            return Hit.NONE;
        }

        int index = this.getSectorIndex(dx, dy, entries.size());

        if (index < 0 || index >= entries.size()) {
            return Hit.NONE;
        }

        if (distanceSquared <= outerRadius * outerRadius) {
            return new Hit(HitType.SELECT, index, entries.get(index));
        }

        return new Hit(HitType.UNLOAD, index, entries.get(index));
    }

    private void handleHit(Hit hit) {
        if (hit.type() == HitType.SELECT) {
            LoadedLitematicManager.select(hit.entry());
        } else if (hit.type() == HitType.UNLOAD) {
            this.removingEntry = hit.entry();
        }
    }

    private void drawSelectedArrow(GuiGraphics guiGraphics, List<LoadedLitematicManager.Entry> entries, int centerX, int centerY, double unloadOuterRadius, double openEase, double scale) {
        int selectedIndex = entries.indexOf(LoadedLitematicManager.selectedEntry);

        if (selectedIndex < 0) {
            return;
        }

        double angle = this.getSectorCenterAngle(selectedIndex, entries.size());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double tangentX = -sin;
        double tangentY = cos;
        double tipRadius = unloadOuterRadius + 12.0 * scale;
        double baseRadius = unloadOuterRadius + 4.0 * scale;
        double halfWidth = 5.0 * scale;
        double tipX = centerX + cos * tipRadius;
        double tipY = centerY + sin * tipRadius;
        double baseX = centerX + cos * baseRadius;
        double baseY = centerY + sin * baseRadius;
        int color = this.withAlpha(this.getEntryColor(selectedIndex), 0.86 * openEase);

        this.drawTriangle(guiGraphics,
                tipX, tipY,
                baseX + tangentX * halfWidth, baseY + tangentY * halfWidth,
                baseX - tangentX * halfWidth, baseY - tangentY * halfWidth,
                color);
    }

    private int getSectorIndex(double dx, double dy, int sectorCount) {
        if (sectorCount <= 0) {
            return -1;
        }

        double sectorAngle = Math.PI * 2.0 / sectorCount;
        double startAngle = -Math.PI / 2.0 - sectorAngle / 2.0;
        double angle = this.normalizeAngle(Math.atan2(dy, dx) - startAngle);
        return Math.min(sectorCount - 1, (int) (angle / sectorAngle));
    }

    private double getSectorCenterAngle(int index, int sectorCount) {
        double sectorAngle = Math.PI * 2.0 / sectorCount;
        double startAngle = -Math.PI / 2.0 - sectorAngle / 2.0;
        return startAngle + sectorAngle * (index + 0.5);
    }

    private double normalizeAngle(double angle) {
        while (angle < 0.0) {
            angle += Math.PI * 2.0;
        }

        while (angle >= Math.PI * 2.0) {
            angle -= Math.PI * 2.0;
        }

        return angle;
    }

    private double getOpenScale() {
        return 0.15 + 0.85 * this.easeOutCubic(this.openProgress);
    }

    private double easeOutCubic(double value) {
        double inverse = 1.0 - value;
        return 1.0 - inverse * inverse * inverse;
    }

    private double approach(double current, double target, double speed, double deltaTime) {
        double step = speed * deltaTime;

        if (current < target) {
            return Math.min(target, current + step);
        }

        return Math.max(target, current - step);
    }

    private boolean isLeftMouseDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private boolean isOpenKeyDown() {
        return VanillaKeyMappings.isOpenRadialDown() || BuilderHelperClientConfig.isOpenRadialKeyDown();
    }

    private void closeRadialMenu() {
        this.centerMouseOnCrosshair();
        this.closeGui(true);
    }

    private void centerMouseOnCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();
        GLFW.glfwSetCursorPos(window, minecraft.getWindow().getScreenWidth() / 2.0, minecraft.getWindow().getScreenHeight() / 2.0);
    }

    private void drawSector(GuiGraphics guiGraphics, double centerX, double centerY, double innerRadius, double outerRadius, double startAngle, double endAngle, int color) {
        if (((color >>> 24) & 0xFF) == 0 || outerRadius <= innerRadius) {
            return;
        }

        int segments = this.getArcSegments(outerRadius, endAngle - startAngle);
        BufferBuilder buffer = this.beginMesh(VertexFormat.Mode.TRIANGLE_STRIP);

        for (int i = 0; i <= segments; i++) {
            double angle = startAngle + (endAngle - startAngle) * i / segments;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            this.addVertex(guiGraphics, buffer, centerX + cos * outerRadius, centerY + sin * outerRadius, color);
            this.addVertex(guiGraphics, buffer, centerX + cos * innerRadius, centerY + sin * innerRadius, color);
        }

        this.drawMesh(buffer);
    }

    private void drawCircle(GuiGraphics guiGraphics, double centerX, double centerY, double radius, int color) {
        if (((color >>> 24) & 0xFF) == 0 || radius <= 0.0) {
            return;
        }

        int segments = Math.max(MIN_CIRCLE_SEGMENTS, this.getArcSegments(radius, Math.PI * 2.0));
        BufferBuilder buffer = this.beginMesh(VertexFormat.Mode.TRIANGLE_FAN);

        this.addVertex(guiGraphics, buffer, centerX, centerY, color);

        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            this.addVertex(guiGraphics, buffer, centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius, color);
        }

        this.drawMesh(buffer);
    }

    private void drawTriangle(GuiGraphics guiGraphics, double x1, double y1, double x2, double y2, double x3, double y3, int color) {
        if (((color >>> 24) & 0xFF) == 0) {
            return;
        }

        BufferBuilder buffer = this.beginMesh(VertexFormat.Mode.TRIANGLES);
        this.addVertex(guiGraphics, buffer, x1, y1, color);
        this.addVertex(guiGraphics, buffer, x2, y2, color);
        this.addVertex(guiGraphics, buffer, x3, y3, color);
        this.drawMesh(buffer);
    }

    private int getArcSegments(double radius, double angleSpan) {
        double normalizedSpan = Math.max(0.0, Math.min(Math.PI * 2.0, Math.abs(angleSpan)));
        int fullCircleSegments = radius >= UNLOAD_OUTER_RADIUS * 0.8 ? LARGE_CIRCLE_SEGMENTS : 96;
        fullCircleSegments = Math.max(MIN_CIRCLE_SEGMENTS, fullCircleSegments);
        return Math.max(4, (int) Math.ceil(fullCircleSegments * normalizedSpan / (Math.PI * 2.0)));
    }

    private BufferBuilder beginMesh(VertexFormat.Mode mode) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        return Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
    }

    private void addVertex(GuiGraphics guiGraphics, BufferBuilder buffer, double x, double y, int color) {
        buffer.addVertex(guiGraphics.pose().last(), (float) x, (float) y, 0.0F).setColor(color);
    }

    private void drawMesh(BufferBuilder buffer) {
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void drawCentered(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        double scaledWidth = this.getStringWidth(text) * TEXT_SCALE;
        this.drawScaledString(guiGraphics, text, centerX - scaledWidth / 2.0, y, color);
    }

    private void drawScaledString(GuiGraphics guiGraphics, String text, double x, double y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) TEXT_SCALE, (float) TEXT_SCALE, 1.0F);
        this.drawStringWithShadow(guiGraphics, text, (int) Math.round(x / TEXT_SCALE), (int) Math.round(y / TEXT_SCALE), color);
        guiGraphics.pose().popPose();
    }

    private void drawCenteredTextBlock(GuiGraphics guiGraphics, int centerX, int centerY, String[] lines, int[] colors) {
        int totalHeight = lines.length * TEXT_LINE_HEIGHT + Math.max(0, lines.length - 1) * TEXT_LINE_GAP;
        int y = centerY - totalHeight / 2;

        for (int i = 0; i < lines.length; i++) {
            this.drawCentered(guiGraphics, lines[i], centerX, y, colors[i]);
            y += TEXT_LINE_HEIGHT + TEXT_LINE_GAP;
        }
    }

    private void drawCenteredTextBlock(GuiGraphics guiGraphics, int centerX, int centerY, String[] lines, int color) {
        int[] colors = new int[lines.length];

        for (int i = 0; i < colors.length; i++) {
            colors[i] = color;
        }

        this.drawCenteredTextBlock(guiGraphics, centerX, centerY, lines, colors);
    }

    private void drawCenterTextBackdrop(GuiGraphics guiGraphics, int centerX, int centerY, String[] lines, double openEase) {
        double maxWidth = 0.0;

        for (String line : lines) {
            maxWidth = Math.max(maxWidth, this.getStringWidth(line) * TEXT_SCALE);
        }

        double totalHeight = lines.length * TEXT_LINE_HEIGHT + Math.max(0, lines.length - 1) * TEXT_LINE_GAP;
        double radius = Math.max(24.0, Math.max(maxWidth / 2.0 + 14.0, totalHeight / 2.0 + 12.0));

        this.drawCircle(guiGraphics, centerX, centerY, radius + 5.0, this.withAlpha(0x03070A, 0.28 * openEase));
        this.drawCircle(guiGraphics, centerX, centerY, radius + 2.0, this.withAlpha(0x071116, 0.86 * openEase));
        this.drawCircle(guiGraphics, centerX, centerY - radius * 0.20, radius * 0.58, this.withAlpha(0xFFFFFF, 0.07 * openEase));
    }

    private int[] centerColors(int length, int firstColor, int restColor) {
        int[] colors = new int[length];

        for (int i = 0; i < colors.length; i++) {
            colors[i] = i == 0 ? firstColor : restColor;
        }

        return colors;
    }

    private String[] wrapLabel(String text, int maxLineLength) {
        if (text == null || text.isBlank()) {
            return new String[] { "" };
        }

        String normalized = text.strip();

        if (normalized.length() <= maxLineLength) {
            return new String[] { normalized };
        }

        int split = this.findWrapIndex(normalized, maxLineLength);
        String first = normalized.substring(0, split).strip();
        String second = normalized.substring(split).strip();

        if (second.length() > maxLineLength) {
            second = second.substring(0, Math.max(1, maxLineLength - 3)).strip() + "...";
        }

        return new String[] { first, second };
    }

    private int findWrapIndex(String text, int maxLineLength) {
        int limit = Math.min(maxLineLength, text.length() - 1);

        for (int i = limit; i >= Math.max(1, limit - 4); i--) {
            char character = text.charAt(i);

            if (character == ' ' || character == '_' || character == '-' || character == '.') {
                return i + 1;
            }
        }

        return limit;
    }

    private String[] withPrefixLine(String prefix, String[] lines) {
        String[] result = new String[lines.length + 1];
        result[0] = prefix;
        System.arraycopy(lines, 0, result, 1, lines.length);
        return result;
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, Math.max(1, maxLength - 3)) + "...";
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

    private int getEntryColor(int index) {
        return ENTRY_COLORS[index % ENTRY_COLORS.length];
    }

    private int getMutedEntryColor(int index) {
        return this.blendRgb(this.getEntryColor(index), 0x9AA2A4, 0.62);
    }

    private void ensureAnimationSize(int size) {
        if (this.hoverProgress.length == size) {
            return;
        }

        this.hoverProgress = copyToSize(this.hoverProgress, size);
        this.unloadHoverProgress = copyToSize(this.unloadHoverProgress, size);
        this.pressProgress = copyToSize(this.pressProgress, size);
        this.removeProgress = copyToSize(this.removeProgress, size);
    }

    private static double[] copyToSize(double[] oldValues, int size) {
        double[] values = new double[size];
        System.arraycopy(oldValues, 0, values, 0, Math.min(oldValues.length, size));
        return values;
    }

    private void resetAnimations() {
        this.hoverProgress = new double[0];
        this.unloadHoverProgress = new double[0];
        this.pressProgress = new double[0];
        this.removeProgress = new double[0];
    }

    private enum HitType {
        NONE,
        SELECT,
        UNLOAD
    }

    private record Hit(HitType type, int index, LoadedLitematicManager.Entry entry) {
        private static final Hit NONE = new Hit(HitType.NONE, -1, null);

        boolean sameTarget(Hit other) {
            return this.type == other.type && this.entry == other.entry;
        }
    }
}

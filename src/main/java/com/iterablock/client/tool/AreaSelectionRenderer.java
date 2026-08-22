package com.iterablock.client.tool;

import com.iterablock.client.config.BuilderHelperClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;

/**
 * Shared world overlay for every selection driven by a first and second block position.
 */
public final class AreaSelectionRenderer {
    private static final AreaSelectionRenderer INSTANCE = new AreaSelectionRenderer();
    private static final double OVERLAY_EPSILON = 0.003D;
    private static final int SELECTION_FILL_RGB = 0xFFFFFF;
    private static final int SELECTION_LINE_COLOR = 0xF4D6FFF0;
    private static final int FIRST_POINT_COLOR = 0x80FF0000;
    private static final int SECOND_POINT_COLOR = 0x800000FF;
    private static final int POINT_BORDER_COLOR = 0xFFFFFFFF;

    private AreaSelectionRenderer() {
    }

    public static AreaSelectionRenderer getInstance() {
        return INSTANCE;
    }

    public void render(PoseStack poseStack) {
        if (ToolState.getMode() != ToolMode.AREA_COPY_PASTE && !ToolState.isRandomAreaMode()) {
            return;
        }

        BlockPos first = AreaSelectionState.getFirstCorner();
        BlockPos second = AreaSelectionState.getSecondCorner();

        if (first != null) {
            this.renderPoint(poseStack, first, FIRST_POINT_COLOR);
        }

        if (second != null) {
            this.renderPoint(poseStack, second, SECOND_POINT_COLOR);
        }

        if (first == null || second == null) {
            return;
        }

        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX()) + 1;
        int maxY = Math.max(first.getY(), second.getY()) + 1;
        int maxZ = Math.max(first.getZ(), second.getZ()) + 1;
        this.renderFilledBox(poseStack, minX, minY, minZ, maxX, maxY, maxZ, this.getSelectionFillColor());
        this.renderLineBox(poseStack, minX, minY, minZ, maxX, maxY, maxZ, SELECTION_LINE_COLOR);
    }

    private void renderPoint(PoseStack poseStack, BlockPos point, int color) {
        this.renderFilledBox(poseStack, point.getX(), point.getY(), point.getZ(), point.getX() + 1, point.getY() + 1, point.getZ() + 1, color);
        this.renderLineBox(poseStack, point.getX(), point.getY(), point.getZ(), point.getX() + 1, point.getY() + 1, point.getZ() + 1, POINT_BORDER_COLOR);
    }

    private int getSelectionFillColor() {
        int alpha = Math.round(BuilderHelperClientConfig.getSelectionFillOpacity() * 255.0F / 100.0F);
        return alpha << 24 | SELECTION_FILL_RGB;
    }

    private void renderFilledBox(PoseStack poseStack, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
        this.renderDepthAwareFilledBox(poseStack,
                minX - OVERLAY_EPSILON,
                minY - OVERLAY_EPSILON,
                minZ - OVERLAY_EPSILON,
                maxX + OVERLAY_EPSILON,
                maxY + OVERLAY_EPSILON,
                maxZ + OVERLAY_EPSILON,
                color);
    }

    private void renderDepthAwareFilledBox(PoseStack poseStack, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        this.addBoxQuads(buffer, poseStack, minX, minY, minZ, maxX, maxY, maxZ, color);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderLineBox(PoseStack poseStack, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
        minX -= OVERLAY_EPSILON;
        minY -= OVERLAY_EPSILON;
        minZ -= OVERLAY_EPSILON;
        maxX += OVERLAY_EPSILON;
        maxY += OVERLAY_EPSILON;
        maxZ += OVERLAY_EPSILON;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        this.addBoxEdges(buffer, poseStack, minX, minY, minZ, maxX, maxY, maxZ, color);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void addBoxQuads(BufferBuilder buffer, PoseStack poseStack, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
        this.addQuad(buffer, poseStack, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color);
        this.addQuad(buffer, poseStack, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        this.addQuad(buffer, poseStack, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
        this.addQuad(buffer, poseStack, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        this.addQuad(buffer, poseStack, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color);
        this.addQuad(buffer, poseStack, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
    }

    private void addBoxEdges(BufferBuilder buffer, PoseStack poseStack, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
        this.addEdge(buffer, poseStack, minX, minY, minZ, maxX, minY, minZ, color);
        this.addEdge(buffer, poseStack, maxX, minY, minZ, maxX, minY, maxZ, color);
        this.addEdge(buffer, poseStack, maxX, minY, maxZ, minX, minY, maxZ, color);
        this.addEdge(buffer, poseStack, minX, minY, maxZ, minX, minY, minZ, color);
        this.addEdge(buffer, poseStack, minX, maxY, minZ, maxX, maxY, minZ, color);
        this.addEdge(buffer, poseStack, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        this.addEdge(buffer, poseStack, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        this.addEdge(buffer, poseStack, minX, maxY, maxZ, minX, maxY, minZ, color);
        this.addEdge(buffer, poseStack, minX, minY, minZ, minX, maxY, minZ, color);
        this.addEdge(buffer, poseStack, maxX, minY, minZ, maxX, maxY, minZ, color);
        this.addEdge(buffer, poseStack, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        this.addEdge(buffer, poseStack, minX, minY, maxZ, minX, maxY, maxZ, color);
    }

    private void addQuad(BufferBuilder buffer, PoseStack poseStack, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, int color) {
        this.addVertex(buffer, poseStack, x1, y1, z1, color);
        this.addVertex(buffer, poseStack, x2, y2, z2, color);
        this.addVertex(buffer, poseStack, x3, y3, z3, color);
        this.addVertex(buffer, poseStack, x4, y4, z4, color);
    }

    private void addEdge(BufferBuilder buffer, PoseStack poseStack, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        this.addVertex(buffer, poseStack, x1, y1, z1, color);
        this.addVertex(buffer, poseStack, x2, y2, z2, color);
    }

    private void addVertex(BufferBuilder buffer, PoseStack poseStack, double x, double y, double z, int color) {
        buffer.addVertex(poseStack.last(), (float) x, (float) y, (float) z).setColor(color);
    }
}

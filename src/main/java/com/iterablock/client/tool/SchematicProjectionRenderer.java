package com.iterablock.client.tool;

import java.util.List;

import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.litematica.LitematicaSchematicInfo;
import com.iterablock.client.template.LoadedLitematicManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class SchematicProjectionRenderer {
    private static final SchematicProjectionRenderer INSTANCE = new SchematicProjectionRenderer();
    private static final int MAX_RENDERED_BOXES = 8192;
    private static final int BOUNDING_BOX_COLOR = 0xD08AE8FF;
    private static final int TARGET_FILL_COLOR = 0x4400FF66;
    private static final int TARGET_LINE_COLOR = 0xF000FF66;

    private SchematicProjectionRenderer() {
    }

    public static SchematicProjectionRenderer getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        this.renderActivePlacement(minecraft, poseStack);
        this.renderPlacementTarget(poseStack);
        poseStack.popPose();
    }

    private boolean hasRenderableBlocks(LitematicaSchematicInfo info) {
        for (LitematicaSchematicInfo.Region region : info.regions()) {
            if (!region.blocks().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void renderActivePlacement(Minecraft minecraft, PoseStack poseStack) {
        if (!SchematicPlacementState.hasPlacement()) {
            return;
        }

        LoadedLitematicManager.Entry entry = SchematicPlacementState.getEntry();
        BlockPos origin = SchematicPlacementState.getOrigin();

        if (entry == null || origin == null || !this.hasRenderableBlocks(entry.info())) {
            return;
        }

        int copies = ToolState.getMode() == ToolMode.LINEAR_ARRAY ? SchematicPlacementState.getLinearArrayCopyCount() : 1;
        int realRenderLimit = Math.min(copies, ToolState.getMode() == ToolMode.LINEAR_ARRAY ? BuilderHelperClientConfig.getLinearArrayRenderLimit() : copies);
        BlockPos arrayStep = SchematicPlacementState.getLinearArrayStep(entry.info());
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        if (realRenderLimit > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.8F);

            for (int copy = 0; copy < realRenderLimit; copy++) {
                BlockPos copyOrigin = origin.offset(SchematicPlacementState.getLinearArrayOffset(copy, arrayStep));
                this.renderPlacement(minecraft, poseStack, bufferSource, copyOrigin, entry.info());
            }

            bufferSource.endBatch();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }

        for (int copy = 0; copy < copies; copy++) {
            BlockPos copyOrigin = origin.offset(SchematicPlacementState.getLinearArrayOffset(copy, arrayStep));
            Bounds bounds = this.findPlacementBounds(copyOrigin, entry.info());

            if (bounds != null) {
                this.renderLineBox(poseStack, bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ(), BOUNDING_BOX_COLOR);
            }
        }
    }

    private void renderPlacement(Minecraft minecraft, PoseStack poseStack, MultiBufferSource bufferSource, BlockPos origin, LitematicaSchematicInfo info) {
        int rendered = 0;

        for (LitematicaSchematicInfo.Region region : info.regions()) {
            List<LitematicaSchematicInfo.BlockSample> blocks = region.blocks();

            for (LitematicaSchematicInfo.BlockSample block : blocks) {
                if (rendered++ >= MAX_RENDERED_BOXES) {
                    break;
                }

                BlockPos pos = origin.offset(SchematicPlacementState.transformBlockOffset(region.position(), block.pos(), region.size()));
                BlockState state = SchematicPlacementState.transformState(block.state());
                this.renderBlock(minecraft, poseStack, bufferSource, pos, state);
            }

            if (rendered >= MAX_RENDERED_BOXES) {
                break;
            }
        }
    }

    private void renderBlock(Minecraft minecraft, PoseStack poseStack, MultiBufferSource bufferSource, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
        minecraft.getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.translucent());
        poseStack.popPose();
    }

    private Bounds findPlacementBounds(BlockPos origin, LitematicaSchematicInfo info) {
        int rendered = 0;
        Integer minX = null;
        int minY = 0;
        int minZ = 0;
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;

        for (LitematicaSchematicInfo.Region region : info.regions()) {
            for (LitematicaSchematicInfo.BlockSample block : region.blocks()) {
                if (rendered++ >= MAX_RENDERED_BOXES) {
                    break;
                }

                BlockState state = SchematicPlacementState.transformState(block.state());

                if (state.isAir()) {
                    continue;
                }

                BlockPos pos = origin.offset(SchematicPlacementState.transformBlockOffset(region.position(), block.pos(), region.size()));

                if (minX == null) {
                    minX = pos.getX();
                    minY = pos.getY();
                    minZ = pos.getZ();
                    maxX = pos.getX() + 1;
                    maxY = pos.getY() + 1;
                    maxZ = pos.getZ() + 1;
                } else {
                    minX = Math.min(minX, pos.getX());
                    minY = Math.min(minY, pos.getY());
                    minZ = Math.min(minZ, pos.getZ());
                    maxX = Math.max(maxX, pos.getX() + 1);
                    maxY = Math.max(maxY, pos.getY() + 1);
                    maxZ = Math.max(maxZ, pos.getZ() + 1);
                }
            }

            if (rendered >= MAX_RENDERED_BOXES) {
                break;
            }
        }

        return minX == null ? null : new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void renderPlacementTarget(PoseStack poseStack) {
        if (!SchematicPlacementState.hasPlacement()) {
            return;
        }

        BlockPos target = SchematicPlacementState.getOrigin();

        if (target == null) {
            return;
        }

        this.renderFilledBox(poseStack, target.getX(), target.getY(), target.getZ(), target.getX() + 1, target.getY() + 1, target.getZ() + 1, TARGET_FILL_COLOR);
        this.renderLineBox(poseStack, target.getX(), target.getY(), target.getZ(), target.getX() + 1, target.getY() + 1, target.getZ() + 1, TARGET_LINE_COLOR);
    }

    private void renderFilledBox(PoseStack poseStack, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        this.addQuad(buffer, poseStack, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color);
        this.addQuad(buffer, poseStack, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        this.addQuad(buffer, poseStack, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
        this.addQuad(buffer, poseStack, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        this.addQuad(buffer, poseStack, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color);
        this.addQuad(buffer, poseStack, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderLineBox(PoseStack poseStack, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
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
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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

    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }
}

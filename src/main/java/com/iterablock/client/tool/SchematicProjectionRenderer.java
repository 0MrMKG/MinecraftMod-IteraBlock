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

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
    private static final double OVERLAY_EPSILON = 0.003D;
    private static final float PROJECTION_MODEL_SCALE = 1.003F;
    private static final int BOUNDING_BOX_COLOR = 0xD08AE8FF;
    private static final int TARGET_LINE_COLOR = 0xF000FF66;
    private static final int AREA_SELECTION_LINE_COLOR = 0xF4D6FFF0;
    private static final int AREA_FIRST_POINT_LINE_COLOR = 0xFFEAFBFF;
    private static final int AREA_FIRST_POINT_ACTIVE_LINE_COLOR = 0xFFBFE3F6;
    private static final int AREA_SECOND_POINT_LINE_COLOR = 0xFFFFF1D6;
    private static final int AREA_SECOND_POINT_ACTIVE_LINE_COLOR = 0xFFF6D6A8;
    private static final int BEZIER_CURVE_LINE_COLOR = 0xE0F3C6D3;
    private static final int BEZIER_POINT_LINE_COLOR = 0xF0F6D6A8;
    private static final int BEZIER_PREVIOUS_POINT_LINE_COLOR = 0xF0FF4A4A;
    private static final int SYMMETRY_AREA_LINE_COLOR = 0xB0D6FFF0;
    private static final int SYMMETRY_LOCKED_LINE_COLOR = 0x99EAFBFF;
    private static final int SYMMETRY_CENTER_LINE_COLOR = 0xF0D6FFF0;
    private LoadedLitematicManager.Entry cachedBoundsEntry;
    private int cachedBoundsRotation = -1;
    private SchematicPlacementState.MirrorAxis cachedBoundsMirror = SchematicPlacementState.MirrorAxis.NONE;
    private BlockPos cachedBoundsPlacementOffset = BlockPos.ZERO;
    private Bounds cachedLocalBounds;
    private LoadedLitematicManager.Entry cachedStepEntry;
    private int cachedStepRotation = -1;
    private SchematicPlacementState.MirrorAxis cachedStepMirror = SchematicPlacementState.MirrorAxis.NONE;
    private int cachedStepOverlapX = -1;
    private int cachedStepOverlapY = -1;
    private int cachedStepOverlapZ = -1;
    private BlockPos cachedArrayStep;
    private ProjectionKey cachedProjectionKey;
    private List<RenderBlock> cachedProjectionBlocks = java.util.List.of();
    private List<Bounds> cachedProjectionBounds = java.util.List.of();

    private SchematicProjectionRenderer() {
    }

    public static SchematicProjectionRenderer getInstance() {
        return INSTANCE;
    }

    public void clearCache() {
        this.cachedBoundsEntry = null;
        this.cachedBoundsRotation = -1;
        this.cachedBoundsMirror = SchematicPlacementState.MirrorAxis.NONE;
        this.cachedBoundsPlacementOffset = BlockPos.ZERO;
        this.cachedLocalBounds = null;
        this.cachedStepEntry = null;
        this.cachedStepRotation = -1;
        this.cachedStepMirror = SchematicPlacementState.MirrorAxis.NONE;
        this.cachedStepOverlapX = -1;
        this.cachedStepOverlapY = -1;
        this.cachedStepOverlapZ = -1;
        this.cachedArrayStep = null;
        this.cachedProjectionKey = null;
        this.cachedProjectionBlocks = java.util.List.of();
        this.cachedProjectionBounds = java.util.List.of();
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
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        boolean hasToolItem = ToolState.hasToolItem(minecraft.player);

        if (hasToolItem) {
            ToolState.updateSchematicPlacementPreview(minecraft);
        }

        this.renderAreaSelection(poseStack);
        this.renderSymmetryPlacement(poseStack);
        this.renderActivePlacement(minecraft, poseStack);
        this.renderPlacementTarget(poseStack);
        this.renderBezierCurve(minecraft, poseStack, camera);
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

        ProjectionSnapshot snapshot = this.getProjectionSnapshot(minecraft, origin, entry);

        if (!snapshot.blocks().isEmpty()) {
            this.renderProjectionBlocks(minecraft, poseStack, snapshot.blocks());
        }

        for (Bounds bounds : snapshot.bounds()) {
            this.renderLineBox(poseStack, bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ(), BOUNDING_BOX_COLOR);
        }
    }

    private ProjectionSnapshot getProjectionSnapshot(Minecraft minecraft, BlockPos origin, LoadedLitematicManager.Entry entry) {
        BlockPos arrayStep = this.getArrayStep(entry);
        List<BlockPos> offsets = this.getPlacementOffsets(arrayStep);
        int realRenderLimit = this.getRealRenderLimit(offsets.size());
        ProjectionKey key = new ProjectionKey(
                entry,
                origin,
                ToolState.getMode(),
                SchematicPlacementState.getArrayMode(),
                SchematicPlacementState.getRotationSteps(),
                SchematicPlacementState.getMirrorAxis(),
                SchematicPlacementState.getLinearArrayCount(),
                SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.X),
                SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.Y),
                SchematicPlacementState.getVolumeArrayCount(SchematicPlacementState.Axis.Z),
                SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.X),
                SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Y),
                SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Z),
                SchematicPlacementState.getPlacementOffset().getX(),
                SchematicPlacementState.getPlacementOffset().getY(),
                SchematicPlacementState.getPlacementOffset().getZ(),
                RingPlacementState.getCount(),
                RingPlacementState.getRadius(),
                BuilderHelperClientConfig.getLinearArrayRenderLimit(),
                BuilderHelperClientConfig.getVolumeArrayRenderLimit()
        );

        if (key.equals(this.cachedProjectionKey)) {
            return new ProjectionSnapshot(this.cachedProjectionBlocks, this.cachedProjectionBounds);
        }

        List<RenderBlock> blocks = new java.util.ArrayList<>();
        List<Bounds> bounds = new java.util.ArrayList<>();

        boolean ringPlacement = ToolState.getMode() == ToolMode.RING_PLACEMENT;

        for (int copy = 0; copy < realRenderLimit; copy++) {
            BlockPos copyOrigin = origin.offset(offsets.get(copy));
            int rotationSteps = ringPlacement ? RingPlacementState.getRotationSteps(copy) : SchematicPlacementState.getRotationSteps();
            this.collectPlacementBlocks(minecraft, copyOrigin, entry.info(), blocks, rotationSteps, SchematicPlacementState.getMirrorAxis(), !ringPlacement);
        }

        for (int copy = 0; copy < offsets.size(); copy++) {
            int rotationSteps = ringPlacement ? RingPlacementState.getRotationSteps(copy) : SchematicPlacementState.getRotationSteps();
            Bounds copyBounds = this.getPlacementBounds(origin.offset(offsets.get(copy)), entry, rotationSteps);

            if (copyBounds != null) {
                bounds.add(copyBounds);
            }
        }

        this.cachedProjectionKey = key;
        this.cachedProjectionBlocks = List.copyOf(blocks);
        this.cachedProjectionBounds = List.copyOf(bounds);
        return new ProjectionSnapshot(this.cachedProjectionBlocks, this.cachedProjectionBounds);
    }

    private List<BlockPos> getPlacementOffsets(BlockPos arrayStep) {
        if (ToolState.getMode() == ToolMode.ARRAY_PLACEMENT
                && SchematicPlacementState.getArrayMode() == SchematicPlacementState.ArrayMode.LINEAR) {
            List<BlockPos> offsets = new java.util.ArrayList<>();
            int copies = SchematicPlacementState.getLinearArrayCopyCount();

            for (int copy = 0; copy < copies; copy++) {
                offsets.add(SchematicPlacementState.getLinearArrayOffset(copy, arrayStep));
            }

            return offsets;
        }

        if (ToolState.getMode() == ToolMode.ARRAY_PLACEMENT
                && SchematicPlacementState.getArrayMode() == SchematicPlacementState.ArrayMode.VOLUME) {
            return SchematicPlacementState.getVolumeArrayOffsets(arrayStep);
        }

        if (ToolState.getMode() == ToolMode.RING_PLACEMENT) {
            return RingPlacementState.getOffsets();
        }

        return java.util.List.of(BlockPos.ZERO);
    }

    private BlockPos getArrayStep(LoadedLitematicManager.Entry entry) {
        int rotation = SchematicPlacementState.getRotationSteps();
        SchematicPlacementState.MirrorAxis mirror = SchematicPlacementState.getMirrorAxis();
        int overlapX = SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.X);
        int overlapY = SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Y);
        int overlapZ = SchematicPlacementState.getOverlap(SchematicPlacementState.Axis.Z);

        if (this.cachedStepEntry == entry
                && this.cachedStepRotation == rotation
                && this.cachedStepMirror == mirror
                && this.cachedStepOverlapX == overlapX
                && this.cachedStepOverlapY == overlapY
                && this.cachedStepOverlapZ == overlapZ
                && this.cachedArrayStep != null) {
            return this.cachedArrayStep;
        }

        this.cachedStepEntry = entry;
        this.cachedStepRotation = rotation;
        this.cachedStepMirror = mirror;
        this.cachedStepOverlapX = overlapX;
        this.cachedStepOverlapY = overlapY;
        this.cachedStepOverlapZ = overlapZ;
        this.cachedArrayStep = SchematicPlacementState.getLinearArrayStep(entry.info());
        return this.cachedArrayStep;
    }

    private int getRealRenderLimit(int copies) {
        if (ToolState.getMode() == ToolMode.RING_PLACEMENT) {
            return copies;
        }

        if (ToolState.getMode() == ToolMode.ARRAY_PLACEMENT
                && SchematicPlacementState.getArrayMode() == SchematicPlacementState.ArrayMode.LINEAR) {
            return Math.min(copies, BuilderHelperClientConfig.getLinearArrayRenderLimit());
        }

        if (ToolState.getMode() == ToolMode.ARRAY_PLACEMENT
                && SchematicPlacementState.getArrayMode() == SchematicPlacementState.ArrayMode.VOLUME) {
            int axisLimit = BuilderHelperClientConfig.getVolumeArrayRenderLimit();
            return Math.min(copies, axisLimit * axisLimit * axisLimit);
        }

        return copies;
    }

    private void collectPlacementBlocks(Minecraft minecraft, BlockPos origin, LitematicaSchematicInfo info, List<RenderBlock> renderBlocks) {
        this.collectPlacementBlocks(minecraft, origin, info, renderBlocks, SchematicPlacementState.getRotationSteps(), SchematicPlacementState.getMirrorAxis(), true);
    }

    private void collectPlacementBlocks(Minecraft minecraft, BlockPos origin, LitematicaSchematicInfo info, List<RenderBlock> renderBlocks, int rotationSteps, SchematicPlacementState.MirrorAxis mirrorAxis, boolean skipOccupiedBlocks) {
        for (LitematicaSchematicInfo.Region region : info.regions()) {
            List<LitematicaSchematicInfo.BlockSample> samples = region.blocks();

            for (LitematicaSchematicInfo.BlockSample block : samples) {
                BlockPos transformedOffset = SchematicPlacementState.transformBlockOffset(region.position(), block.pos(), region.size(), rotationSteps, mirrorAxis);
                BlockPos offset = SchematicPlacementState.applyPlacementOffset(transformedOffset, rotationSteps);
                BlockPos pos = origin.offset(offset);
                BlockState state = SchematicPlacementState.transformState(block.state(), rotationSteps, mirrorAxis);

                if (skipOccupiedBlocks && !minecraft.level.getBlockState(pos).isAir()) {
                    continue;
                }

                renderBlocks.add(new RenderBlock(pos, state));
            }
        }
    }

    private void renderProjectionBlocks(Minecraft minecraft, PoseStack poseStack, List<RenderBlock> blocks) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.8F);
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        for (RenderBlock block : blocks) {
            if (block.state().isAir()) {
                continue;
            }

            BlockPos pos = block.pos();
            poseStack.pushPose();
            poseStack.translate(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            poseStack.scale(PROJECTION_MODEL_SCALE, PROJECTION_MODEL_SCALE, PROJECTION_MODEL_SCALE);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            minecraft.getBlockRenderer().renderSingleBlock(block.state(), poseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.translucent());
            poseStack.popPose();
        }

        bufferSource.endBatch();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private Bounds getPlacementBounds(BlockPos origin, LoadedLitematicManager.Entry entry) {
        return this.getPlacementBounds(origin, entry, SchematicPlacementState.getRotationSteps());
    }

    private Bounds getPlacementBounds(BlockPos origin, LoadedLitematicManager.Entry entry, int rotationSteps) {
        Bounds localBounds = this.getLocalPlacementBounds(entry);

        if (rotationSteps != SchematicPlacementState.getRotationSteps()) {
            localBounds = this.findLocalPlacementBounds(entry.info(), rotationSteps, SchematicPlacementState.getMirrorAxis());
        }

        if (localBounds == null) {
            return null;
        }

        return localBounds.offset(origin);
    }

    private Bounds getLocalPlacementBounds(LoadedLitematicManager.Entry entry) {
        int rotation = SchematicPlacementState.getRotationSteps();
        SchematicPlacementState.MirrorAxis mirror = SchematicPlacementState.getMirrorAxis();
        BlockPos placementOffset = SchematicPlacementState.getPlacementOffset();

        if (this.cachedBoundsEntry == entry
                && this.cachedBoundsRotation == rotation
                && this.cachedBoundsMirror == mirror
                && this.cachedBoundsPlacementOffset.equals(placementOffset)) {
            return this.cachedLocalBounds;
        }

        this.cachedBoundsEntry = entry;
        this.cachedBoundsRotation = rotation;
        this.cachedBoundsMirror = mirror;
        this.cachedBoundsPlacementOffset = placementOffset;
        this.cachedLocalBounds = this.findLocalPlacementBounds(entry.info(), rotation, mirror);
        return this.cachedLocalBounds;
    }

    private Bounds findLocalPlacementBounds(LitematicaSchematicInfo info) {
        return this.findLocalPlacementBounds(info, SchematicPlacementState.getRotationSteps(), SchematicPlacementState.getMirrorAxis());
    }

    private Bounds findLocalPlacementBounds(LitematicaSchematicInfo info, int rotationSteps, SchematicPlacementState.MirrorAxis mirrorAxis) {
        Integer minX = null;
        int minY = 0;
        int minZ = 0;
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;

        for (LitematicaSchematicInfo.Region region : info.regions()) {
            for (LitematicaSchematicInfo.BlockSample block : region.blocks()) {
                BlockState state = SchematicPlacementState.transformState(block.state(), rotationSteps, mirrorAxis);

                if (state.isAir()) {
                    continue;
                }

                BlockPos offset = SchematicPlacementState.transformBlockOffset(region.position(), block.pos(), region.size(), rotationSteps, mirrorAxis);
                BlockPos pos = SchematicPlacementState.applyPlacementOffset(offset, rotationSteps);

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

        this.renderFilledBox(poseStack, target.getX(), target.getY(), target.getZ(), target.getX() + 1, target.getY() + 1, target.getZ() + 1, this.getConfiguredFillColor());
        this.renderLineBox(poseStack, target.getX(), target.getY(), target.getZ(), target.getX() + 1, target.getY() + 1, target.getZ() + 1, TARGET_LINE_COLOR);
    }

    private void renderAreaSelection(PoseStack poseStack) {
        if (ToolState.getMode() != ToolMode.AREA_COPY_PASTE && !ToolState.isRandomAreaMode()) {
            return;
        }

        BlockPos first = AreaSelectionState.getFirstCorner();
        BlockPos second = AreaSelectionState.getSecondCorner();

        if (first != null) {
            boolean active = AreaSelectionState.getActiveCorner() == AreaSelectionState.Corner.FIRST;
            int lineColor = active ? AREA_FIRST_POINT_ACTIVE_LINE_COLOR : AREA_FIRST_POINT_LINE_COLOR;
            this.renderAreaPointBox(poseStack, first, lineColor);
        }

        if (second != null) {
            boolean active = AreaSelectionState.getActiveCorner() == AreaSelectionState.Corner.SECOND;
            int lineColor = active ? AREA_SECOND_POINT_ACTIVE_LINE_COLOR : AREA_SECOND_POINT_LINE_COLOR;
            this.renderAreaPointBox(poseStack, second, lineColor);
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
        this.renderFilledBox(poseStack, minX, minY, minZ, maxX, maxY, maxZ, this.getConfiguredFillColor());
        this.renderLineBox(poseStack, minX, minY, minZ, maxX, maxY, maxZ, AREA_SELECTION_LINE_COLOR);
    }

    private void renderAreaPointBox(PoseStack poseStack, BlockPos point, int lineColor) {
        this.renderFilledBox(poseStack, point.getX(), point.getY(), point.getZ(), point.getX() + 1, point.getY() + 1, point.getZ() + 1, this.getConfiguredFillColor());
        this.renderLineBox(poseStack, point.getX(), point.getY(), point.getZ(), point.getX() + 1, point.getY() + 1, point.getZ() + 1, lineColor);
    }

    private int getConfiguredFillColor() {
        int alpha = Math.round(BuilderHelperClientConfig.getRenderFillOpacity() * 255.0F / 100.0F);
        return alpha << 24 | BuilderHelperClientConfig.getRenderFillRgb();
    }

    private void renderSymmetryPlacement(PoseStack poseStack) {
        if (ToolState.getMode() != ToolMode.SYMMETRY_PLACEMENT || !SymmetryPlacementState.hasCenter()) {
            return;
        }

        SymmetryPlacementState.Bounds bounds = SymmetryPlacementState.getBounds();

        if (bounds != null) {
            int fillColor = this.getConfiguredFillColor();
            if ((fillColor >>> 24) > 0) {
                this.renderFilledBox(poseStack, bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ(), fillColor);
            }
            this.renderLineBox(poseStack, bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ(),
                    SymmetryPlacementState.isLocked() ? SYMMETRY_LOCKED_LINE_COLOR : SYMMETRY_AREA_LINE_COLOR);
        }

        int centerFillColor = this.getConfiguredFillColor();
        SymmetryPlacementState.PlaneBounds planeBounds = SymmetryPlacementState.getPlaneMarkerBounds();

        if (planeBounds != null) {
            this.renderFilledBox(poseStack, planeBounds.minX(), planeBounds.minY(), planeBounds.minZ(), planeBounds.maxX(), planeBounds.maxY(), planeBounds.maxZ(), centerFillColor);
            this.renderLineBox(poseStack, planeBounds.minX(), planeBounds.minY(), planeBounds.minZ(), planeBounds.maxX(), planeBounds.maxY(), planeBounds.maxZ(), SYMMETRY_CENTER_LINE_COLOR);
        }

        for (SymmetryPlacementState.PlaneBounds markerBounds : SymmetryPlacementState.getEvenCenterMarkerBounds()) {
            this.renderFilledBox(poseStack, markerBounds.minX(), markerBounds.minY(), markerBounds.minZ(), markerBounds.maxX(), markerBounds.maxY(), markerBounds.maxZ(), centerFillColor);
            this.renderLineBox(poseStack, markerBounds.minX(), markerBounds.minY(), markerBounds.minZ(), markerBounds.maxX(), markerBounds.maxY(), markerBounds.maxZ(), SYMMETRY_CENTER_LINE_COLOR);
        }

        for (BlockPos marker : SymmetryPlacementState.getCenterMarkerBlocks()) {
            this.renderFilledBox(poseStack, marker.getX(), marker.getY(), marker.getZ(), marker.getX() + 1, marker.getY() + 1, marker.getZ() + 1, centerFillColor);
            this.renderLineBox(poseStack, marker.getX(), marker.getY(), marker.getZ(), marker.getX() + 1, marker.getY() + 1, marker.getZ() + 1, SYMMETRY_CENTER_LINE_COLOR);
        }
    }

    private void renderBezierCurve(Minecraft minecraft, PoseStack poseStack, Camera camera) {
        if (ToolState.getMode() != ToolMode.BEZIER_CURVE_GENERATION) {
            return;
        }

        List<BlockPos> previousPoints = BezierCurveState.getPreviousControlPoints();
        for (int i = 0; i < previousPoints.size(); i++) {
            BlockPos point = previousPoints.get(i);
            this.renderFilledBox(poseStack, point.getX(), point.getY(), point.getZ(), point.getX() + 1, point.getY() + 1, point.getZ() + 1, this.getConfiguredFillColor());
            this.renderLineBox(poseStack, point.getX(), point.getY(), point.getZ(), point.getX() + 1, point.getY() + 1, point.getZ() + 1, BEZIER_PREVIOUS_POINT_LINE_COLOR);
            this.renderPointLabel(minecraft, poseStack, camera, point, Integer.toString(i + 1), 0xFFFF6A6A);
        }

        List<BlockPos> currentPoints = BezierCurveState.getControlPoints();
        for (int i = 0; i < currentPoints.size(); i++) {
            BlockPos point = currentPoints.get(i);
            this.renderFilledBox(poseStack, point.getX(), point.getY(), point.getZ(), point.getX() + 1, point.getY() + 1, point.getZ() + 1, this.getConfiguredFillColor());
            this.renderLineBox(poseStack, point.getX(), point.getY(), point.getZ(), point.getX() + 1, point.getY() + 1, point.getZ() + 1, BEZIER_POINT_LINE_COLOR);
            this.renderPointLabel(minecraft, poseStack, camera, point, Integer.toString(i + 1), 0xFFFFF1B0);
        }

        for (BlockPos block : BezierCurveState.getCurveBlocks()) {
            this.renderFilledBox(poseStack, block.getX(), block.getY(), block.getZ(), block.getX() + 1, block.getY() + 1, block.getZ() + 1, this.getConfiguredFillColor());
            this.renderLineBox(poseStack, block.getX(), block.getY(), block.getZ(), block.getX() + 1, block.getY() + 1, block.getZ() + 1, BEZIER_CURVE_LINE_COLOR);
        }
    }

    private void renderPointLabel(Minecraft minecraft, PoseStack poseStack, Camera camera, BlockPos point, String label, int color) {
        Font font = minecraft.font;
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(point.getX() + 0.5, point.getY() + 1.28, point.getZ() + 0.5);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        float textX = -font.width(label) / 2.0F;
        font.drawInBatch(label, textX, 0.0F, color, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0x66000000, LightTexture.FULL_BRIGHT);
        bufferSource.endBatch();
        poseStack.popPose();
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

    private void addBoxQuads(BufferBuilder buffer, PoseStack poseStack, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
        this.addQuad(buffer, poseStack, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color);
        this.addQuad(buffer, poseStack, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        this.addQuad(buffer, poseStack, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
        this.addQuad(buffer, poseStack, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        this.addQuad(buffer, poseStack, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color);
        this.addQuad(buffer, poseStack, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
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

    private record RenderBlock(BlockPos pos, BlockState state) {
    }

    private record ProjectionKey(
            LoadedLitematicManager.Entry entry,
            BlockPos origin,
            ToolMode mode,
            SchematicPlacementState.ArrayMode arrayMode,
            int rotation,
            SchematicPlacementState.MirrorAxis mirror,
            int linearCount,
            int volumeX,
            int volumeY,
            int volumeZ,
            int overlapX,
            int overlapY,
            int overlapZ,
            int placementOffsetX,
            int placementOffsetY,
            int placementOffsetZ,
            int ringCount,
            int ringRadius,
            int linearRenderLimit,
            int volumeRenderLimit) {
    }

    private record ProjectionSnapshot(List<RenderBlock> blocks, List<Bounds> bounds) {
    }

    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private Bounds offset(BlockPos pos) {
            return new Bounds(
                    this.minX + pos.getX(),
                    this.minY + pos.getY(),
                    this.minZ + pos.getZ(),
                    this.maxX + pos.getX(),
                    this.maxY + pos.getY(),
                    this.maxZ + pos.getZ()
            );
        }
    }
}

package com.iterablock.client.tool;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import com.iterablock.client.Lang;
import com.iterablock.client.config.BuilderHelperClientConfig;
import com.iterablock.client.litematica.LitematicaSchematicInfo;
import com.iterablock.client.template.LoadedLitematicManager;
import com.iterablock.common.PlacementReplaceMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ToolState {
    private static final int PLACE_COMMAND_BATCH_SIZE = 256;
    private static final int FOLLOW_PREVIEW_RANGE = 64;
    private static final double RANDOM_AREA_REPLACE_CHANCE = 0.35D;
    private static final Path TEMPORARY_AREA_PATH = Path.of(".iterablock-memory", "temporary-area.litematic").toAbsolutePath().normalize();
    private static final Random RANDOM = new Random();
    private static ToolMode mode = ToolMode.AREA_COPY_PASTE;
    private static RandomPlacementMode randomPlacementMode = RandomPlacementMode.SCHEMATIC;
    private static boolean randomAreaLocked;
    private static String lastAction = "";
    private static boolean placeSchematicImmediately;

    private ToolState() {
    }

    public static ToolMode getMode() {
        return mode;
    }

    public static String getLastAction() {
        return lastAction.isEmpty() ? Lang.tr("iterablock.tool.action.ready") : lastAction;
    }

    public static String getLastActionCacheToken() {
        return lastAction;
    }

    public static void setMode(ToolMode newMode) {
        if (newMode == null || mode == newMode) {
            return;
        }

        mode = newMode;
        if (mode != ToolMode.SCHEMATIC_PLACEMENT) {
            SchematicPlacementState.stopPlacementPointAdjustment();
        }

        if (mode == ToolMode.ARRAY_PLACEMENT) {
            SchematicPlacementState.resetArrayMode();
        }
        if (mode == ToolMode.RANDOM_SCHEMATIC_PLACEMENT) {
            randomPlacementMode = RandomPlacementMode.SCHEMATIC;
            randomAreaLocked = false;
        }
        setLastAction(Lang.tr("iterablock.tool.action.mode_changed", mode.getDisplayName()));
    }

    public static RandomPlacementMode getRandomPlacementMode() {
        return randomPlacementMode;
    }

    public static boolean isRandomAreaMode() {
        return mode == ToolMode.RANDOM_SCHEMATIC_PLACEMENT && randomPlacementMode == RandomPlacementMode.AREA_BLOCK;
    }

    public static boolean isRandomAreaLocked() {
        return randomAreaLocked;
    }

    public static boolean toggleRandomPlacementMode() {
        if (mode != ToolMode.RANDOM_SCHEMATIC_PLACEMENT) {
            return false;
        }

        randomPlacementMode = randomPlacementMode == RandomPlacementMode.SCHEMATIC
                ? RandomPlacementMode.AREA_BLOCK
                : RandomPlacementMode.SCHEMATIC;
        randomAreaLocked = false;
        setLastAction(Lang.tr("iterablock.tool.action.random_mode",
                Lang.tr(randomPlacementMode == RandomPlacementMode.SCHEMATIC
                        ? "iterablock.tool.random.mode.schematic"
                        : "iterablock.tool.random.mode.area_block")));
        return true;
    }

    public static boolean toggleRandomAreaLock() {
        if (!isRandomAreaMode() || !AreaSelectionState.hasSelection()) {
            setLastAction(Lang.tr("iterablock.tool.action.random_area_need_selection"));
            return true;
        }

        randomAreaLocked = !randomAreaLocked;
        setLastAction(Lang.tr(randomAreaLocked
                ? "iterablock.tool.action.random_area_locked"
                : "iterablock.tool.action.random_area_unlocked"));
        return true;
    }

    public static void cycleMode(boolean forward) {
        setMode(mode.cycle(forward));
    }

    public static boolean toggleSchematicPlacementExecutionMode() {
        placeSchematicImmediately = !placeSchematicImmediately;
        setLastAction(Lang.tr(placeSchematicImmediately
                ? "iterablock.tool.action.schematic_place_mode_follow"
                : "iterablock.tool.action.schematic_place_mode_execute"));
        return true;
    }

    public static void updateSchematicPlacementPreview(Minecraft minecraft) {
        if (!placeSchematicImmediately
                || mode != ToolMode.SCHEMATIC_PLACEMENT
                || minecraft.screen != null
                || minecraft.player == null
                || ClientToolState.currentLitematic == null
                || !hasToolItem(minecraft.player)) {
            return;
        }

        BlockPos origin = getPlacementOrigin(minecraft, FOLLOW_PREVIEW_RANGE);
        if (SchematicPlacementState.getEntry() != ClientToolState.currentLitematic || !origin.equals(SchematicPlacementState.getOrigin())) {
            SchematicPlacementState.preview(ClientToolState.currentLitematic, origin);
        }
    }

    public static void adjustLinearArray(Minecraft minecraft, int amount) {
        if (mode != ToolMode.ARRAY_PLACEMENT
                || SchematicPlacementState.getArrayMode() != SchematicPlacementState.ArrayMode.LINEAR
                || minecraft.player == null
                || amount == 0
                || !SchematicPlacementState.hasPlacement()) {
            return;
        }

        SchematicPlacementState.adjustLinearArray(minecraft.player.getLookAngle(), amount);
        setLastAction(Lang.tr("iterablock.tool.action.linear_array_count", SchematicPlacementState.getLinearArrayAxisName(), SchematicPlacementState.getLinearArrayCount()));
    }

    public static void adjustVolumeArray(Minecraft minecraft, int amount) {
        if (mode != ToolMode.ARRAY_PLACEMENT
                || SchematicPlacementState.getArrayMode() != SchematicPlacementState.ArrayMode.VOLUME
                || minecraft.player == null
                || amount == 0
                || !SchematicPlacementState.hasPlacement()) {
            return;
        }

        SchematicPlacementState.adjustVolumeArray(minecraft.player.getLookAngle(), amount);
        setLastAction(Lang.tr("iterablock.tool.action.volume_array_count", SchematicPlacementState.getVolumeArraySummary()));
    }

    public static boolean toggleArrayMode() {
        if (mode != ToolMode.ARRAY_PLACEMENT) {
            return false;
        }

        SchematicPlacementState.ArrayMode arrayMode = SchematicPlacementState.toggleArrayMode();
        SchematicProjectionRenderer.getInstance().clearCache();
        setLastAction(Lang.tr("iterablock.tool.action.array_mode",
                Lang.tr(arrayMode == SchematicPlacementState.ArrayMode.LINEAR
                        ? "iterablock.tool.array.mode.linear"
                        : "iterablock.tool.array.mode.volume")));
        return true;
    }

    public static boolean adjustRingRadius(Minecraft minecraft, int amount) {
        if (mode != ToolMode.RING_PLACEMENT || minecraft.player == null || amount == 0) {
            return false;
        }

        RingPlacementState.adjustRadius(amount);
        SchematicProjectionRenderer.getInstance().clearCache();
        setLastAction("\u5706\u73af\u534a\u5f84\uff1a" + RingPlacementState.getRadius());
        return true;
    }

    public static boolean adjustRingCount(int amount) {
        if (mode != ToolMode.RING_PLACEMENT || amount == 0) {
            return false;
        }

        RingPlacementState.adjustCount(amount);
        SchematicProjectionRenderer.getInstance().clearCache();
        setLastAction("\u5706\u73af\u6570\u91cf\uff1a" + RingPlacementState.getCount());
        return true;
    }

    public static boolean adjustSchematicPlacement(Minecraft minecraft, int amount) {
        if (mode != ToolMode.SCHEMATIC_PLACEMENT || minecraft.player == null || amount == 0 || !SchematicPlacementState.hasPlacement() || !SchematicPlacementState.isPlacementPointAdjusting()) {
            return false;
        }

        SchematicPlacementState.Axis axis = SchematicPlacementState.adjustPlacementOffset(minecraft.player.getLookAngle(), amount);
        BlockPos origin = SchematicPlacementState.getPlacementOffset();

        if (axis == null || origin == null) {
            return false;
        }

        setLastAction("\u653e\u7f6e\u70b9\u5df2\u6cbf " + axis.name() + " \u8f74\u5fae\u8c03\uff1a" + origin.getX() + ", " + origin.getY() + ", " + origin.getZ());
        return true;
    }

    public static boolean toggleSchematicPlacementPointAdjustment() {
        if (mode != ToolMode.SCHEMATIC_PLACEMENT || !SchematicPlacementState.hasPlacement()) {
            return false;
        }

        if (!SchematicPlacementState.togglePlacementPointAdjustment()) {
            return false;
        }

        BlockPos offset = SchematicPlacementState.getPlacementOffset();
        setLastAction(SchematicPlacementState.isPlacementPointAdjusting()
                ? "\u653e\u7f6e\u70b9\u5fae\u8c03\uff1a\u5f00\uff08" + offset.getX() + ", " + offset.getY() + ", " + offset.getZ() + "\uff09"
                : "\u653e\u7f6e\u70b9\u5fae\u8c03\uff1a\u5173");
        return true;
    }

    public static boolean adjustAreaSelection(Minecraft minecraft, int amount) {
        boolean areaMode = mode == ToolMode.AREA_COPY_PASTE || isRandomAreaMode();
        if (!areaMode
                || randomAreaLocked
                || minecraft.player == null
                || amount == 0
                || !AreaSelectionState.hasFirstCorner()) {
            return false;
        }

        SchematicPlacementState.Axis axis = SchematicPlacementState.getLookAxis(minecraft.player.getLookAngle());
        int signedAmount = getSignedAxisAmount(minecraft, axis, amount);
        if (!AreaSelectionState.adjustActiveCorner(axis, signedAmount)) {
            return false;
        }

        setLastAction(Lang.tr("iterablock.tool.action.area_adjusted", AreaSelectionState.getActiveCornerNumber(), axis.name(), AreaSelectionState.getSizeX(), AreaSelectionState.getSizeY(), AreaSelectionState.getSizeZ()));
        return true;
    }

    public static boolean toggleAreaSelectionReference() {
        if (mode != ToolMode.AREA_COPY_PASTE || !AreaSelectionState.toggleActiveCorner()) {
            return false;
        }

        setLastAction(Lang.tr("iterablock.tool.action.area_reference", AreaSelectionState.getActiveCornerNumber()));
        return true;
    }

    public static boolean toggleSymmetryLock() {
        if (mode != ToolMode.SYMMETRY_PLACEMENT || !SymmetryPlacementState.toggleLockOrClear()) {
            return false;
        }

        if (SymmetryPlacementState.hasCenter()) {
            setLastAction(Lang.tr("iterablock.tool.action.symmetry_locked"));
        } else {
            setLastAction(Lang.tr("iterablock.tool.action.symmetry_cleared"));
        }

        return true;
    }

    public static boolean adjustSymmetryArea(Minecraft minecraft, int amount) {
        if (mode != ToolMode.SYMMETRY_PLACEMENT || minecraft.player == null || amount == 0 || !SymmetryPlacementState.hasCenter()) {
            return false;
        }

        if (!SymmetryPlacementState.adjust(minecraft.player.getLookAngle(), amount)) {
            return false;
        }

        setLastAction(Lang.tr("iterablock.tool.action.symmetry_adjusted", SymmetryPlacementState.getRadius(), SymmetryPlacementState.getHeight()));
        return true;
    }

    public static boolean toggleSymmetryKind() {
        if (mode != ToolMode.SYMMETRY_PLACEMENT) {
            return false;
        }

        SymmetryPlacementState.Kind symmetryKind = SymmetryPlacementState.toggleKind();
        setLastAction(Lang.tr("iterablock.tool.action.symmetry_kind", Lang.tr(symmetryKind.translationKey())));
        return true;
    }

    public static boolean toggleSymmetryParity() {
        if (mode != ToolMode.SYMMETRY_PLACEMENT) {
            return false;
        }

        SymmetryPlacementState.Parity symmetryParity = SymmetryPlacementState.toggleParity();
        setLastAction(Lang.tr("iterablock.tool.action.symmetry_parity", Lang.tr(symmetryParity.translationKey())));
        return true;
    }

    public static boolean toggleSymmetryEnabled() {
        if (mode != ToolMode.SYMMETRY_PLACEMENT) {
            return false;
        }

        if (!SymmetryPlacementState.toggleEnabled()) {
            setLastAction(Lang.tr("iterablock.tool.action.symmetry_need_locked"));
            return true;
        }

        setLastAction(Lang.tr(SymmetryPlacementState.isEnabled()
                ? "iterablock.tool.action.symmetry_resumed"
                : "iterablock.tool.action.symmetry_paused"));
        return true;
    }

    public static boolean copyAreaSelectionToLoaded(Minecraft minecraft) {
        if (mode != ToolMode.AREA_COPY_PASTE || minecraft.level == null) {
            return false;
        }

        if (!AreaSelectionState.hasSelection()) {
            setLastAction(Lang.tr("iterablock.tool.action.area_copy_need_selection"));
            return true;
        }

        BlockPos min = AreaSelectionState.getMinCorner();
        BlockPos max = AreaSelectionState.getMaxCorner();
        BlockPos size = new BlockPos(AreaSelectionState.getSizeX(), AreaSelectionState.getSizeY(), AreaSelectionState.getSizeZ());
        List<LitematicaSchematicInfo.BlockSample> blocks = new ArrayList<>();

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockState state = minecraft.level.getBlockState(worldPos);

                    if (state.isAir()) {
                        continue;
                    }

                    blocks.add(new LitematicaSchematicInfo.BlockSample(new BlockPos(x - min.getX(), y - min.getY(), z - min.getZ()), state));
                }
            }
        }

        if (blocks.isEmpty()) {
            setLastAction(Lang.tr("iterablock.tool.action.area_copy_empty"));
            return true;
        }

        long now = System.currentTimeMillis();
        String name = Lang.tr("iterablock.tool.area.temp_name");
        removeExistingTemporaryArea();
        LitematicaSchematicInfo.Metadata metadata = new LitematicaSchematicInfo.Metadata(
                name,
                minecraft.player == null ? "IteraBlock" : minecraft.player.getName().getString(),
                Lang.tr("iterablock.tool.area.temp_description"),
                size,
                now,
                now,
                1,
                size.getX() * size.getY() * size.getZ(),
                blocks.size()
        );
        LitematicaSchematicInfo.Region region = new LitematicaSchematicInfo.Region(
                "Region 1",
                BlockPos.ZERO,
                size,
                size,
                0,
                0,
                0,
                0,
                0,
                0,
                size.getX() * size.getY() * size.getZ(),
                blocks.size(),
                0,
                List.of(),
                blocks
        );
        LitematicaSchematicInfo info = new LitematicaSchematicInfo(TEMPORARY_AREA_PATH, 0, 0, 0, metadata, List.of(region));

        if (LoadedLitematicManager.load(TEMPORARY_AREA_PATH, info) == null) {
            setLastAction(Lang.tr("iterablock.tool.action.area_copy_limit", LoadedLitematicManager.MAX_LOADED));
            return true;
        }

        setLastAction(Lang.tr("iterablock.tool.action.area_copy_loaded", name, blocks.size()));
        return true;
    }

    private static void removeExistingTemporaryArea() {
        LoadedLitematicManager.Entry existing = null;

        for (LoadedLitematicManager.Entry entry : LoadedLitematicManager.getEntries()) {
            if (entry.path().equals(TEMPORARY_AREA_PATH)) {
                existing = entry;
                break;
            }
        }

        if (existing != null) {
            LoadedLitematicManager.unload(existing);
        }
    }

    public static boolean hasToolItem(LocalPlayer player) {
        return player != null && (isToolItem(player.getMainHandItem()) || isToolItem(player.getOffhandItem()));
    }

    public static boolean isToolItem(ItemStack stack) {
        return stack.is(Items.BLAZE_ROD);
    }

    public static boolean handlePrimaryAction(Minecraft minecraft) {
        if (!hasToolItem(minecraft.player)) {
            return false;
        }

        if (mode == ToolMode.AREA_COPY_PASTE || (isRandomAreaMode() && !randomAreaLocked)) {
            handleAreaSelectionPrimary(minecraft);
            return true;
        }

        setLastAction(withCurrentLitematic(Lang.tr("iterablock.tool.action.mode_primary", mode.getDisplayName())));
        return true;
    }

    public static boolean handleSecondaryAction(Minecraft minecraft) {
        if (!hasToolItem(minecraft.player)) {
            return false;
        }

        switch (mode) {
            case AREA_COPY_PASTE -> handleAreaSelectionSecondary(minecraft);
            case SCHEMATIC_PLACEMENT, ARRAY_PLACEMENT -> handleSchematicPlacementSecondary(minecraft);
            case RING_PLACEMENT -> handleRingPlacementSecondary(minecraft);
            case BEZIER_CURVE_GENERATION -> handleBezierSecondary(minecraft);
            case SYMMETRY_PLACEMENT -> handleSymmetrySecondary(minecraft);
            case RANDOM_SCHEMATIC_PLACEMENT -> {
                if (isRandomAreaMode() && !randomAreaLocked) {
                    handleAreaSelectionSecondary(minecraft);
                } else {
                    setLastAction(withCurrentLitematic(Lang.tr("iterablock.tool.action.mode_secondary", mode.getDisplayName())));
                }
            }
            default -> setLastAction(withCurrentLitematic(Lang.tr("iterablock.tool.action.mode_secondary", mode.getDisplayName())));
        }

        return true;
    }

    public static boolean placeCurrentProjection(Minecraft minecraft) {
        if (mode == ToolMode.BEZIER_CURVE_GENERATION) {
            return placeBezierCurve(minecraft);
        }

        if (mode == ToolMode.RANDOM_SCHEMATIC_PLACEMENT) {
            if (randomPlacementMode == RandomPlacementMode.AREA_BLOCK) {
                return placeRandomAreaBlocks(minecraft);
            }
            return placeRandomSchematic(minecraft);
        }

        if (!hasToolItem(minecraft.player)) {
            return false;
        }

        if (!SchematicPlacementState.hasPlacement() || minecraft.level == null || minecraft.getConnection() == null) {
            setLastAction(Lang.tr("iterablock.tool.action.no_projection"));
            return true;
        }

        LoadedLitematicManager.Entry entry = SchematicPlacementState.getEntry();
        BlockPos origin = SchematicPlacementState.getOrigin();

        if (entry == null || origin == null) {
            setLastAction(Lang.tr("iterablock.tool.action.no_projection"));
            return true;
        }

        PlacementBatcher batcher = new PlacementBatcher(BuilderHelperClientConfig.getPlacementReplaceMode());
        collectBlocks(origin, entry.info(), batcher);
        batcher.flush();
        SchematicProjectionRenderer.getInstance().clearCache();
        setLastAction(Lang.tr("iterablock.tool.action.placed_projection", batcher.totalSent()));
        return true;
    }

    private static boolean placeRandomSchematic(Minecraft minecraft) {
        if (!hasToolItem(minecraft.player)) {
            return false;
        }

        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) {
            return false;
        }

        LoadedLitematicManager.Entry entry = ClientToolState.currentLitematic;

        if (entry == null) {
            setLastAction(Lang.tr("iterablock.tool.action.no_litematic"));
            return true;
        }

        PlacementBatcher batcher = new PlacementBatcher(BuilderHelperClientConfig.getPlacementReplaceMode());
        collectRandomBlocks(minecraft.player.blockPosition(), entry.info(), batcher);

        if (batcher.isEmpty()) {
            setLastAction(Lang.tr("iterablock.tool.action.random_empty"));
            return true;
        }

        batcher.flush();
        setLastAction(Lang.tr("iterablock.tool.action.random_placed", batcher.totalSent(), BuilderHelperClientConfig.getRandomPlacementCount()));
        return true;
    }

    public static boolean placeRandomAreaBlocks(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) {
            return false;
        }

        if (!isRandomAreaMode() || !randomAreaLocked || !AreaSelectionState.hasSelection()) {
            setLastAction(Lang.tr("iterablock.tool.action.random_area_need_lock"));
            return true;
        }

        BlockState state = getHeldBlockState(minecraft.player);
        if (state == null) {
            state = Blocks.AIR.defaultBlockState();
        }

        BlockPos min = AreaSelectionState.getMinCorner();
        BlockPos max = AreaSelectionState.getMaxCorner();
        PlacementBatcher batcher = new PlacementBatcher(PlacementReplaceMode.REPLACE_ALL);
        long seed = RANDOM.nextLong();

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (matchesRandomAreaNoise(pos, seed)) {
                        batcher.accept(new PlacedBlock(pos, state));
                    }
                }
            }
        }

        batcher.flush();
        setLastAction(Lang.tr("iterablock.tool.action.random_area_placed", batcher.totalSent()));
        return true;
    }

    private static boolean matchesRandomAreaNoise(BlockPos pos, long seed) {
        long value = pos.asLong() ^ seed ^ 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        double normalized = (value >>> 11) * 0x1.0p-53;
        return normalized < RANDOM_AREA_REPLACE_CHANCE;
    }

    public static boolean placeBezierCurve(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) {
            return false;
        }

        if (!BezierCurveState.isReady()) {
            setLastAction(Lang.tr("iterablock.tool.action.bezier_need_points", BezierCurveState.getRequiredPointCount(), BezierCurveState.getPointCount()));
            return true;
        }

        if (BuilderHelperClientConfig.isBezierPlaceNbtMode()) {
            return placeBezierSchematic(minecraft);
        }

        BlockState state = getHeldBlockState(minecraft.player);

        if (state == null || state.isAir()) {
            setLastAction(Lang.tr("iterablock.tool.action.bezier_no_block"));
            return true;
        }

        List<PlacedBlock> blocks = new java.util.ArrayList<>();
        int precision = BuilderHelperClientConfig.getBezierPlacementPrecision();
        int width = BuilderHelperClientConfig.getBezierPlacementWidth();

        for (BlockPos pos : BezierCurveState.getCurveBlocks(precision, width)) {
            blocks.add(new PlacedBlock(pos, state));
        }

        if (blocks.isEmpty()) {
            setLastAction(Lang.tr("iterablock.tool.action.bezier_empty"));
            return true;
        }

        PlacementReplaceMode replaceMode = BuilderHelperClientConfig.getPlacementReplaceMode();
        int sent = sendPlacedBlocks(blocks, replaceMode);
        setLastAction(Lang.tr("iterablock.tool.action.bezier_placed", sent));
        return true;
    }

    private static boolean placeBezierSchematic(Minecraft minecraft) {
        LoadedLitematicManager.Entry entry = ClientToolState.currentLitematic;

        if (entry == null) {
            setLastAction(Lang.tr("iterablock.tool.action.no_litematic"));
            return true;
        }

        int precision = BuilderHelperClientConfig.getBezierPlacementPrecision();
        int width = BuilderHelperClientConfig.getBezierPlacementWidth();
        PlacementBatcher batcher = new PlacementBatcher(BuilderHelperClientConfig.getPlacementReplaceMode());

        for (BezierCurveState.CurvePlacement placement : BezierCurveState.getCurvePlacements(precision, width)) {
            collectBlocksForOrigin(placement.pos(), entry.info(), batcher, placement.rotationSteps());
        }

        if (batcher.isEmpty()) {
            setLastAction(Lang.tr("iterablock.tool.action.bezier_empty"));
            return true;
        }

        batcher.flush();
        setLastAction(Lang.tr("iterablock.tool.action.bezier_placed", batcher.totalSent()));
        return true;
    }

    public static boolean rotateCurrentProjection() {
        if (!SchematicPlacementState.hasPlacement()) {
            setLastAction(Lang.tr("iterablock.tool.action.no_projection"));
            return true;
        }

        SchematicPlacementState.rotateClockwise();
        setLastAction(Lang.tr("iterablock.tool.action.rotated_projection", SchematicPlacementState.getRotationSteps() * 90));
        return true;
    }

    public static boolean mirrorCurrentProjection(Minecraft minecraft) {
        if (!SchematicPlacementState.hasPlacement()) {
            setLastAction(Lang.tr("iterablock.tool.action.no_projection"));
            return true;
        }

        if (minecraft.player == null) {
            return false;
        }

        SchematicPlacementState.MirrorAxis axis = SchematicPlacementState.mirrorByLook(minecraft.player.getLookAngle());
        setLastAction(Lang.tr("iterablock.tool.action.mirrored_projection", getMirrorAxisName(axis)));
        return true;
    }

    private static void handleSchematicPlacementSecondary(Minecraft minecraft) {
        if (ClientToolState.currentLitematic == null) {
            setLastAction(Lang.tr("iterablock.tool.action.no_litematic"));
            return;
        }

        if (mode == ToolMode.SCHEMATIC_PLACEMENT && placeSchematicImmediately) {
            BlockPos origin = getPlacementOrigin(minecraft, FOLLOW_PREVIEW_RANGE);
            if (SchematicPlacementState.getEntry() != ClientToolState.currentLitematic || !origin.equals(SchematicPlacementState.getOrigin())) {
                SchematicPlacementState.preview(ClientToolState.currentLitematic, origin);
            }
            placeCurrentProjection(minecraft);
            return;
        }

        BlockPos origin = getPlacementOrigin(minecraft);
        SchematicPlacementState.place(ClientToolState.currentLitematic, origin);
        setLastAction(withCurrentLitematic(Lang.tr("iterablock.tool.action.placement_projected")));
    }

    private static void handleRingPlacementSecondary(Minecraft minecraft) {
        if (ClientToolState.currentLitematic == null) {
            setLastAction(Lang.tr("iterablock.tool.action.no_litematic"));
            return;
        }

        BlockPos center = getPlacementOrigin(minecraft);
        SchematicPlacementState.place(ClientToolState.currentLitematic, center);
        setLastAction(withCurrentLitematic("\u5706\u73af\u5706\u5fc3\u5df2\u8bbe\u7f6e\uff1a" + center.getX() + ", " + center.getY() + ", " + center.getZ()));
    }

    private static void handleAreaSelectionPrimary(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        BlockPos pos = getTargetBlockPos(minecraft);
        AreaSelectionState.setFirstCorner(pos);
        setLastAction(Lang.tr("iterablock.tool.action.area_first", pos.getX(), pos.getY(), pos.getZ()));
    }

    private static void handleAreaSelectionSecondary(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        BlockPos pos = getTargetBlockPos(minecraft);
        AreaSelectionState.setSecondCorner(pos);
        setLastAction(Lang.tr("iterablock.tool.action.area_second", pos.getX(), pos.getY(), pos.getZ(), AreaSelectionState.getSizeX(), AreaSelectionState.getSizeY(), AreaSelectionState.getSizeZ()));
    }

    private static void handleBezierSecondary(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        BlockPos point = getPlacementOrigin(minecraft);
        if (!BezierCurveState.addControlPoint(point)) {
            setLastAction(Lang.tr("iterablock.tool.action.bezier_archived", BezierCurveState.getRequiredPointCount()));
            return;
        }

        setLastAction(Lang.tr("iterablock.tool.action.bezier_point", BezierCurveState.getPointCount(), point.getX(), point.getY(), point.getZ()));
    }

    private static void handleSymmetrySecondary(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        BlockPos point = getTargetBlockPos(minecraft);
        SymmetryPlacementState.setCenter(point, minecraft.player.getLookAngle());
        setLastAction(Lang.tr("iterablock.tool.action.symmetry_center", point.getX(), point.getY(), point.getZ()));
    }

    private static BlockPos getPlacementOrigin(Minecraft minecraft) {
        return getPlacementOrigin(minecraft, BuilderHelperClientConfig.getPlacementRange());
    }

    private static BlockPos getPlacementOrigin(Minecraft minecraft, int range) {
        HitResult result = minecraft.player.pick(range, 0.0F, false);

        if (result instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos().relative(hitResult.getDirection());
        }

        return BlockPos.containing(result.getLocation());
    }

    private static BlockPos getTargetBlockPos(Minecraft minecraft) {
        int range = BuilderHelperClientConfig.getPlacementRange();
        HitResult result = minecraft.player.pick(range, 0.0F, false);

        if (result instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }

        return BlockPos.containing(result.getLocation());
    }

    private static int getSignedAxisAmount(Minecraft minecraft, SchematicPlacementState.Axis axis, int amount) {
        double lookValue = switch (axis) {
            case X -> minecraft.player.getLookAngle().x;
            case Y -> minecraft.player.getLookAngle().y;
            case Z -> minecraft.player.getLookAngle().z;
        };

        return (lookValue < 0.0 ? -1 : 1) * amount;
    }

    private static List<PlacedBlock> collectBlocks(BlockPos origin, LitematicaSchematicInfo info) {
        List<PlacedBlock> placed = new java.util.ArrayList<>();
        collectBlocks(origin, info, placed::add);
        return placed;
    }

    private static void collectBlocks(BlockPos origin, LitematicaSchematicInfo info, Consumer<PlacedBlock> collector) {
        if (mode == ToolMode.RING_PLACEMENT) {
            List<BlockPos> offsets = RingPlacementState.getOffsets();

            for (int i = 0; i < offsets.size(); i++) {
                collectBlocksForOrigin(origin.offset(offsets.get(i)), info, collector, RingPlacementState.getRotationSteps(i), SchematicPlacementState.getMirrorAxis());
            }

            return;
        }

        BlockPos arrayStep = SchematicPlacementState.getLinearArrayStep(info);
        List<BlockPos> offsets = getPlacementOffsets(arrayStep);

        for (BlockPos offset : offsets) {
            BlockPos copyOrigin = origin.offset(offset);
            collectBlocksForOrigin(copyOrigin, info, collector, SchematicPlacementState.getRotationSteps(), SchematicPlacementState.getMirrorAxis());
        }
    }

    private static void collectRandomBlocks(BlockPos playerPos, LitematicaSchematicInfo info, Consumer<PlacedBlock> collector) {
        int radius = BuilderHelperClientConfig.getRandomPlacementRadius();
        int minHeight = BuilderHelperClientConfig.getRandomPlacementHeightMin();
        int maxHeight = BuilderHelperClientConfig.getRandomPlacementHeightMax();
        int count = BuilderHelperClientConfig.getRandomPlacementCount();
        int rotationChance = BuilderHelperClientConfig.getRandomPlacementRotationChance();

        if (minHeight > maxHeight) {
            int tmp = minHeight;
            minHeight = maxHeight;
            maxHeight = tmp;
        }

        for (int i = 0; i < count; i++) {
            int xOffset = RANDOM.nextInt(Math.max(1, radius * 2)) - radius;
            int zOffset = RANDOM.nextInt(Math.max(1, radius * 2)) - radius;
            int yOffset = minHeight + RANDOM.nextInt(Math.max(1, maxHeight - minHeight + 1));
            int rotationSteps = RANDOM.nextInt(100) < rotationChance ? 1 + RANDOM.nextInt(3) : 0;
            BlockPos origin = playerPos.offset(xOffset, yOffset, zOffset);
            collectBlocksForOrigin(origin, info, collector, rotationSteps, SchematicPlacementState.getMirrorAxis());
        }
    }

    private static BlockState getHeldBlockState(LocalPlayer player) {
        BlockState mainHand = getBlockState(player.getMainHandItem());

        if (mainHand != null) {
            return mainHand;
        }

        return getBlockState(player.getOffhandItem());
    }

    private static BlockState getBlockState(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState();
        }

        return null;
    }

    private static List<BlockPos> getPlacementOffsets(BlockPos arrayStep) {
        if (mode == ToolMode.ARRAY_PLACEMENT
                && SchematicPlacementState.getArrayMode() == SchematicPlacementState.ArrayMode.LINEAR) {
            List<BlockPos> offsets = new java.util.ArrayList<>();
            int copies = SchematicPlacementState.getLinearArrayCopyCount();

            for (int copy = 0; copy < copies; copy++) {
                offsets.add(SchematicPlacementState.getLinearArrayOffset(copy, arrayStep));
            }

            return offsets;
        }

        if (mode == ToolMode.ARRAY_PLACEMENT
                && SchematicPlacementState.getArrayMode() == SchematicPlacementState.ArrayMode.VOLUME) {
            return SchematicPlacementState.getVolumeArrayOffsets(arrayStep);
        }

        return java.util.List.of(BlockPos.ZERO);
    }

    private static void collectBlocksForOrigin(BlockPos origin, LitematicaSchematicInfo info, List<PlacedBlock> placed) {
        collectBlocksForOrigin(origin, info, placed::add, SchematicPlacementState.getRotationSteps(), SchematicPlacementState.getMirrorAxis());
    }

    private static void collectBlocksForOrigin(BlockPos origin, LitematicaSchematicInfo info, List<PlacedBlock> placed, int rotationSteps) {
        collectBlocksForOrigin(origin, info, placed::add, rotationSteps, SchematicPlacementState.getMirrorAxis());
    }

    private static void collectBlocksForOrigin(BlockPos origin, LitematicaSchematicInfo info, Consumer<PlacedBlock> collector, int rotationSteps) {
        collectBlocksForOrigin(origin, info, collector, rotationSteps, SchematicPlacementState.getMirrorAxis());
    }

    private static void collectBlocksForOrigin(BlockPos origin, LitematicaSchematicInfo info, Consumer<PlacedBlock> collector, int rotationSteps, SchematicPlacementState.MirrorAxis mirrorAxis) {
        for (LitematicaSchematicInfo.Region region : info.regions()) {
            List<LitematicaSchematicInfo.BlockSample> blocks = region.blocks();

            for (LitematicaSchematicInfo.BlockSample block : blocks) {
                BlockState state = transformState(block.state(), rotationSteps, mirrorAxis);

                if (state.isAir()) {
                    continue;
                }

                BlockPos transformedOffset = transformBlockOffset(region.position(), block.pos(), region.size(), rotationSteps, mirrorAxis);
                BlockPos offset = SchematicPlacementState.applyPlacementOffset(transformedOffset, rotationSteps);
                BlockPos pos = origin.offset(offset);
                collector.accept(new PlacedBlock(pos, state));
            }
        }
    }

    private static int sendPlacedBlocks(List<PlacedBlock> blocks, PlacementReplaceMode replaceMode) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().getConnection() == null) {
            return 0;
        }

        List<PlacedBlock> finalBlocks = preparePlacedBlocks(blocks);
        List<String> commands = new ArrayList<>(finalBlocks.size());

        for (PlacedBlock block : finalBlocks) {
            commands.add(toSetBlockCommand(block, replaceMode));
        }

        PlacementCommandQueue.getInstance().enqueue(commands);
        return finalBlocks.size();
    }

    private static List<PlacedBlock> preparePlacedBlocks(List<PlacedBlock> blocks) {
        Map<BlockPos, BlockState> expanded = new LinkedHashMap<>();

        for (PlacedBlock block : blocks) {
            putPreparedBlock(expanded, block.pos(), block.state());

            if (SymmetryPlacementState.isActive()) {
                for (SymmetryPlacementState.MirrorPlacement placement : SymmetryPlacementState.getMirrorPlacements(block.pos())) {
                    putPreparedBlock(expanded, placement.pos(), mirrorState(block.state(), placement));
                }
            }
        }

        List<PlacedBlock> result = new ArrayList<>(expanded.size());
        expanded.forEach((pos, state) -> result.add(new PlacedBlock(pos, state)));
        return result;
    }

    private static void putPreparedBlock(Map<BlockPos, BlockState> blocks, BlockPos pos, BlockState state) {
        BlockState existing = blocks.get(pos);

        if (existing != null && isHalfSlab(existing) && isSlab(state)) {
            return;
        }

        blocks.put(pos, state);
    }

    private static boolean isSlab(BlockState state) {
        return state.getBlock() instanceof SlabBlock && state.hasProperty(SlabBlock.TYPE);
    }

    private static boolean isHalfSlab(BlockState state) {
        return isSlab(state) && state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE;
    }

    private static final class PlacementBatcher implements Consumer<PlacedBlock> {
        private final PlacementReplaceMode replaceMode;
        private final List<PlacedBlock> buffer = new ArrayList<>(PLACE_COMMAND_BATCH_SIZE);
        private int totalSent;

        private PlacementBatcher(PlacementReplaceMode replaceMode) {
            this.replaceMode = replaceMode;
        }

        @Override
        public void accept(PlacedBlock block) {
            this.buffer.add(block);

            if (this.buffer.size() >= PLACE_COMMAND_BATCH_SIZE) {
                this.flush();
            }
        }

        private void flush() {
            if (this.buffer.isEmpty()) {
                return;
            }

            this.totalSent += sendPlacedBlocks(this.buffer, this.replaceMode);
            this.buffer.clear();
        }

        private boolean isEmpty() {
            return this.totalSent == 0 && this.buffer.isEmpty();
        }

        private int totalSent() {
            return this.totalSent;
        }
    }

    private static String toSetBlockCommand(PlacedBlock block, PlacementReplaceMode replaceMode) {
        BlockPos pos = block.pos();
        String setBlockCommand = "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + BlockStateParser.serialize(block.state()) + " replace";

        return switch (replaceMode) {
            case REPLACE_ALL -> setBlockCommand;
            case ONLY_REPLACE_AIR -> "execute if block " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " air run " + setBlockCommand;
        };
    }

    private record PlacedBlock(BlockPos pos, BlockState state) {
    }

    public enum RandomPlacementMode {
        SCHEMATIC,
        AREA_BLOCK
    }

    private static BlockPos transformBlockOffset(BlockPos regionPosition, BlockPos localPos, BlockPos regionSize, int rotationSteps) {
        return transformBlockOffset(regionPosition, localPos, regionSize, rotationSteps, SchematicPlacementState.MirrorAxis.NONE);
    }

    private static BlockPos transformBlockOffset(BlockPos regionPosition, BlockPos localPos, BlockPos regionSize, int rotationSteps, SchematicPlacementState.MirrorAxis mirrorAxis) {
        return SchematicPlacementState.transformBlockOffset(regionPosition, localPos, regionSize, rotationSteps, mirrorAxis);
    }

    private static BlockState transformState(BlockState state, int rotationSteps) {
        return transformState(state, rotationSteps, SchematicPlacementState.MirrorAxis.NONE);
    }

    private static BlockState transformState(BlockState state, int rotationSteps, SchematicPlacementState.MirrorAxis mirrorAxis) {
        return SchematicPlacementState.transformState(state, rotationSteps, mirrorAxis);
    }

    private static BlockState mirrorState(BlockState state, SymmetryPlacementState.MirrorPlacement placement) {
        BlockState mirrored = state;

        if (placement.mirrorX()) {
            mirrored = SchematicPlacementState.transformState(mirrored, 0, SchematicPlacementState.MirrorAxis.X);
        }

        if (placement.mirrorZ()) {
            mirrored = SchematicPlacementState.transformState(mirrored, 0, SchematicPlacementState.MirrorAxis.Z);
        }

        return mirrored;
    }

    private static String getMirrorAxisName(SchematicPlacementState.MirrorAxis axis) {
        return switch (axis) {
            case X -> "X";
            case Y -> "Y";
            case Z -> "Z";
            case NONE -> Lang.tr("iterablock.tool.mirror.none");
        };
    }

    private static String withCurrentLitematic(String action) {
        if (ClientToolState.currentLitematic == null) {
            return action + " - " + Lang.tr("iterablock.tool.litematic_none");
        }

        return action + " - " + Lang.tr("iterablock.tool.litematic", ClientToolState.currentLitematic.displayName());
    }

    private static void setLastAction(String text) {
        lastAction = text;
        showActionBar(text);
    }

    private static void showActionBar(String text) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player != null) {
            player.displayClientMessage(Component.literal(Lang.tr("iterablock.tool.actionbar", text)), true);
        }
    }
}

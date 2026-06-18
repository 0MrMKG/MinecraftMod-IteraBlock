package com.iterablock.client.tool;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ToolState {
    private static final int PLACE_COMMAND_BATCH_SIZE = 256;
    private static final int FOLLOW_PREVIEW_RANGE = 64;
    private static final Path TEMPORARY_AREA_PATH = Path.of(".iterablock-memory", "temporary-area.litematic").toAbsolutePath().normalize();
    private static final Random RANDOM = new Random();
    private static ToolMode mode = ToolMode.AREA_COPY_PASTE;
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

    public static void setMode(ToolMode newMode) {
        if (newMode == null || mode == newMode) {
            return;
        }

        mode = newMode;
        if (mode == ToolMode.LINEAR_ARRAY || mode == ToolMode.VOLUME_ARRAY) {
            SchematicPlacementState.resetArrayCounts();
        }
        setLastAction(Lang.tr("iterablock.tool.action.mode_changed", mode.getDisplayName()));
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

        SchematicPlacementState.preview(ClientToolState.currentLitematic, getPlacementOrigin(minecraft, FOLLOW_PREVIEW_RANGE));
    }

    public static void adjustLinearArray(Minecraft minecraft, int amount) {
        if (mode != ToolMode.LINEAR_ARRAY || minecraft.player == null || amount == 0 || !SchematicPlacementState.hasPlacement()) {
            return;
        }

        SchematicPlacementState.adjustLinearArray(minecraft.player.getLookAngle(), amount);
        setLastAction(Lang.tr("iterablock.tool.action.linear_array_count", SchematicPlacementState.getLinearArrayAxisName(), SchematicPlacementState.getLinearArrayCount()));
    }

    public static void adjustVolumeArray(Minecraft minecraft, int amount) {
        if (mode != ToolMode.VOLUME_ARRAY || minecraft.player == null || amount == 0 || !SchematicPlacementState.hasPlacement()) {
            return;
        }

        SchematicPlacementState.adjustVolumeArray(minecraft.player.getLookAngle(), amount);
        setLastAction(Lang.tr("iterablock.tool.action.volume_array_count", SchematicPlacementState.getVolumeArraySummary()));
    }

    public static boolean adjustAreaSelection(Minecraft minecraft, int amount) {
        if (mode != ToolMode.AREA_COPY_PASTE || minecraft.player == null || amount == 0 || !AreaSelectionState.hasFirstCorner()) {
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

        if (mode == ToolMode.AREA_COPY_PASTE) {
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
            case SCHEMATIC_PLACEMENT, LINEAR_ARRAY, VOLUME_ARRAY -> handleSchematicPlacementSecondary(minecraft);
            case BEZIER_CURVE_GENERATION -> handleBezierSecondary(minecraft);
            case SYMMETRY_PLACEMENT -> handleSymmetrySecondary(minecraft);
            default -> setLastAction(withCurrentLitematic(Lang.tr("iterablock.tool.action.mode_secondary", mode.getDisplayName())));
        }

        return true;
    }

    public static boolean placeCurrentProjection(Minecraft minecraft) {
        if (mode == ToolMode.BEZIER_CURVE_GENERATION) {
            return placeBezierCurve(minecraft);
        }

        if (mode == ToolMode.RANDOM_SCHEMATIC_PLACEMENT) {
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
        sendPlacedBlocks(blocks, replaceMode);
        setLastAction(Lang.tr("iterablock.tool.action.bezier_placed", blocks.size()));
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
            SchematicPlacementState.preview(ClientToolState.currentLitematic, getPlacementOrigin(minecraft, FOLLOW_PREVIEW_RANGE));
            placeCurrentProjection(minecraft);
            return;
        }

        BlockPos origin = getPlacementOrigin(minecraft);
        SchematicPlacementState.place(ClientToolState.currentLitematic, origin);
        setLastAction(withCurrentLitematic(Lang.tr("iterablock.tool.action.placement_projected")));
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
        if (mode == ToolMode.LINEAR_ARRAY) {
            List<BlockPos> offsets = new java.util.ArrayList<>();
            int copies = SchematicPlacementState.getLinearArrayCopyCount();

            for (int copy = 0; copy < copies; copy++) {
                offsets.add(SchematicPlacementState.getLinearArrayOffset(copy, arrayStep));
            }

            return offsets;
        }

        if (mode == ToolMode.VOLUME_ARRAY) {
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

                BlockPos pos = origin.offset(transformBlockOffset(region.position(), block.pos(), region.size(), rotationSteps, mirrorAxis));
                collector.accept(new PlacedBlock(pos, state));
            }
        }
    }

    private static void sendPlacedBlocks(List<PlacedBlock> blocks, PlacementReplaceMode replaceMode) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null || Minecraft.getInstance().getConnection() == null) {
            return;
        }

        CommandFeedbackSilencer.getInstance().expectPlacementFeedback(blocks.size());

        for (PlacedBlock block : blocks) {
            player.connection.sendCommand(toSetBlockCommand(block, replaceMode));
        }
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

            sendPlacedBlocks(this.buffer, this.replaceMode);
            this.totalSent += this.buffer.size();
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

    private static String getMirrorAxisName(SchematicPlacementState.MirrorAxis axis) {
        return switch (axis) {
            case X -> "X";
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

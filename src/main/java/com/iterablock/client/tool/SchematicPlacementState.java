package com.iterablock.client.tool;

import java.util.ArrayList;
import java.util.List;

import com.iterablock.client.template.LoadedLitematicManager;
import com.iterablock.client.litematica.LitematicaSchematicInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SchematicPlacementState {
    private static final int MAX_LINEAR_ARRAY_COUNT = 64;
    private static final int MAX_VOLUME_ARRAY_AXIS_COUNT = 4;
    private static LoadedLitematicManager.Entry entry;
    private static BlockPos origin;
    private static int rotationSteps;
    private static int linearArrayX;
    private static int linearArrayY;
    private static int linearArrayZ;
    private static int volumeArrayX;
    private static int volumeArrayY;
    private static int volumeArrayZ;
    private static int overlapX;
    private static int overlapY;
    private static int overlapZ;

    private SchematicPlacementState() {
    }

    public static void place(LoadedLitematicManager.Entry selectedEntry, BlockPos selectedOrigin) {
        entry = selectedEntry;
        origin = selectedOrigin;
        resetArrayCounts();
    }

    public static LoadedLitematicManager.Entry getEntry() {
        return entry;
    }

    public static BlockPos getOrigin() {
        return origin;
    }

    public static boolean hasPlacement() {
        return entry != null && origin != null;
    }

    public static int getRotationSteps() {
        return rotationSteps;
    }

    public static void rotateClockwise() {
        if (hasPlacement()) {
            rotationSteps = (rotationSteps + 1) & 3;
        }
    }

    public static void adjustLinearArray(Vec3 lookDirection, int amount) {
        if (amount == 0) {
            return;
        }

        double absX = Math.abs(lookDirection.x);
        double absY = Math.abs(lookDirection.y);
        double absZ = Math.abs(lookDirection.z);

        if (absX >= absY && absX >= absZ) {
            linearArrayX = clampLinearArrayCount(linearArrayX + signFrom(lookDirection.x) * amount);
            linearArrayY = 0;
            linearArrayZ = 0;
        } else if (absY >= absZ) {
            linearArrayX = 0;
            linearArrayY = clampLinearArrayCount(linearArrayY + signFrom(lookDirection.y) * amount);
            linearArrayZ = 0;
        } else {
            linearArrayX = 0;
            linearArrayY = 0;
            linearArrayZ = clampLinearArrayCount(linearArrayZ + signFrom(lookDirection.z) * amount);
        }
    }

    public static void adjustVolumeArray(Vec3 lookDirection, int amount) {
        if (amount == 0) {
            return;
        }

        double absX = Math.abs(lookDirection.x);
        double absY = Math.abs(lookDirection.y);
        double absZ = Math.abs(lookDirection.z);

        if (absX >= absY && absX >= absZ) {
            volumeArrayX = clampVolumeArrayCount(volumeArrayX + signFrom(lookDirection.x) * amount);
        } else if (absY >= absZ) {
            volumeArrayY = clampVolumeArrayCount(volumeArrayY + signFrom(lookDirection.y) * amount);
        } else {
            volumeArrayZ = clampVolumeArrayCount(volumeArrayZ + signFrom(lookDirection.z) * amount);
        }
    }

    public static int getLinearArrayCopyCount() {
        return Math.abs(getLinearArrayCount()) + 1;
    }

    public static int getLinearArrayCount() {
        if (linearArrayX != 0) {
            return linearArrayX;
        }

        if (linearArrayY != 0) {
            return linearArrayY;
        }

        return linearArrayZ;
    }

    public static String getLinearArrayAxisName() {
        if (linearArrayX != 0) {
            return "X";
        }

        if (linearArrayY != 0) {
            return "Y";
        }

        if (linearArrayZ != 0) {
            return "Z";
        }

        return "-";
    }

    public static BlockPos getLinearArrayOffset(int copyIndex, BlockPos step) {
        if (copyIndex <= 0) {
            return BlockPos.ZERO;
        }

        BlockPos overlapStep = applyOverlap(step);

        if (linearArrayX != 0) {
            return new BlockPos(Integer.signum(linearArrayX) * overlapStep.getX() * copyIndex, 0, 0);
        }

        if (linearArrayY != 0) {
            return new BlockPos(0, Integer.signum(linearArrayY) * overlapStep.getY() * copyIndex, 0);
        }

        if (linearArrayZ != 0) {
            return new BlockPos(0, 0, Integer.signum(linearArrayZ) * overlapStep.getZ() * copyIndex);
        }

        return BlockPos.ZERO;
    }

    public static List<BlockPos> getVolumeArrayOffsets(BlockPos step) {
        BlockPos overlapStep = applyOverlap(step);
        int xCopies = Math.abs(volumeArrayX) + 1;
        int yCopies = Math.abs(volumeArrayY) + 1;
        int zCopies = Math.abs(volumeArrayZ) + 1;
        int xSign = Integer.signum(volumeArrayX);
        int ySign = Integer.signum(volumeArrayY);
        int zSign = Integer.signum(volumeArrayZ);
        List<BlockPos> offsets = new ArrayList<>(xCopies * yCopies * zCopies);

        for (int x = 0; x < xCopies; x++) {
            for (int y = 0; y < yCopies; y++) {
                for (int z = 0; z < zCopies; z++) {
                    offsets.add(new BlockPos(xSign * overlapStep.getX() * x, ySign * overlapStep.getY() * y, zSign * overlapStep.getZ() * z));
                }
            }
        }

        return offsets;
    }

    public static void adjustOverlap(Axis axis, int amount) {
        switch (axis) {
            case X -> overlapX = clampOverlap(overlapX + amount);
            case Y -> overlapY = clampOverlap(overlapY + amount);
            case Z -> overlapZ = clampOverlap(overlapZ + amount);
        }
    }

    public static void resetOverlap() {
        overlapX = 0;
        overlapY = 0;
        overlapZ = 0;
    }

    public static int getOverlap(Axis axis) {
        return switch (axis) {
            case X -> overlapX;
            case Y -> overlapY;
            case Z -> overlapZ;
        };
    }

    public static Axis getLookAxis(Vec3 lookDirection) {
        double absX = Math.abs(lookDirection.x);
        double absY = Math.abs(lookDirection.y);
        double absZ = Math.abs(lookDirection.z);

        if (absX >= absY && absX >= absZ) {
            return Axis.X;
        }

        return absY >= absZ ? Axis.Y : Axis.Z;
    }

    public static String getVolumeArraySummary() {
        return "X " + volumeArrayX + " / Y " + volumeArrayY + " / Z " + volumeArrayZ;
    }

    public static int getVolumeArrayCount(Axis axis) {
        return switch (axis) {
            case X -> volumeArrayX;
            case Y -> volumeArrayY;
            case Z -> volumeArrayZ;
        };
    }

    public static BlockPos getLinearArrayStep(LitematicaSchematicInfo info) {
        Integer minX = null;
        int minY = 0;
        int minZ = 0;
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;

        for (LitematicaSchematicInfo.Region region : info.regions()) {
            for (LitematicaSchematicInfo.BlockSample block : region.blocks()) {
                BlockState state = transformState(block.state());

                if (state.isAir()) {
                    continue;
                }

                BlockPos pos = transformBlockOffset(region.position(), block.pos(), region.size());

                if (minX == null) {
                    minX = pos.getX();
                    minY = pos.getY();
                    minZ = pos.getZ();
                    maxX = pos.getX();
                    maxY = pos.getY();
                    maxZ = pos.getZ();
                } else {
                    minX = Math.min(minX, pos.getX());
                    minY = Math.min(minY, pos.getY());
                    minZ = Math.min(minZ, pos.getZ());
                    maxX = Math.max(maxX, pos.getX());
                    maxY = Math.max(maxY, pos.getY());
                    maxZ = Math.max(maxZ, pos.getZ());
                }
            }
        }

        if (minX == null) {
            return new BlockPos(1, 1, 1);
        }

        return new BlockPos(Math.max(1, maxX - minX + 1), Math.max(1, maxY - minY + 1), Math.max(1, maxZ - minZ + 1));
    }

    public static BlockPos transformBlockOffset(BlockPos regionPosition, BlockPos localPos, BlockPos regionSize) {
        BlockPos oriented = orientLocalPos(localPos, regionSize);
        BlockPos offset = regionPosition.offset(oriented);
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();

        return switch (rotationSteps & 3) {
            case 1 -> new BlockPos(-z, y, x);
            case 2 -> new BlockPos(-x, y, -z);
            case 3 -> new BlockPos(z, y, -x);
            default -> offset;
        };
    }

    public static BlockState transformState(BlockState state) {
        BlockState rotated = state;

        for (int i = 0; i < (rotationSteps & 3); i++) {
            rotated = rotated.rotate(Rotation.CLOCKWISE_90);
        }

        return rotated;
    }

    public static void clearIfEntry(LoadedLitematicManager.Entry removedEntry) {
        if (entry == removedEntry) {
            clear();
        }
    }

    public static void clear() {
        entry = null;
        origin = null;
        rotationSteps = 0;
        resetArrayCounts();
    }

    public static void resetArrayCounts() {
        linearArrayX = 0;
        linearArrayY = 0;
        linearArrayZ = 0;
        volumeArrayX = 0;
        volumeArrayY = 0;
        volumeArrayZ = 0;
    }

    private static BlockPos orientLocalPos(BlockPos localPos, BlockPos regionSize) {
        int x = regionSize.getX() < 0 ? -localPos.getX() : localPos.getX();
        int y = regionSize.getY() < 0 ? -localPos.getY() : localPos.getY();
        int z = regionSize.getZ() < 0 ? -localPos.getZ() : localPos.getZ();
        return new BlockPos(x, y, z);
    }

    private static int clampLinearArrayCount(int count) {
        return Math.max(-MAX_LINEAR_ARRAY_COUNT, Math.min(MAX_LINEAR_ARRAY_COUNT, count));
    }

    private static int clampVolumeArrayCount(int count) {
        return Math.max(-MAX_VOLUME_ARRAY_AXIS_COUNT, Math.min(MAX_VOLUME_ARRAY_AXIS_COUNT, count));
    }

    private static int clampOverlap(int value) {
        return Math.max(0, Math.min(999, value));
    }

    private static BlockPos applyOverlap(BlockPos step) {
        return new BlockPos(
                Math.max(1, step.getX() - overlapX),
                Math.max(1, step.getY() - overlapY),
                Math.max(1, step.getZ() - overlapZ)
        );
    }

    private static int signFrom(double value) {
        return value < 0.0 ? -1 : 1;
    }

    public enum Axis {
        X,
        Y,
        Z
    }
}

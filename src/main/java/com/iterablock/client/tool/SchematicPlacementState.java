package com.iterablock.client.tool;

import com.iterablock.client.template.LoadedLitematicManager;
import com.iterablock.client.litematica.LitematicaSchematicInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SchematicPlacementState {
    private static final int MAX_LINEAR_ARRAY_COUNT = 64;
    private static LoadedLitematicManager.Entry entry;
    private static BlockPos origin;
    private static int rotationSteps;
    private static int linearArrayX;
    private static int linearArrayY;
    private static int linearArrayZ;

    private SchematicPlacementState() {
    }

    public static void place(LoadedLitematicManager.Entry selectedEntry, BlockPos selectedOrigin) {
        entry = selectedEntry;
        origin = selectedOrigin;
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

        if (linearArrayX != 0) {
            return new BlockPos(Integer.signum(linearArrayX) * step.getX() * copyIndex, 0, 0);
        }

        if (linearArrayY != 0) {
            return new BlockPos(0, Integer.signum(linearArrayY) * step.getY() * copyIndex, 0);
        }

        if (linearArrayZ != 0) {
            return new BlockPos(0, 0, Integer.signum(linearArrayZ) * step.getZ() * copyIndex);
        }

        return BlockPos.ZERO;
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
            entry = null;
            origin = null;
            rotationSteps = 0;
        }
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

    private static int signFrom(double value) {
        return value < 0.0 ? -1 : 1;
    }
}

package com.iterablock.client.tool;

import net.minecraft.core.BlockPos;

public final class AreaSelectionState {
    private static BlockPos firstCorner;
    private static BlockPos secondCorner;
    private static Corner activeCorner = Corner.SECOND;

    private AreaSelectionState() {
    }

    public static void setFirstCorner(BlockPos pos) {
        firstCorner = pos;
        activeCorner = Corner.FIRST;
    }

    public static void setSecondCorner(BlockPos pos) {
        secondCorner = pos;
        activeCorner = Corner.SECOND;
    }

    public static BlockPos getFirstCorner() {
        return firstCorner;
    }

    public static BlockPos getSecondCorner() {
        return secondCorner;
    }

    public static boolean hasFirstCorner() {
        return firstCorner != null;
    }

    public static boolean hasSecondCorner() {
        return secondCorner != null;
    }

    public static boolean hasSelection() {
        return firstCorner != null && secondCorner != null;
    }

    public static BlockPos getMinCorner() {
        if (!hasSelection()) {
            return null;
        }

        return new BlockPos(
                Math.min(firstCorner.getX(), secondCorner.getX()),
                Math.min(firstCorner.getY(), secondCorner.getY()),
                Math.min(firstCorner.getZ(), secondCorner.getZ())
        );
    }

    public static BlockPos getMaxCorner() {
        if (!hasSelection()) {
            return null;
        }

        return new BlockPos(
                Math.max(firstCorner.getX(), secondCorner.getX()),
                Math.max(firstCorner.getY(), secondCorner.getY()),
                Math.max(firstCorner.getZ(), secondCorner.getZ())
        );
    }

    public static int getSizeX() {
        return getAxisSize(Axis.X);
    }

    public static int getSizeY() {
        return getAxisSize(Axis.Y);
    }

    public static int getSizeZ() {
        return getAxisSize(Axis.Z);
    }

    public static Corner getActiveCorner() {
        return activeCorner;
    }

    public static int getActiveCornerNumber() {
        return activeCorner == Corner.FIRST ? 1 : 2;
    }

    public static boolean toggleActiveCorner() {
        if (firstCorner == null) {
            return false;
        }

        if (secondCorner == null) {
            activeCorner = Corner.FIRST;
            return true;
        }

        activeCorner = activeCorner == Corner.FIRST ? Corner.SECOND : Corner.FIRST;
        return true;
    }

    public static boolean adjustActiveCorner(SchematicPlacementState.Axis axis, int amount) {
        if (amount == 0) {
            return false;
        }

        if (activeCorner == Corner.FIRST) {
            if (firstCorner == null) {
                return false;
            }

            firstCorner = offset(firstCorner, axis, amount);
            return true;
        }

        if (secondCorner == null) {
            if (firstCorner == null) {
                return false;
            }

            secondCorner = firstCorner;
        }

        secondCorner = offset(secondCorner, axis, amount);
        return true;
    }

    public static void clear() {
        firstCorner = null;
        secondCorner = null;
        activeCorner = Corner.SECOND;
    }

    private static BlockPos offset(BlockPos pos, SchematicPlacementState.Axis axis, int amount) {
        return switch (axis) {
            case X -> pos.offset(amount, 0, 0);
            case Y -> pos.offset(0, amount, 0);
            case Z -> pos.offset(0, 0, amount);
        };
    }

    private static int getAxisSize(Axis axis) {
        if (!hasSelection()) {
            return 0;
        }

        return switch (axis) {
            case X -> Math.abs(firstCorner.getX() - secondCorner.getX()) + 1;
            case Y -> Math.abs(firstCorner.getY() - secondCorner.getY()) + 1;
            case Z -> Math.abs(firstCorner.getZ() - secondCorner.getZ()) + 1;
        };
    }

    private enum Axis {
        X,
        Y,
        Z
    }

    public enum Corner {
        FIRST,
        SECOND
    }
}

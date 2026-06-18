package com.iterablock.client.tool;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class SymmetryPlacementState {
    private static final int MIN_RADIUS = 0;
    private static final int MAX_RADIUS = 128;
    private static final int MIN_HEIGHT = 0;
    private static final int MAX_HEIGHT = 128;
    private static BlockPos center;
    private static int radius = 4;
    private static int height = 2;
    private static boolean locked;

    private SymmetryPlacementState() {
    }

    public static void setCenter(BlockPos selectedCenter) {
        center = selectedCenter;
        locked = false;
    }

    public static BlockPos getCenter() {
        return center;
    }

    public static int getRadius() {
        return radius;
    }

    public static int getHeight() {
        return height;
    }

    public static boolean hasCenter() {
        return center != null;
    }

    public static boolean isLocked() {
        return locked;
    }

    public static boolean toggleLockOrClear() {
        if (center == null) {
            return false;
        }

        if (locked) {
            clear();
        } else {
            locked = true;
        }

        return true;
    }

    public static boolean adjust(Vec3 lookDirection, int amount) {
        if (center == null || locked || amount == 0) {
            return false;
        }

        SchematicPlacementState.Axis axis = SchematicPlacementState.getLookAxis(lookDirection);

        if (axis == SchematicPlacementState.Axis.Y) {
            height = clamp(height + amount, MIN_HEIGHT, MAX_HEIGHT);
        } else {
            radius = clamp(radius + amount, MIN_RADIUS, MAX_RADIUS);
        }

        return true;
    }

    public static boolean contains(BlockPos pos) {
        if (center == null) {
            return false;
        }

        return Math.abs(pos.getX() - center.getX()) <= radius
                && Math.abs(pos.getZ() - center.getZ()) <= radius
                && Math.abs(pos.getY() - center.getY()) <= height;
    }

    public static List<MirrorPlacement> getMirrorPlacements(BlockPos placedPos) {
        if (!locked || center == null || !contains(placedPos)) {
            return List.of();
        }

        List<MirrorPlacement> placements = new ArrayList<>(3);
        int mirroredX = center.getX() * 2 - placedPos.getX();
        int mirroredZ = center.getZ() * 2 - placedPos.getZ();
        BlockPos xMirror = new BlockPos(mirroredX, placedPos.getY(), placedPos.getZ());
        BlockPos zMirror = new BlockPos(placedPos.getX(), placedPos.getY(), mirroredZ);
        BlockPos xzMirror = new BlockPos(mirroredX, placedPos.getY(), mirroredZ);

        addMirrorPlacement(placements, placedPos, xMirror, true, false);
        addMirrorPlacement(placements, placedPos, zMirror, false, true);
        addMirrorPlacement(placements, placedPos, xzMirror, true, true);
        return List.copyOf(placements);
    }

    public static Bounds getBounds() {
        if (center == null) {
            return null;
        }

        return new Bounds(
                center.getX() - radius,
                center.getY() - height,
                center.getZ() - radius,
                center.getX() + radius + 1,
                center.getY() + height + 1,
                center.getZ() + radius + 1
        );
    }

    public static void clear() {
        center = null;
        locked = false;
    }

    private static void addMirrorPlacement(List<MirrorPlacement> placements, BlockPos original, BlockPos mirrored, boolean mirrorX, boolean mirrorZ) {
        if (!mirrored.equals(original) && contains(mirrored)) {
            placements.add(new MirrorPlacement(mirrored, mirrorX, mirrorZ));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record MirrorPlacement(BlockPos pos, boolean mirrorX, boolean mirrorZ) {
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }
}

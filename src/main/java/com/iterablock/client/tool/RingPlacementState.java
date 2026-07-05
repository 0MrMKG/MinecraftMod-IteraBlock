package com.iterablock.client.tool;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

public final class RingPlacementState {
    private static final int DEFAULT_COUNT = 6;
    private static final int DEFAULT_RADIUS = 10;
    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 64;
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 256;
    private static int count = DEFAULT_COUNT;
    private static int radius = DEFAULT_RADIUS;

    private RingPlacementState() {
    }

    public static int getCount() {
        return count;
    }

    public static int getRadius() {
        return radius;
    }

    public static void adjustCount(int amount) {
        count = clamp(count + amount, MIN_COUNT, MAX_COUNT);
    }

    public static void adjustRadius(int amount) {
        radius = clamp(radius + amount, MIN_RADIUS, MAX_RADIUS);
    }

    public static List<BlockPos> getOffsets() {
        List<BlockPos> offsets = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            double angle = getAngle(i);
            int x = (int) Math.round(Math.cos(angle) * radius);
            int z = (int) Math.round(Math.sin(angle) * radius);
            offsets.add(new BlockPos(x, 0, z));
        }

        return offsets;
    }

    public static int getRotationSteps(int index) {
        double quarterTurns = getAngle(index) / (Math.PI / 2.0);
        return Math.floorMod(SchematicPlacementState.getRotationSteps() + (int) Math.round(quarterTurns), 4);
    }

    private static double getAngle(int index) {
        return (Math.PI * 2.0 * index) / Math.max(1, count);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

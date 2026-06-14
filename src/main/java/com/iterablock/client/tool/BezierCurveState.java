package com.iterablock.client.tool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.iterablock.client.config.BuilderHelperClientConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class BezierCurveState {
    private static final int REQUIRED_POINTS = 4;
    private static final int MAX_SAMPLES = 4096;
    private static final List<BlockPos> CONTROL_POINTS = new ArrayList<>(REQUIRED_POINTS);
    private static final List<BlockPos> PREVIOUS_CONTROL_POINTS = new ArrayList<>(REQUIRED_POINTS);

    private BezierCurveState() {
    }

    public static boolean addControlPoint(BlockPos point) {
        if (CONTROL_POINTS.size() >= REQUIRED_POINTS) {
            PREVIOUS_CONTROL_POINTS.clear();
            PREVIOUS_CONTROL_POINTS.addAll(CONTROL_POINTS);
            CONTROL_POINTS.clear();
            return false;
        }

        CONTROL_POINTS.add(point);
        return true;
    }

    public static List<BlockPos> getControlPoints() {
        return List.copyOf(CONTROL_POINTS);
    }

    public static List<BlockPos> getPreviousControlPoints() {
        return List.copyOf(PREVIOUS_CONTROL_POINTS);
    }

    public static int getPointCount() {
        return CONTROL_POINTS.size();
    }

    public static boolean isReady() {
        return CONTROL_POINTS.size() == REQUIRED_POINTS;
    }

    public static void clear() {
        CONTROL_POINTS.clear();
        PREVIOUS_CONTROL_POINTS.clear();
    }

    public static List<BlockPos> getCurveBlocks() {
        return getCurveBlocks(BuilderHelperClientConfig.getBezierPlacementPrecision(), BuilderHelperClientConfig.getBezierPlacementWidth());
    }

    public static List<BlockPos> getCurveBlocks(int precision, int width) {
        List<CurvePlacement> placements = getCurvePlacements(precision, width);
        Set<BlockPos> blocks = new LinkedHashSet<>();

        for (CurvePlacement placement : placements) {
            blocks.add(placement.pos());
        }

        return List.copyOf(blocks);
    }

    public static List<CurvePlacement> getCurvePlacements(int precision, int width) {
        if (!isReady()) {
            return List.of();
        }

        Vec3 p0 = Vec3.atCenterOf(CONTROL_POINTS.get(0));
        Vec3 p1 = Vec3.atCenterOf(CONTROL_POINTS.get(1));
        Vec3 p2 = Vec3.atCenterOf(CONTROL_POINTS.get(2));
        Vec3 p3 = Vec3.atCenterOf(CONTROL_POINTS.get(3));
        int step = Math.max(1, precision);
        int samples = Math.min(MAX_SAMPLES, Math.max(1, (int) Math.ceil(estimateLength(p0, p1, p2, p3) / step)));
        int radius = Math.max(0, (width - 1) / 2);
        List<CurvePlacement> placements = new ArrayList<>();
        Set<BlockPos> visited = new LinkedHashSet<>();

        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            Vec3 tangent = tangent(p0, p1, p2, p3, t);
            BlockPos current = toBlockPos(sample(p0, p1, p2, p3, t));
            addWidthPlacements(current, getRotationStepsForTangent(tangent), radius, visited, placements);
        }

        return List.copyOf(placements);
    }

    public static int getSamplePointCount() {
        if (!isReady()) {
            return 0;
        }

        Vec3 p0 = Vec3.atCenterOf(CONTROL_POINTS.get(0));
        Vec3 p1 = Vec3.atCenterOf(CONTROL_POINTS.get(1));
        Vec3 p2 = Vec3.atCenterOf(CONTROL_POINTS.get(2));
        Vec3 p3 = Vec3.atCenterOf(CONTROL_POINTS.get(3));
        int step = Math.max(1, BuilderHelperClientConfig.getBezierPlacementPrecision());
        return Math.min(MAX_SAMPLES, Math.max(1, (int) Math.ceil(estimateLength(p0, p1, p2, p3) / step))) + 1;
    }

    private static double estimateLength(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        return p0.distanceTo(p1) + p1.distanceTo(p2) + p2.distanceTo(p3);
    }

    private static Vec3 sample(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double inv = 1.0 - t;
        double a = inv * inv * inv;
        double b = 3.0 * inv * inv * t;
        double c = 3.0 * inv * t * t;
        double d = t * t * t;
        return new Vec3(
                p0.x * a + p1.x * b + p2.x * c + p3.x * d,
                p0.y * a + p1.y * b + p2.y * c + p3.y * d,
                p0.z * a + p1.z * b + p2.z * c + p3.z * d
        );
    }

    private static Vec3 tangent(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double inv = 1.0 - t;
        return new Vec3(
                3.0 * inv * inv * (p1.x - p0.x) + 6.0 * inv * t * (p2.x - p1.x) + 3.0 * t * t * (p3.x - p2.x),
                3.0 * inv * inv * (p1.y - p0.y) + 6.0 * inv * t * (p2.y - p1.y) + 3.0 * t * t * (p3.y - p2.y),
                3.0 * inv * inv * (p1.z - p0.z) + 6.0 * inv * t * (p2.z - p1.z) + 3.0 * t * t * (p3.z - p2.z)
        );
    }

    private static BlockPos toBlockPos(Vec3 point) {
        return BlockPos.containing(point.x, point.y, point.z);
    }

    private static void addWidthPlacements(BlockPos center, int rotationSteps, int radius, Set<BlockPos> visited, List<CurvePlacement> placements) {
        if (radius <= 0) {
            addPlacement(center, rotationSteps, visited, placements);
            return;
        }

        boolean spreadOnX = (rotationSteps & 1) == 0;

        for (int offset = -radius; offset <= radius; offset++) {
            BlockPos pos = spreadOnX ? center.offset(offset, 0, 0) : center.offset(0, 0, offset);
            addPlacement(pos, rotationSteps, visited, placements);
        }
    }

    private static void addPlacement(BlockPos pos, int rotationSteps, Set<BlockPos> visited, List<CurvePlacement> placements) {
        if (visited.add(pos)) {
            placements.add(new CurvePlacement(pos, rotationSteps));
        }
    }

    private static int getRotationStepsForTangent(Vec3 tangent) {
        if (Math.abs(tangent.x) >= Math.abs(tangent.z)) {
            return tangent.x >= 0.0 ? 1 : 3;
        }

        return tangent.z >= 0.0 ? 0 : 2;
    }

    public record CurvePlacement(BlockPos pos, int rotationSteps) {
    }
}

package com.iterablock.client.tool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.iterablock.client.config.BuilderHelperClientConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class BezierCurveState {
    private static final int MAX_SAMPLES = 4096;
    private static final List<BlockPos> CONTROL_POINTS = new ArrayList<>();
    private static final List<BlockPos> PREVIOUS_CONTROL_POINTS = new ArrayList<>();

    private BezierCurveState() {
    }

    public static boolean addControlPoint(BlockPos point) {
        if (CONTROL_POINTS.size() >= getRequiredPointCount()) {
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

    public static int getRequiredPointCount() {
        return BuilderHelperClientConfig.getBezierControlPointCount();
    }

    public static boolean isReady() {
        return CONTROL_POINTS.size() >= getRequiredPointCount();
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

        List<Vec3> points = getActiveCurvePoints();
        int step = Math.max(1, precision);
        int samples = Math.min(MAX_SAMPLES, Math.max(1, (int) Math.ceil(estimateLength(points) / step)));
        int radius = Math.max(0, (width - 1) / 2);
        List<CurvePlacement> placements = new ArrayList<>();
        Set<BlockPos> visited = new LinkedHashSet<>();

        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            Vec3 tangent = tangent(points, t);
            BlockPos current = toBlockPos(sample(points, t));
            addWidthPlacements(current, getRotationStepsForTangent(tangent), radius, visited, placements);
        }

        return List.copyOf(placements);
    }

    public static int getSamplePointCount() {
        if (!isReady()) {
            return 0;
        }

        List<Vec3> points = getActiveCurvePoints();
        int step = Math.max(1, BuilderHelperClientConfig.getBezierPlacementPrecision());
        return Math.min(MAX_SAMPLES, Math.max(1, (int) Math.ceil(estimateLength(points) / step))) + 1;
    }

    private static List<Vec3> getActiveCurvePoints() {
        int count = Math.min(CONTROL_POINTS.size(), getRequiredPointCount());
        List<Vec3> points = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            points.add(Vec3.atCenterOf(CONTROL_POINTS.get(i)));
        }

        return points;
    }

    private static double estimateLength(List<Vec3> points) {
        double length = 0.0;

        for (int i = 1; i < points.size(); i++) {
            length += points.get(i - 1).distanceTo(points.get(i));
        }

        return length;
    }

    private static Vec3 sample(List<Vec3> points, double t) {
        if (points.isEmpty()) {
            return new Vec3(0.0, 0.0, 0.0);
        }

        List<Vec3> work = new ArrayList<>(points);

        for (int level = work.size() - 1; level > 0; level--) {
            for (int i = 0; i < level; i++) {
                work.set(i, lerp(work.get(i), work.get(i + 1), t));
            }
        }

        return work.get(0);
    }

    private static Vec3 tangent(List<Vec3> points, double t) {
        int degree = points.size() - 1;

        if (degree <= 0) {
            return new Vec3(0.0, 0.0, 0.0);
        }

        List<Vec3> derivativePoints = new ArrayList<>(degree);

        for (int i = 0; i < degree; i++) {
            Vec3 current = points.get(i);
            Vec3 next = points.get(i + 1);
            derivativePoints.add(new Vec3(
                    (next.x - current.x) * degree,
                    (next.y - current.y) * degree,
                    (next.z - current.z) * degree
            ));
        }

        return sample(derivativePoints, t);
    }

    private static Vec3 lerp(Vec3 start, Vec3 end, double t) {
        return new Vec3(
                start.x + (end.x - start.x) * t,
                start.y + (end.y - start.y) * t,
                start.z + (end.z - start.z) * t
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

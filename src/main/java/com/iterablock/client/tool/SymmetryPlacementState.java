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
    private static Kind kind = Kind.CENTER;
    private static Parity parity = Parity.ODD;
    private static PlaneAxis planeAxis = PlaneAxis.X;

    private SymmetryPlacementState() {
    }

    public static void setCenter(BlockPos selectedCenter, Vec3 lookDirection) {
        center = selectedCenter;
        if (kind == Kind.PLANE) {
            planeAxis = Math.abs(lookDirection.x) >= Math.abs(lookDirection.z) ? PlaneAxis.X : PlaneAxis.Z;
        }
        locked = false;
    }

    public static Kind getKind() {
        return kind;
    }

    public static Parity getParity() {
        return parity;
    }

    public static PlaneAxis getPlaneAxis() {
        return planeAxis;
    }

    public static Kind toggleKind() {
        kind = kind == Kind.CENTER ? Kind.PLANE : Kind.CENTER;
        locked = false;
        return kind;
    }

    public static Parity toggleParity() {
        parity = parity == Parity.ODD ? Parity.EVEN : Parity.ODD;
        locked = false;
        return parity;
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

        if (kind == Kind.PLANE) {
            return getPlaneMirrorPlacements(placedPos);
        }

        List<MirrorPlacement> placements = new ArrayList<>(3);
        int centerOffset = parity == Parity.EVEN ? 1 : 0;
        int mirroredX = center.getX() * 2 + centerOffset - placedPos.getX();
        int mirroredZ = center.getZ() * 2 + centerOffset - placedPos.getZ();
        BlockPos xMirror = new BlockPos(mirroredX, placedPos.getY(), placedPos.getZ());
        BlockPos zMirror = new BlockPos(placedPos.getX(), placedPos.getY(), mirroredZ);
        BlockPos xzMirror = new BlockPos(mirroredX, placedPos.getY(), mirroredZ);

        addMirrorPlacement(placements, placedPos, xMirror, true, false);
        addMirrorPlacement(placements, placedPos, zMirror, false, true);
        addMirrorPlacement(placements, placedPos, xzMirror, true, true);
        return List.copyOf(placements);
    }

    public static List<BlockPos> getCenterMarkerBlocks() {
        if (center == null) {
            return List.of();
        }

        if (kind == Kind.CENTER && parity == Parity.EVEN) {
            return List.of();
        }

        if (kind == Kind.PLANE) {
            if (parity == Parity.EVEN) {
                return List.of();
            }

            List<BlockPos> blocks = new ArrayList<>(parity == Parity.EVEN ? 18 : 9);

            for (int vertical = -1; vertical <= 1; vertical++) {
                for (int across = -1; across <= 1; across++) {
                    if (planeAxis == PlaneAxis.X) {
                        blocks.add(center.offset(0, vertical, across));
                        if (parity == Parity.EVEN) {
                            blocks.add(center.offset(1, vertical, across));
                        }
                    } else {
                        blocks.add(center.offset(across, vertical, 0));
                        if (parity == Parity.EVEN) {
                            blocks.add(center.offset(across, vertical, 1));
                        }
                    }
                }
            }

            return List.copyOf(blocks);
        }

        return List.of(center);
    }

    public static PlaneBounds getPlaneMarkerBounds() {
        if (center == null || kind != Kind.PLANE || parity != Parity.EVEN) {
            return null;
        }

        double thickness = 0.02D;

        if (planeAxis == PlaneAxis.X) {
            double x = center.getX() + 1.0D;
            return new PlaneBounds(x - thickness, center.getY() - 1.0D, center.getZ() - 1.0D, x + thickness, center.getY() + 2.0D, center.getZ() + 2.0D);
        }

        double z = center.getZ() + 1.0D;
        return new PlaneBounds(center.getX() - 1.0D, center.getY() - 1.0D, z - thickness, center.getX() + 2.0D, center.getY() + 2.0D, z + thickness);
    }

    public static List<PlaneBounds> getEvenCenterMarkerBounds() {
        if (center == null || kind != Kind.CENTER || parity != Parity.EVEN) {
            return List.of();
        }

        double thickness = 0.02D;
        double x = center.getX() + 1.0D;
        double z = center.getZ() + 1.0D;

        return List.of(
                new PlaneBounds(x - thickness, center.getY(), center.getZ(), x + thickness, center.getY() + 1.0D, center.getZ() + 2.0D),
                new PlaneBounds(center.getX(), center.getY(), z - thickness, center.getX() + 2.0D, center.getY() + 1.0D, z + thickness)
        );
    }

    public static Bounds getBounds() {
        if (center == null) {
            return null;
        }

        int extraX = parity == Parity.EVEN && (kind == Kind.CENTER || planeAxis == PlaneAxis.X) ? 1 : 0;
        int extraZ = parity == Parity.EVEN && (kind == Kind.CENTER || planeAxis == PlaneAxis.Z) ? 1 : 0;

        return new Bounds(
                center.getX() - radius,
                center.getY() - height,
                center.getZ() - radius,
                center.getX() + radius + extraX + 1,
                center.getY() + height + 1,
                center.getZ() + radius + extraZ + 1
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

    private static List<MirrorPlacement> getPlaneMirrorPlacements(BlockPos placedPos) {
        List<MirrorPlacement> placements = new ArrayList<>(1);
        int planeOffset = parity == Parity.EVEN ? 1 : 0;
        BlockPos mirrored = planeAxis == PlaneAxis.X
                ? new BlockPos(center.getX() * 2 + planeOffset - placedPos.getX(), placedPos.getY(), placedPos.getZ())
                : new BlockPos(placedPos.getX(), placedPos.getY(), center.getZ() * 2 + planeOffset - placedPos.getZ());

        addMirrorPlacement(placements, placedPos, mirrored, planeAxis == PlaneAxis.X, planeAxis == PlaneAxis.Z);
        return List.copyOf(placements);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record MirrorPlacement(BlockPos pos, boolean mirrorX, boolean mirrorZ) {
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    public record PlaneBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }

    public enum Kind {
        CENTER("iterablock.tool.symmetry.kind.center"),
        PLANE("iterablock.tool.symmetry.kind.plane");

        private final String translationKey;

        Kind(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }

    public enum Parity {
        ODD("iterablock.tool.symmetry.parity.odd"),
        EVEN("iterablock.tool.symmetry.parity.even");

        private final String translationKey;

        Parity(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }

    public enum PlaneAxis {
        X,
        Z
    }
}

package dev.traveler.core.common.geometry;

import java.util.Objects;

public record WorldPoint(double x, double y, double z) {
    public WorldPoint {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public double horizontalDistanceTo(WorldPoint target) {
        WorldPoint point = Objects.requireNonNull(target, "target");
        return Math.hypot(point.x - x, point.z - z);
    }

    public double distanceTo(WorldPoint target) {
        WorldPoint point = Objects.requireNonNull(target, "target");
        return Math.sqrt(squared(point.x - x) + squared(point.y - y) + squared(point.z - z));
    }

    public HorizontalVector horizontalVectorTo(WorldPoint target) {
        WorldPoint point = Objects.requireNonNull(target, "target");
        return new HorizontalVector(point.x - x, point.z - z);
    }

    public WorldPoint interpolate(WorldPoint target, double ratio) {
        WorldPoint point = Objects.requireNonNull(target, "target");
        double clampedRatio = Math.clamp(ratio, 0.0, 1.0);
        return new WorldPoint(
                x + (point.x - x) * clampedRatio,
                y + (point.y - y) * clampedRatio,
                z + (point.z - z) * clampedRatio);
    }

    private static double squared(double value) {
        return value * value;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}

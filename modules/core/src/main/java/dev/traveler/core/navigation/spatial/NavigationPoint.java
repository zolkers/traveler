package dev.traveler.core.navigation.spatial;

import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

public record NavigationPoint(double x, double y, double z) {
    public NavigationPoint {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public static NavigationPoint blockCenter(BlockPosition position) {
        BlockPosition blockPosition = Objects.requireNonNull(position, "position");
        return new NavigationPoint(blockPosition.x() + 0.5, blockPosition.y(), blockPosition.z() + 0.5);
    }

    public double horizontalDistanceTo(NavigationPoint target) {
        NavigationPoint point = Objects.requireNonNull(target, "target");
        return Math.hypot(point.x - x, point.z - z);
    }

    public double distanceTo(NavigationPoint target) {
        NavigationPoint point = Objects.requireNonNull(target, "target");
        return Math.sqrt(squared(point.x - x) + squared(point.y - y) + squared(point.z - z));
    }

    public HorizontalVector horizontalVectorTo(NavigationPoint target) {
        NavigationPoint point = Objects.requireNonNull(target, "target");
        return new HorizontalVector(point.x - x, point.z - z);
    }

    public NavigationPoint interpolate(NavigationPoint target, double ratio) {
        NavigationPoint point = Objects.requireNonNull(target, "target");
        double clampedRatio = Math.clamp(ratio, 0.0, 1.0);
        return new NavigationPoint(
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

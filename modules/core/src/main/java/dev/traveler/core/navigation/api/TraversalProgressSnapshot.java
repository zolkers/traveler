package dev.traveler.core.navigation.api;

import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;

public record TraversalProgressSnapshot(
        TraversalKind traversalKind,
        WorldPoint position,
        WorldPoint target,
        double progress,
        double lateralDistance,
        double targetDistance) {
    public TraversalProgressSnapshot {
        Objects.requireNonNull(traversalKind, "traversalKind");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(target, "target");
        requireFinite(progress, "progress");
        requireNonNegative(lateralDistance, "lateralDistance");
        requireNonNegative(targetDistance, "targetDistance");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }
}

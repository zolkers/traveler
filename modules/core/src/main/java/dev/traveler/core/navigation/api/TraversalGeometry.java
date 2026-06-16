package dev.traveler.core.navigation.api;

import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;

public record TraversalGeometry(
        WorldPoint entryAnchor,
        WorldPoint targetAnchor,
        WorldPoint exitAnchor,
        double lateralTolerance,
        double yawTolerance) {
    public TraversalGeometry {
        Objects.requireNonNull(entryAnchor, "entryAnchor");
        Objects.requireNonNull(targetAnchor, "targetAnchor");
        Objects.requireNonNull(exitAnchor, "exitAnchor");
        requireNonNegative(lateralTolerance, "lateralTolerance");
        requireNonNegative(yawTolerance, "yawTolerance");
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }
}

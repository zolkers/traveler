package dev.traveler.core.navigation.follow;

import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;

public record PathProjection(
        WorldPoint nearestPoint,
        HorizontalVector tangent,
        double distanceOnPath,
        double lateralError,
        double signedLateralError) {
    public PathProjection {
        Objects.requireNonNull(nearestPoint, "nearestPoint");
        Objects.requireNonNull(tangent, "tangent");
        requireNonNegative(distanceOnPath, "distanceOnPath");
        requireNonNegative(lateralError, "lateralError");
        requireFinite(signedLateralError, "signedLateralError");
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}

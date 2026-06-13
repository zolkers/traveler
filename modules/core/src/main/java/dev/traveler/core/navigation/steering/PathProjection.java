package dev.traveler.core.navigation.steering;

import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public record PathProjection(
        NavigationPoint nearestPoint,
        HorizontalVector tangent,
        double distanceOnPath,
        double lateralError) {
    public PathProjection {
        Objects.requireNonNull(nearestPoint, "nearestPoint");
        Objects.requireNonNull(tangent, "tangent");
        requireNonNegative(distanceOnPath, "distanceOnPath");
        requireNonNegative(lateralError, "lateralError");
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }
}

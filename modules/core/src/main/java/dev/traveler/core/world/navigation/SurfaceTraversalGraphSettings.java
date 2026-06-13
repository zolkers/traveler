package dev.traveler.core.world.navigation;

import java.util.Objects;

public record SurfaceTraversalGraphSettings(
        int horizontalMargin,
        int verticalMargin,
        SurfaceClearanceScorer clearanceScorer) {
    public SurfaceTraversalGraphSettings {
        requirePositive(horizontalMargin, "horizontalMargin");
        requirePositive(verticalMargin, "verticalMargin");
        Objects.requireNonNull(clearanceScorer, "clearanceScorer");
    }

    public static SurfaceTraversalGraphSettings basic(int horizontalMargin, int verticalMargin) {
        return new SurfaceTraversalGraphSettings(
                horizontalMargin,
                verticalMargin,
                SurfaceClearanceScorer.disabled());
    }

    public static SurfaceTraversalGraphSettings standard(int horizontalMargin, int verticalMargin) {
        return new SurfaceTraversalGraphSettings(
                horizontalMargin,
                verticalMargin,
                SurfaceClearanceScorer.standard());
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}

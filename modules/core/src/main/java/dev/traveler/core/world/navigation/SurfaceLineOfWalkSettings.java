package dev.traveler.core.world.navigation;

public record SurfaceLineOfWalkSettings(
        int horizontalMargin,
        int verticalMargin,
        boolean requiresAdjacentClearance) {
    public SurfaceLineOfWalkSettings {
        requirePositive(horizontalMargin, "horizontalMargin");
        requirePositive(verticalMargin, "verticalMargin");
    }

    public static SurfaceLineOfWalkSettings standard(int horizontalMargin, int verticalMargin) {
        return new SurfaceLineOfWalkSettings(horizontalMargin, verticalMargin, false);
    }

    public static SurfaceLineOfWalkSettings smoothing(int horizontalMargin, int verticalMargin) {
        return new SurfaceLineOfWalkSettings(horizontalMargin, verticalMargin, true);
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}

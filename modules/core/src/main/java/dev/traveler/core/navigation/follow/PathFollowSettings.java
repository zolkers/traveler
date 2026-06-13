package dev.traveler.core.navigation.follow;

public record PathFollowSettings(
        double reachedDistance,
        double lookAheadDistance,
        double arrivalDistance,
        double minimumSpeedScale) {
    public PathFollowSettings {
        requirePositive(reachedDistance, "reachedDistance");
        requirePositive(lookAheadDistance, "lookAheadDistance");
        requirePositive(arrivalDistance, "arrivalDistance");
        if (!Double.isFinite(minimumSpeedScale) || minimumSpeedScale < 0.0 || minimumSpeedScale > 1.0) {
            throw new IllegalArgumentException("minimumSpeedScale must be between 0 and 1.");
        }
    }

    public static PathFollowSettings standard() {
        return new PathFollowSettings(0.45, 2.0, 2.5, 0.35);
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}

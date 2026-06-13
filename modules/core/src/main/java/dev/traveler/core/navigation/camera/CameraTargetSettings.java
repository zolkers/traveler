package dev.traveler.core.navigation.camera;

public record CameraTargetSettings(
        double lookAheadDistance,
        double minimumHorizontalDistance,
        double neutralPitchDegrees) {
    public CameraTargetSettings {
        requirePositive(lookAheadDistance, "lookAheadDistance");
        requirePositive(minimumHorizontalDistance, "minimumHorizontalDistance");
        requirePitch(neutralPitchDegrees);
    }

    public static CameraTargetSettings standard() {
        return new CameraTargetSettings(3.5, 0.35, 0.0);
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }

    private static void requirePitch(double value) {
        if (!Double.isFinite(value) || Math.abs(value) > 90.0) {
            throw new IllegalArgumentException("neutralPitchDegrees must be between -90 and 90.");
        }
    }
}

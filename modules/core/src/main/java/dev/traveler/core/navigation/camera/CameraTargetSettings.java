package dev.traveler.core.navigation.camera;

public record CameraTargetSettings(
        double lookAheadDistance,
        double minimumHorizontalDistance,
        double neutralPitchDegrees,
        double verticalAimScale,
        double maxPitchDegrees) {
    public CameraTargetSettings(
            double lookAheadDistance,
            double minimumHorizontalDistance,
            double neutralPitchDegrees) {
        this(lookAheadDistance, minimumHorizontalDistance, neutralPitchDegrees, 0.65, 18.0);
    }

    public CameraTargetSettings {
        requirePositive(lookAheadDistance, "lookAheadDistance");
        requirePositive(minimumHorizontalDistance, "minimumHorizontalDistance");
        requirePitch(neutralPitchDegrees);
        requireScale(verticalAimScale, "verticalAimScale");
        requirePositive(maxPitchDegrees, "maxPitchDegrees");
        requirePitch(maxPitchDegrees);
    }

    public static CameraTargetSettings standard() {
        return new CameraTargetSettings(3.5, 0.35, 0.0, 0.65, 18.0);
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }

    private static void requirePitch(double value) {
        if (!Double.isFinite(value) || Math.abs(value) > 90.0) {
            throw new IllegalArgumentException("pitch must be between -90 and 90.");
        }
    }

    private static void requireScale(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1.");
        }
    }
}

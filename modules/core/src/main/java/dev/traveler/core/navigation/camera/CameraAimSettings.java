package dev.traveler.core.navigation.camera;

public record CameraAimSettings(
        double maxYawDegreesPerSecond,
        double maxPitchDegreesPerSecond,
        double response,
        double deadzoneDegrees) {
    public CameraAimSettings {
        requirePositive(maxYawDegreesPerSecond, "maxYawDegreesPerSecond");
        requirePositive(maxPitchDegreesPerSecond, "maxPitchDegreesPerSecond");
        requirePositive(response, "response");
        if (!Double.isFinite(deadzoneDegrees) || deadzoneDegrees < 0.0) {
            throw new IllegalArgumentException("deadzoneDegrees must be non-negative.");
        }
    }

    public static CameraAimSettings standard() {
        return new CameraAimSettings(540.0, 240.0, 18.0, 0.05);
    }

    public static CameraAimSettings preview() {
        return new CameraAimSettings(300.0, 160.0, 10.0, 0.05);
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}

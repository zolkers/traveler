package dev.traveler.core.navigation.camera;

import dev.traveler.core.settings.TravelerSettings;

public record CameraAimSettings(
        double maxYawDegreesPerSecond,
        double maxPitchDegreesPerSecond,
        double response,
        double deadzoneDegrees,
        double maxYawStepDegrees) {
    public CameraAimSettings {
        requirePositive(maxYawDegreesPerSecond, "maxYawDegreesPerSecond");
        requirePositive(maxPitchDegreesPerSecond, "maxPitchDegreesPerSecond");
        requirePositive(response, "response");
        requirePositive(maxYawStepDegrees, "maxYawStepDegrees");
        if (!Double.isFinite(deadzoneDegrees) || deadzoneDegrees < 0.0) {
            throw new IllegalArgumentException("deadzoneDegrees must be non-negative.");
        }
    }

    public CameraAimSettings(
            double maxYawDegreesPerSecond,
            double maxPitchDegreesPerSecond,
            double response,
            double deadzoneDegrees) {
        this(
                maxYawDegreesPerSecond,
                maxPitchDegreesPerSecond,
                response,
                deadzoneDegrees,
                Math.max(6.0, maxYawDegreesPerSecond * 0.025));
    }

    public static CameraAimSettings standard() {
        return TravelerSettings.standard().cameraAimSettings();
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

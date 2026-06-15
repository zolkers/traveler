package dev.traveler.core.navigation.steering;

import dev.traveler.core.settings.TravelerSettings;

public record PathSteeringSettings(
        double pathOffset,
        double predictionSeconds,
        double corridorRadius,
        double lateralCorrectionGain,
        double maxCorrectionDistance,
        double clearanceWarningLateralError) {
    public PathSteeringSettings(
            double pathOffset,
            double predictionSeconds,
            double corridorRadius,
            double lateralCorrectionGain,
            double maxCorrectionDistance) {
        this(pathOffset, predictionSeconds, corridorRadius, lateralCorrectionGain, maxCorrectionDistance, 0.65);
    }

    public PathSteeringSettings {
        requireNonNegative(pathOffset, "pathOffset");
        requireNonNegative(predictionSeconds, "predictionSeconds");
        requireNonNegative(corridorRadius, "corridorRadius");
        requireNonNegative(lateralCorrectionGain, "lateralCorrectionGain");
        requireNonNegative(maxCorrectionDistance, "maxCorrectionDistance");
        requireNonNegative(clearanceWarningLateralError, "clearanceWarningLateralError");
    }

    public static PathSteeringSettings standard() {
        return TravelerSettings.standard().pathSteeringSettings();
    }

    public static PathSteeringSettings standard(double pathOffset) {
        TravelerSettings settings = TravelerSettings.standard();
        return new PathSteeringSettings(
                pathOffset,
                settings.get(TravelerSettings.STEERING_PREDICTION_SECONDS),
                settings.get(TravelerSettings.STEERING_CORRIDOR_RADIUS),
                settings.get(TravelerSettings.STEERING_LATERAL_CORRECTION_GAIN),
                settings.get(TravelerSettings.STEERING_MAX_CORRECTION_DISTANCE),
                settings.get(TravelerSettings.STEERING_CLEARANCE_WARNING_LATERAL_ERROR));
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }
}

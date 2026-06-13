package dev.traveler.core.navigation.steering;

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
        return standard(2.0);
    }

    public static PathSteeringSettings standard(double pathOffset) {
        return new PathSteeringSettings(pathOffset, 0.25, 0.35, 1.0, 0.75, 0.65);
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }
}

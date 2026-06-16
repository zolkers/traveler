package dev.traveler.core.navigation.steering;

import dev.traveler.core.settings.TravelerSettings;

public record PathSteeringSettings(
        double pathOffset,
        double predictionSeconds,
        double corridorRadius,
        double lateralCorrectionGain,
        double maxCorrectionDistance,
        double clearanceWarningLateralError,
        double lateralCorrectionDeadband,
        double lateralCorrectionDerivativeGain,
        double minimumPathOffset,
        double lateralErrorLookaheadReductionGain,
        double actionApproachPathOffset) {
    public PathSteeringSettings(
            double pathOffset,
            double predictionSeconds,
            double corridorRadius,
            double lateralCorrectionGain,
            double maxCorrectionDistance) {
        this(pathOffset, predictionSeconds, corridorRadius, lateralCorrectionGain, maxCorrectionDistance, 0.65);
    }

    public PathSteeringSettings(
            double pathOffset,
            double predictionSeconds,
            double corridorRadius,
            double lateralCorrectionGain,
            double maxCorrectionDistance,
            double clearanceWarningLateralError) {
        this(
                pathOffset,
                predictionSeconds,
                corridorRadius,
                lateralCorrectionGain,
                maxCorrectionDistance,
                clearanceWarningLateralError,
                0.08);
    }

    public PathSteeringSettings(
            double pathOffset,
            double predictionSeconds,
            double corridorRadius,
            double lateralCorrectionGain,
            double maxCorrectionDistance,
            double clearanceWarningLateralError,
            double lateralCorrectionDeadband) {
        this(
                pathOffset,
                predictionSeconds,
                corridorRadius,
                lateralCorrectionGain,
                maxCorrectionDistance,
                clearanceWarningLateralError,
                lateralCorrectionDeadband,
                0.0,
                pathOffset,
                0.0,
                pathOffset);
    }

    public PathSteeringSettings {
        requireNonNegative(pathOffset, "pathOffset");
        requireNonNegative(predictionSeconds, "predictionSeconds");
        requireNonNegative(corridorRadius, "corridorRadius");
        requireNonNegative(lateralCorrectionGain, "lateralCorrectionGain");
        requireNonNegative(maxCorrectionDistance, "maxCorrectionDistance");
        requireNonNegative(clearanceWarningLateralError, "clearanceWarningLateralError");
        requireNonNegative(lateralCorrectionDeadband, "lateralCorrectionDeadband");
        requireNonNegative(lateralCorrectionDerivativeGain, "lateralCorrectionDerivativeGain");
        requireNonNegative(minimumPathOffset, "minimumPathOffset");
        requireNonNegative(lateralErrorLookaheadReductionGain, "lateralErrorLookaheadReductionGain");
        requireNonNegative(actionApproachPathOffset, "actionApproachPathOffset");
        if (minimumPathOffset > pathOffset) {
            throw new IllegalArgumentException("minimumPathOffset must not exceed pathOffset.");
        }
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
                settings.get(TravelerSettings.STEERING_CLEARANCE_WARNING_LATERAL_ERROR),
                settings.get(TravelerSettings.STEERING_LATERAL_CORRECTION_DEADBAND),
                settings.get(TravelerSettings.STEERING_LATERAL_CORRECTION_DERIVATIVE_GAIN),
                Math.min(settings.get(TravelerSettings.STEERING_MINIMUM_PATH_OFFSET), pathOffset),
                settings.get(TravelerSettings.STEERING_LATERAL_ERROR_LOOKAHEAD_REDUCTION_GAIN),
                Math.min(settings.get(TravelerSettings.STEERING_ACTION_APPROACH_PATH_OFFSET), pathOffset));
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }
}

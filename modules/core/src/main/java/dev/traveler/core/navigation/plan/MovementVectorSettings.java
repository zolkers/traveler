package dev.traveler.core.navigation.plan;

import dev.traveler.core.settings.TravelerSettings;

public record MovementVectorSettings(
        double pressThreshold,
        double centeringCorrectionThreshold,
        double turnStrafeThreshold,
        double forwardArcMinimumForward,
        double backpedalMaximumDistance,
        double specialActionLateralTolerance) {
    public MovementVectorSettings {
        requireThreshold(pressThreshold, "pressThreshold");
        requireThreshold(turnStrafeThreshold, "turnStrafeThreshold");
        requireThreshold(forwardArcMinimumForward, "forwardArcMinimumForward");
        if (!Double.isFinite(centeringCorrectionThreshold) || centeringCorrectionThreshold < 0.0) {
            throw new IllegalArgumentException("centeringCorrectionThreshold must be non-negative.");
        }
        if (!Double.isFinite(backpedalMaximumDistance) || backpedalMaximumDistance < 0.0) {
            throw new IllegalArgumentException("backpedalMaximumDistance must be non-negative.");
        }
        if (!Double.isFinite(specialActionLateralTolerance) || specialActionLateralTolerance < 0.0) {
            throw new IllegalArgumentException("specialActionLateralTolerance must be non-negative.");
        }
    }

    public static MovementVectorSettings standard() {
        return TravelerSettings.standard().movementVectorSettings();
    }

    private static void requireThreshold(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1.");
        }
    }
}

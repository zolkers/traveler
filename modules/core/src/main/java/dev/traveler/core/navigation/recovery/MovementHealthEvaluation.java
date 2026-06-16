package dev.traveler.core.navigation.recovery;

import java.util.Objects;

public record MovementHealthEvaluation(
        MovementHealthPhase phase,
        double progressValue,
        double targetDistance,
        double lateralDistance,
        double allowedLateralDistance) {
    public MovementHealthEvaluation {
        Objects.requireNonNull(phase, "phase");
        requireFinite(progressValue, "progressValue");
        requireFinite(targetDistance, "targetDistance");
        requireNonNegative(lateralDistance, "lateralDistance");
        requireNonNegative(allowedLateralDistance, "allowedLateralDistance");
    }

    public static MovementHealthEvaluation idle() {
        return new MovementHealthEvaluation(MovementHealthPhase.IDLE, 0.0, 0.0, 0.0, 0.0);
    }

    public static MovementHealthEvaluation setup() {
        return new MovementHealthEvaluation(MovementHealthPhase.SETUP, 0.0, 0.0, 0.0, 0.0);
    }

    public static MovementHealthEvaluation setup(double lateralDistance, double allowedLateralDistance) {
        return new MovementHealthEvaluation(
                MovementHealthPhase.SETUP,
                0.0,
                0.0,
                lateralDistance,
                allowedLateralDistance);
    }

    public static MovementHealthEvaluation committed(double allowedLateralDistance) {
        return new MovementHealthEvaluation(
                MovementHealthPhase.COMMITTED,
                0.0,
                0.0,
                0.0,
                allowedLateralDistance);
    }

    public static MovementHealthEvaluation active(
            double progressValue,
            double targetDistance,
            double lateralDistance,
            double allowedLateralDistance) {
        return new MovementHealthEvaluation(
                MovementHealthPhase.ACTIVE,
                progressValue,
                targetDistance,
                lateralDistance,
                allowedLateralDistance);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative and finite.");
        }
    }
}

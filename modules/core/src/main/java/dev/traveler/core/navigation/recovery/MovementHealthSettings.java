package dev.traveler.core.navigation.recovery;

public record MovementHealthSettings(
        double minimumProgressDistance,
        double stuckAfterSeconds,
        double recoveryCooldownSeconds,
        double pathDivergenceDistance,
        double pathDivergenceAfterSeconds,
        double actionSetupTimeoutSeconds,
        double jumpGraceSeconds,
        int stuckAfterTicks,
        int pathDivergenceAfterTicks,
        int actionSetupTimeoutTicks,
        int jumpGraceTicks) {
    public MovementHealthSettings(
            double minimumProgressDistance,
            double stuckAfterSeconds,
            double recoveryCooldownSeconds) {
        this(
                minimumProgressDistance,
                stuckAfterSeconds,
                recoveryCooldownSeconds,
                1.2,
                0.75,
                2.0,
                0.35);
    }

    public MovementHealthSettings(
            double minimumProgressDistance,
            double stuckAfterSeconds,
            double recoveryCooldownSeconds,
            double pathDivergenceDistance,
            double pathDivergenceAfterSeconds,
            double actionSetupTimeoutSeconds,
            double jumpGraceSeconds) {
        this(
                minimumProgressDistance,
                stuckAfterSeconds,
                recoveryCooldownSeconds,
                pathDivergenceDistance,
                pathDivergenceAfterSeconds,
                actionSetupTimeoutSeconds,
                jumpGraceSeconds,
                ticksForSeconds(stuckAfterSeconds),
                ticksForSeconds(pathDivergenceAfterSeconds),
                ticksForSeconds(actionSetupTimeoutSeconds),
                ticksForSeconds(jumpGraceSeconds));
    }

    public MovementHealthSettings {
        requireNonNegative(minimumProgressDistance, "minimumProgressDistance");
        requirePositive(stuckAfterSeconds, "stuckAfterSeconds");
        requireNonNegative(recoveryCooldownSeconds, "recoveryCooldownSeconds");
        requireNonNegative(pathDivergenceDistance, "pathDivergenceDistance");
        requirePositive(pathDivergenceAfterSeconds, "pathDivergenceAfterSeconds");
        requirePositive(actionSetupTimeoutSeconds, "actionSetupTimeoutSeconds");
        requireNonNegative(jumpGraceSeconds, "jumpGraceSeconds");
        requirePositive(stuckAfterTicks, "stuckAfterTicks");
        requirePositive(pathDivergenceAfterTicks, "pathDivergenceAfterTicks");
        requirePositive(actionSetupTimeoutTicks, "actionSetupTimeoutTicks");
        requireNonNegative(jumpGraceTicks, "jumpGraceTicks");
    }

    public static MovementHealthSettings standard() {
        return new MovementHealthSettings(0.05, 1.5, 1.0, 1.2, 0.75, 2.0, 0.35);
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive and finite.");
        }
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }

    private static int ticksForSeconds(double seconds) {
        requireNonNegative(seconds, "seconds");
        return Math.max(1, (int) Math.ceil(seconds * 20.0));
    }
}

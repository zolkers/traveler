package dev.traveler.core.navigation.recovery;

public record MovementHealthSettings(
        double minimumProgressDistance,
        double stuckAfterSeconds,
        double recoveryCooldownSeconds) {
    public MovementHealthSettings {
        requireNonNegative(minimumProgressDistance, "minimumProgressDistance");
        requirePositive(stuckAfterSeconds, "stuckAfterSeconds");
        requireNonNegative(recoveryCooldownSeconds, "recoveryCooldownSeconds");
    }

    public static MovementHealthSettings standard() {
        return new MovementHealthSettings(0.05, 1.5, 1.0);
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
}

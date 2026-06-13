package dev.traveler.core.navigation.plan;

public record MovementVectorSettings(
        double pressThreshold,
        double centeringCorrectionThreshold,
        double turnStrafeThreshold,
        double backpedalMaximumDistance) {
    public MovementVectorSettings {
        requireThreshold(pressThreshold, "pressThreshold");
        requireThreshold(turnStrafeThreshold, "turnStrafeThreshold");
        if (!Double.isFinite(centeringCorrectionThreshold) || centeringCorrectionThreshold < 0.0) {
            throw new IllegalArgumentException("centeringCorrectionThreshold must be non-negative.");
        }
        if (!Double.isFinite(backpedalMaximumDistance) || backpedalMaximumDistance < 0.0) {
            throw new IllegalArgumentException("backpedalMaximumDistance must be non-negative.");
        }
    }

    public static MovementVectorSettings standard() {
        return new MovementVectorSettings(0.32, 0.5, 0.32, 0.8);
    }

    private static void requireThreshold(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1.");
        }
    }
}

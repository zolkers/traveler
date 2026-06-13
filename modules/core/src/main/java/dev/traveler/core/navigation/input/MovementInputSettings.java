package dev.traveler.core.navigation.input;

public record MovementInputSettings(
        double pressThreshold,
        double releaseThreshold,
        double centeringCorrectionThreshold,
        double turnStrafeThreshold,
        double backpedalMaximumDistance) {
    public MovementInputSettings(double pressThreshold, double releaseThreshold) {
        this(pressThreshold, releaseThreshold, 1.0);
    }

    public MovementInputSettings(
            double pressThreshold,
            double releaseThreshold,
            double centeringCorrectionThreshold) {
        this(pressThreshold, releaseThreshold, centeringCorrectionThreshold, pressThreshold, 0.8);
    }

    public MovementInputSettings {
        requireThreshold(pressThreshold, "pressThreshold");
        requireThreshold(releaseThreshold, "releaseThreshold");
        requireThreshold(turnStrafeThreshold, "turnStrafeThreshold");
        if (releaseThreshold > pressThreshold) {
            throw new IllegalArgumentException("Release threshold cannot be above press threshold.");
        }
        if (!Double.isFinite(centeringCorrectionThreshold) || centeringCorrectionThreshold < 0.0) {
            throw new IllegalArgumentException("centeringCorrectionThreshold must be non-negative.");
        }
        if (!Double.isFinite(backpedalMaximumDistance) || backpedalMaximumDistance < 0.0) {
            throw new IllegalArgumentException("backpedalMaximumDistance must be non-negative.");
        }
    }

    public static MovementInputSettings standard() {
        return new MovementInputSettings(0.32, 0.18, 1.0, 0.32, 0.8);
    }

    private static void requireThreshold(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1.");
        }
    }
}

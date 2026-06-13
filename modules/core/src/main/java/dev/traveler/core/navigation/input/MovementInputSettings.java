package dev.traveler.core.navigation.input;

public record MovementInputSettings(
        double pressThreshold,
        double releaseThreshold,
        double centeringCorrectionThreshold) {
    public MovementInputSettings(double pressThreshold, double releaseThreshold) {
        this(pressThreshold, releaseThreshold, 1.0);
    }

    public MovementInputSettings {
        requireThreshold(pressThreshold, "pressThreshold");
        requireThreshold(releaseThreshold, "releaseThreshold");
        if (releaseThreshold > pressThreshold) {
            throw new IllegalArgumentException("Release threshold cannot be above press threshold.");
        }
        if (!Double.isFinite(centeringCorrectionThreshold) || centeringCorrectionThreshold < 0.0) {
            throw new IllegalArgumentException("centeringCorrectionThreshold must be non-negative.");
        }
    }

    public static MovementInputSettings standard() {
        return new MovementInputSettings(0.32, 0.18, 1.0);
    }

    private static void requireThreshold(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1.");
        }
    }
}

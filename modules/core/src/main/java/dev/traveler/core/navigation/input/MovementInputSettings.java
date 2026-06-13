package dev.traveler.core.navigation.input;

public record MovementInputSettings(double pressThreshold, double releaseThreshold) {
    public MovementInputSettings {
        requireThreshold(pressThreshold, "pressThreshold");
        requireThreshold(releaseThreshold, "releaseThreshold");
        if (releaseThreshold > pressThreshold) {
            throw new IllegalArgumentException("Release threshold cannot be above press threshold.");
        }
    }

    public static MovementInputSettings standard() {
        return new MovementInputSettings(0.32, 0.18);
    }

    private static void requireThreshold(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1.");
        }
    }
}

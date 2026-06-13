package dev.traveler.core.navigation.plan;

public record SpeedIntent(double scale, boolean sprintRequested) {
    public SpeedIntent {
        if (!Double.isFinite(scale) || scale < 0.0 || scale > 1.0) {
            throw new IllegalArgumentException("scale must be between 0 and 1.");
        }
    }

    public static SpeedIntent stop() {
        return new SpeedIntent(0.0, false);
    }
}

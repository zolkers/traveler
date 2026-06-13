package dev.traveler.core.navigation.plan;

public record ToleranceProfile(double reachedDistance) {
    public ToleranceProfile {
        if (!Double.isFinite(reachedDistance) || reachedDistance < 0.0) {
            throw new IllegalArgumentException("reachedDistance must be non-negative.");
        }
    }

    public static ToleranceProfile standard() {
        return new ToleranceProfile(0.35);
    }
}

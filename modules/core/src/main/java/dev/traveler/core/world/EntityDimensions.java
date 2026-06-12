package dev.traveler.core.world;

public record EntityDimensions(double width, double height) {
    public EntityDimensions {
        if (!(Double.isFinite(width) && width > 0.0)) {
            throw new IllegalArgumentException("Entity width must be positive and finite.");
        }
        if (!(Double.isFinite(height) && height > 0.0)) {
            throw new IllegalArgumentException("Entity height must be positive and finite.");
        }
    }
}

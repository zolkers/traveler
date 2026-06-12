package dev.traveler.core.world.movement;

public record TraversalCost(double value) {
    public TraversalCost {
        if (!(Double.isFinite(value) && value >= 0.0)) {
            throw new IllegalArgumentException("Traversal cost must be non-negative and finite.");
        }
    }
}

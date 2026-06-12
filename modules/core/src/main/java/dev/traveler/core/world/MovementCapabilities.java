package dev.traveler.core.world;

public record MovementCapabilities(
        boolean canWalk, boolean canSwim, boolean canFly, double stepHeight, double jumpHeight) {
    public MovementCapabilities {
        if (!(canWalk || canSwim || canFly)) {
            throw new IllegalArgumentException("At least one movement mode must be available.");
        }
        if (!(Double.isFinite(stepHeight) && stepHeight >= 0.0)) {
            throw new IllegalArgumentException("Step height must be non-negative and finite.");
        }
        if (!(Double.isFinite(jumpHeight) && jumpHeight >= 0.0)) {
            throw new IllegalArgumentException("Jump height must be non-negative and finite.");
        }
    }
}

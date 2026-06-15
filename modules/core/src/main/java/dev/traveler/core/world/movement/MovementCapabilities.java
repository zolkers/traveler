package dev.traveler.core.world.movement;

public record MovementCapabilities(
        boolean canWalk,
        boolean canSwim,
        boolean canFly,
        boolean canCrouch,
        double maxStepUp,
        double maxJumpHeight,
        double maxSafeFallDistance) {
    public MovementCapabilities {
        if (!(canWalk || canSwim || canFly)) {
            throw new IllegalArgumentException("At least one movement mode must be available.");
        }
        if (!(Double.isFinite(maxStepUp) && maxStepUp >= 0.0)) {
            throw new IllegalArgumentException("Max step up must be non-negative and finite.");
        }
        if (!(Double.isFinite(maxJumpHeight) && maxJumpHeight >= 0.0)) {
            throw new IllegalArgumentException("Max jump height must be non-negative and finite.");
        }
        if (!(Double.isFinite(maxSafeFallDistance) && maxSafeFallDistance >= 0.0)) {
            throw new IllegalArgumentException("Max safe fall distance must be non-negative and finite.");
        }
    }

    public MovementCapabilities withMaxSafeFallDistance(double distance) {
        return new MovementCapabilities(
                canWalk,
                canSwim,
                canFly,
                canCrouch,
                maxStepUp,
                maxJumpHeight,
                distance);
    }

    public MovementCapabilities withCanSwim(boolean enabled) {
        return new MovementCapabilities(
                canWalk,
                enabled,
                canFly,
                canCrouch,
                maxStepUp,
                maxJumpHeight,
                maxSafeFallDistance);
    }
}

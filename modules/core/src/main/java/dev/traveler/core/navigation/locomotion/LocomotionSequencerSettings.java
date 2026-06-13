package dev.traveler.core.navigation.locomotion;

public record LocomotionSequencerSettings(
        int requiredStableGroundFrames,
        double verticalVelocityTolerance,
        int actionHoldFrames) {
    public LocomotionSequencerSettings(
            int requiredStableGroundFrames,
            double verticalVelocityTolerance) {
        this(requiredStableGroundFrames, verticalVelocityTolerance, 4);
    }

    public LocomotionSequencerSettings {
        if (requiredStableGroundFrames < 1) {
            throw new IllegalArgumentException("requiredStableGroundFrames must be positive.");
        }
        if (!Double.isFinite(verticalVelocityTolerance) || verticalVelocityTolerance < 0.0) {
            throw new IllegalArgumentException("verticalVelocityTolerance must be non-negative.");
        }
        if (actionHoldFrames < 0) {
            throw new IllegalArgumentException("actionHoldFrames must be non-negative.");
        }
    }

    public static LocomotionSequencerSettings standard() {
        return new LocomotionSequencerSettings(2, 0.08, 4);
    }
}

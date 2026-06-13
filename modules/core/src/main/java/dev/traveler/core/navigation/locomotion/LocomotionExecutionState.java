package dev.traveler.core.navigation.locomotion;

public record LocomotionExecutionState(boolean settlingAfterAction, int stableGroundFrames) {
    public LocomotionExecutionState {
        if (stableGroundFrames < 0) {
            throw new IllegalArgumentException("stableGroundFrames must be non-negative.");
        }
    }

    public static LocomotionExecutionState start() {
        return new LocomotionExecutionState(false, 0);
    }

    public static LocomotionExecutionState settling() {
        return new LocomotionExecutionState(true, 0);
    }
}

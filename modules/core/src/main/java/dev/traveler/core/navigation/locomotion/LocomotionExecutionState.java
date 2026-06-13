package dev.traveler.core.navigation.locomotion;

public record LocomotionExecutionState(
        boolean settlingAfterAction,
        int stableGroundFrames,
        int actionHoldFrames) {
    public LocomotionExecutionState(boolean settlingAfterAction, int stableGroundFrames) {
        this(settlingAfterAction, stableGroundFrames, 0);
    }

    public LocomotionExecutionState {
        if (stableGroundFrames < 0) {
            throw new IllegalArgumentException("stableGroundFrames must be non-negative.");
        }
        if (actionHoldFrames < 0) {
            throw new IllegalArgumentException("actionHoldFrames must be non-negative.");
        }
    }

    public static LocomotionExecutionState start() {
        return new LocomotionExecutionState(false, 0, 0);
    }

    public static LocomotionExecutionState settling() {
        return settling(0);
    }

    public static LocomotionExecutionState settling(int actionHoldFrames) {
        return new LocomotionExecutionState(true, 0, actionHoldFrames);
    }

    public LocomotionExecutionState decrementActionHold() {
        return new LocomotionExecutionState(
                settlingAfterAction,
                stableGroundFrames,
                Math.max(0, actionHoldFrames - 1));
    }

    public LocomotionExecutionState withoutActionHold() {
        return new LocomotionExecutionState(settlingAfterAction, stableGroundFrames, 0);
    }
}

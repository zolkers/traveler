package dev.traveler.core.navigation.locomotion;

import java.util.Objects;

public record LocomotionExecutionState(
        boolean settlingAfterAction,
        int stableGroundFrames,
        int actionHoldFrames,
        LocomotionAction heldAction) {
    public LocomotionExecutionState(boolean settlingAfterAction, int stableGroundFrames) {
        this(settlingAfterAction, stableGroundFrames, 0, LocomotionAction.WALK);
    }

    public LocomotionExecutionState(
            boolean settlingAfterAction,
            int stableGroundFrames,
            int actionHoldFrames) {
        this(settlingAfterAction, stableGroundFrames, actionHoldFrames, LocomotionAction.WALK);
    }

    public LocomotionExecutionState {
        Objects.requireNonNull(heldAction, "heldAction");
        if (stableGroundFrames < 0) {
            throw new IllegalArgumentException("stableGroundFrames must be non-negative.");
        }
        if (actionHoldFrames < 0) {
            throw new IllegalArgumentException("actionHoldFrames must be non-negative.");
        }
    }

    public static LocomotionExecutionState start() {
        return new LocomotionExecutionState(false, 0, 0, LocomotionAction.WALK);
    }

    public static LocomotionExecutionState settling() {
        return settling(0, LocomotionAction.WALK);
    }

    public static LocomotionExecutionState settling(int actionHoldFrames) {
        return settling(actionHoldFrames, LocomotionAction.WALK);
    }

    public static LocomotionExecutionState settling(
            int actionHoldFrames,
            LocomotionAction heldAction) {
        return new LocomotionExecutionState(true, 0, actionHoldFrames, heldAction);
    }

    public LocomotionExecutionState decrementActionHold() {
        return new LocomotionExecutionState(
                settlingAfterAction,
                stableGroundFrames,
                Math.max(0, actionHoldFrames - 1),
                heldAction);
    }

    public LocomotionExecutionState withoutActionHold() {
        return new LocomotionExecutionState(
                settlingAfterAction,
                stableGroundFrames,
                0,
                LocomotionAction.WALK);
    }
}

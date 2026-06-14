package dev.traveler.core.navigation.locomotion;

import dev.traveler.core.navigation.control.MovementIntent;
import java.util.Objects;

public final class LocomotionSequencer {
    private final LocomotionSequencerSettings settings;

    public LocomotionSequencer(LocomotionSequencerSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public static LocomotionSequencer standard() {
        return new LocomotionSequencer(LocomotionSequencerSettings.standard());
    }

    public LocomotionDecision update(
            LocomotionExecutionState state,
            LocomotionPlan requestedPlan,
            AgentMotionState motionState,
            MovementIntent previousIntent) {
        LocomotionExecutionState currentState = Objects.requireNonNull(state, "state");
        LocomotionPlan requested = Objects.requireNonNull(requestedPlan, "requestedPlan");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        MovementIntent previous = Objects.requireNonNull(previousIntent, "previousIntent");
        if (requested.action() == LocomotionAction.RECOVER) {
            return new LocomotionDecision(requested, LocomotionExecutionState.settling());
        }
        if (shouldHoldAction(currentState)) {
            return new LocomotionDecision(
                    LocomotionPlan.fromAction(currentState.heldAction()),
                    currentState.decrementActionHold());
        }
        LocomotionExecutionState advanced = advance(currentState, motion, previous);
        if (isSpecial(requested) && advanced.settlingAfterAction()) {
            return new LocomotionDecision(LocomotionPlan.walk(), advanced);
        }
        if (isSpecial(requested)) {
            return new LocomotionDecision(
                    requested,
                    LocomotionExecutionState.settling(settings.actionHoldFrames(), requested.action()));
        }
        return new LocomotionDecision(requested, advanced);
    }

    private static boolean shouldHoldAction(LocomotionExecutionState state) {
        return state.heldAction() == LocomotionAction.JUMP
                && state.actionHoldFrames() > 0;
    }

    private LocomotionExecutionState advance(
            LocomotionExecutionState state,
            AgentMotionState motion,
            MovementIntent previous) {
        if (!state.settlingAfterAction()) {
            return state;
        }
        int stableFrames = stableFrames(state, motion, previous);
        if (stableFrames >= settings.requiredStableGroundFrames()) {
            return LocomotionExecutionState.start();
        }
        return new LocomotionExecutionState(true, stableFrames, 0, LocomotionAction.WALK);
    }

    private int stableFrames(
            LocomotionExecutionState state,
            AgentMotionState motion,
            MovementIntent previous) {
        if (!isStableGroundContact(motion, previous)) {
            return 0;
        }
        return Math.min(settings.requiredStableGroundFrames(), state.stableGroundFrames() + 1);
    }

    private boolean isStableGroundContact(AgentMotionState motion, MovementIntent previous) {
        if (!motion.onGround() || previous.jump()) {
            return false;
        }
        return Math.abs(motion.verticalVelocity()) <= settings.verticalVelocityTolerance();
    }

    private static boolean isSpecial(LocomotionPlan plan) {
        return isSpecial(plan.action());
    }

    private static boolean isSpecial(LocomotionAction action) {
        return action == LocomotionAction.JUMP
                || action == LocomotionAction.STEP_UP
                || action == LocomotionAction.DROP;
    }
}

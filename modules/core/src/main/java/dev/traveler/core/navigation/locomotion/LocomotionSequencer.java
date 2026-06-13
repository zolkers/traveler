package dev.traveler.core.navigation.locomotion;

import dev.traveler.core.navigation.input.MovementIntent;
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
        LocomotionExecutionState advanced = advance(currentState, motion, previous);
        if (isSpecial(requested) && advanced.settlingAfterAction()) {
            return new LocomotionDecision(LocomotionPlan.walk(), advanced);
        }
        if (isSpecial(requested)) {
            return new LocomotionDecision(requested, LocomotionExecutionState.settling());
        }
        return new LocomotionDecision(requested, advanced);
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
        return new LocomotionExecutionState(true, stableFrames);
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
        LocomotionAction action = plan.action();
        return action == LocomotionAction.JUMP
                || action == LocomotionAction.STEP_UP
                || action == LocomotionAction.DROP;
    }
}

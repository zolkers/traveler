package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionDecision;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.locomotion.LocomotionSequencer;
import java.util.Objects;

public final class ActionTimingPolicy {
    private final LocomotionSequencer sequencer;

    public ActionTimingPolicy(LocomotionSequencer sequencer) {
        this.sequencer = Objects.requireNonNull(sequencer, "sequencer");
    }

    public static ActionTimingPolicy standard() {
        return new ActionTimingPolicy(LocomotionSequencer.standard());
    }

    public LocomotionDecision decide(
            NavigationControllerState state,
            PathProgress progress,
            LocomotionPlan requestedPlan,
            AgentMotionState motionState) {
        NavigationControllerState controllerState = Objects.requireNonNull(state, "state");
        PathProgress routeProgress = Objects.requireNonNull(progress, "progress");
        LocomotionPlan requested = Objects.requireNonNull(requestedPlan, "requestedPlan");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        return sequencer.update(
                locomotionState(controllerState, routeProgress),
                requested,
                motion,
                controllerState.previousIntent());
    }

    private static LocomotionExecutionState locomotionState(
            NavigationControllerState state,
            PathProgress progress) {
        if (state.progress().equals(progress)) {
            return state.locomotionState();
        }
        return state.locomotionState().withoutActionHold();
    }
}

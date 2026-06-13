package dev.traveler.core.navigation.locomotion;

import java.util.Objects;

public record LocomotionDecision(LocomotionPlan plan, LocomotionExecutionState state) {
    public LocomotionDecision {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(state, "state");
    }
}

package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import java.util.Objects;

public record MovementActionDecision(
        LocomotionPlan locomotionPlan,
        boolean retainActionTarget) {
    public MovementActionDecision {
        Objects.requireNonNull(locomotionPlan, "locomotionPlan");
    }
}

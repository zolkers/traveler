package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Objects;

public record ProgressSample(
        double routeProgress,
        double lateralDistance,
        double actionTargetDistance,
        MovementAction action,
        NavigationPhase phase) {
    public ProgressSample {
        requireFinite(routeProgress, "routeProgress");
        requireFinite(lateralDistance, "lateralDistance");
        requireFinite(actionTargetDistance, "actionTargetDistance");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(phase, "phase");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}

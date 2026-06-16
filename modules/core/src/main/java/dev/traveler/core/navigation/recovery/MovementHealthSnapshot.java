package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Objects;

public record MovementHealthSnapshot(
        int nextNodeIndex,
        WorldPoint position,
        WorldPoint segmentStart,
        WorldPoint segmentEnd,
        WorldPoint actionTarget,
        MovementAction action,
        NavigationPhase phase,
        LocomotionAction locomotionAction,
        LocomotionExecutionState locomotionState,
        boolean moving,
        boolean onGround,
        boolean horizontalCollision,
        double horizontalSpeed,
        double verticalVelocity,
        double routeProgress,
        double lateralDistance,
        double actionTargetDistance) {
    public MovementHealthSnapshot {
        if (nextNodeIndex < 1) {
            throw new IllegalArgumentException("nextNodeIndex must be positive.");
        }
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(segmentStart, "segmentStart");
        Objects.requireNonNull(segmentEnd, "segmentEnd");
        Objects.requireNonNull(actionTarget, "actionTarget");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(locomotionAction, "locomotionAction");
        Objects.requireNonNull(locomotionState, "locomotionState");
        requireFinite(horizontalSpeed, "horizontalSpeed");
        requireFinite(verticalVelocity, "verticalVelocity");
        requireFinite(routeProgress, "routeProgress");
        requireNonNegative(lateralDistance, "lateralDistance");
        requireNonNegative(actionTargetDistance, "actionTargetDistance");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative and finite.");
        }
    }
}

package dev.traveler.core.navigation.internal;

import dev.traveler.core.navigation.api.RecoveryAction;
import dev.traveler.core.navigation.api.TraversalFailure;
import dev.traveler.core.navigation.recovery.MovementFailure;
import dev.traveler.core.navigation.recovery.MovementFailureKind;
import java.util.Objects;

public final class RecoveryPlanner {
    public RecoveryAction plan(MovementFailure failure) {
        return plan(traversalFailure(failure));
    }

    public RecoveryAction plan(TraversalFailure failure) {
        return switch (Objects.requireNonNull(failure, "failure").kind()) {
            case NO_PROGRESS, PATH_DIVERGENCE -> RecoveryAction.REPLAN_SEGMENT;
            case ACTION_SETUP_TIMEOUT -> RecoveryAction.MICRO_REPAIR;
            case TRAVERSAL_ABORTED, SEGMENT_STITCH_FAILURE -> RecoveryAction.REBUILD_TRAVERSAL;
            case WORLD_STATE_INVALIDATED, UNLOADED_FRONTIER -> RecoveryAction.REPLAN_ROUTE;
        };
    }

    public TraversalFailure traversalFailure(MovementFailure failure) {
        MovementFailure movementFailure = Objects.requireNonNull(failure, "failure");
        return new TraversalFailure(kind(movementFailure.kind()), movementFailure.message());
    }

    private static TraversalFailure.Kind kind(MovementFailureKind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case STUCK_NO_PROGRESS -> TraversalFailure.Kind.NO_PROGRESS;
            case PATH_DIVERGENCE -> TraversalFailure.Kind.PATH_DIVERGENCE;
            case ACTION_SETUP_TIMEOUT -> TraversalFailure.Kind.ACTION_SETUP_TIMEOUT;
            case OFF_ROUTE_BUT_RECOVERABLE -> TraversalFailure.Kind.TRAVERSAL_ABORTED;
        };
    }
}

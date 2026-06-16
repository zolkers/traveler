package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.locomotion.JumpTraversalRules;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.plan.NavigationPhase;

public final class JumpMovementHealthPolicy implements MovementHealthPolicy {
    private static final double DIVERGENCE_MULTIPLIER = 1.25;

    @Override
    public MovementHealthEvaluation evaluate(MovementHealthSnapshot snapshot, MovementHealthSettings settings) {
        if (snapshot.phase() == NavigationPhase.RECOVER) {
            return MovementHealthEvaluation.idle();
        }
        if (snapshot.phase() == NavigationPhase.ALIGN) {
            return MovementHealthEvaluation.setup(
                    snapshot.lateralDistance(),
                    settings.pathDivergenceDistance() * DIVERGENCE_MULTIPLIER);
        }
        if (snapshot.onGround()
                && !JumpTraversalRules.hasLeftTakeoffZone(
                        snapshot.position(),
                        snapshot.segmentStart())) {
            return MovementHealthEvaluation.setup(
                    snapshot.lateralDistance(),
                    settings.pathDivergenceDistance() * DIVERGENCE_MULTIPLIER);
        }
        if (isCommitted(snapshot, settings)) {
            return MovementHealthEvaluation.committed(settings.pathDivergenceDistance() * DIVERGENCE_MULTIPLIER);
        }
        return MovementHealthEvaluation.active(
                snapshot.routeProgress(),
                snapshot.actionTargetDistance(),
                snapshot.lateralDistance(),
                settings.pathDivergenceDistance() * DIVERGENCE_MULTIPLIER);
    }

    private static boolean isCommitted(MovementHealthSnapshot snapshot, MovementHealthSettings settings) {
        if (!snapshot.onGround()) {
            return true;
        }
        if (!JumpTraversalRules.hasReachedLandingHeight(snapshot.position(), snapshot.actionTarget())) {
            return true;
        }
        if (snapshot.locomotionAction() == LocomotionAction.JUMP) {
            return true;
        }
        return snapshot.locomotionState().settlingAfterAction()
                && snapshot.locomotionState().heldAction() == LocomotionAction.JUMP
                && snapshot.actionTargetDistance() > settings.minimumProgressDistance();
    }
}

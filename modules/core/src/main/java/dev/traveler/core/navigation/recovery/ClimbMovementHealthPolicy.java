package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.plan.NavigationPhase;

public final class ClimbMovementHealthPolicy implements MovementHealthPolicy {
    private static final double DIVERGENCE_MULTIPLIER = 0.75;

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
        return MovementHealthEvaluation.active(
                snapshot.routeProgress(),
                snapshot.actionTargetDistance(),
                snapshot.lateralDistance(),
                settings.pathDivergenceDistance() * DIVERGENCE_MULTIPLIER);
    }
}

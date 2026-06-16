package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.plan.NavigationPhase;

public final class WalkMovementHealthPolicy implements MovementHealthPolicy {
    @Override
    public MovementHealthEvaluation evaluate(MovementHealthSnapshot snapshot, MovementHealthSettings settings) {
        if (snapshot.phase() == NavigationPhase.RECOVER) {
            return MovementHealthEvaluation.idle();
        }
        if (snapshot.phase() == NavigationPhase.ALIGN) {
            return MovementHealthEvaluation.setup(
                    snapshot.lateralDistance(),
                    settings.pathDivergenceDistance());
        }
        return MovementHealthEvaluation.active(
                snapshot.routeProgress(),
                snapshot.actionTargetDistance(),
                snapshot.lateralDistance(),
                settings.pathDivergenceDistance());
    }
}

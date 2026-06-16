package dev.traveler.core.navigation.recovery;

import dev.traveler.core.world.behavior.decision.MovementAction;

public record MovementHealthState(
        int nextNodeIndex,
        MovementAction action,
        Double bestProgressValue,
        Double bestTargetDistance,
        double stagnantSeconds,
        double divergentSeconds,
        double setupSeconds,
        int stagnantTicks,
        int divergentTicks,
        int setupTicks) {
    public static MovementHealthState empty() {
        return new MovementHealthState(0, null, null, null, 0.0, 0.0, 0.0, 0, 0, 0);
    }

    public boolean tracksSameSegment(MovementHealthSnapshot snapshot) {
        return nextNodeIndex == snapshot.nextNodeIndex() && action == snapshot.action();
    }

    public MovementHealthState resetFor(MovementHealthSnapshot snapshot) {
        return new MovementHealthState(snapshot.nextNodeIndex(), snapshot.action(), null, null, 0.0, 0.0, 0.0, 0, 0, 0);
    }
}

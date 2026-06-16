package dev.traveler.core.navigation.follow;

import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Objects;

public record NavigationSegmentIntent(
        MovementAction action,
        WorldPoint actionTarget) {
    public NavigationSegmentIntent {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actionTarget, "actionTarget");
        if (action == MovementAction.BLOCKED) {
            throw new IllegalArgumentException("Navigation segment action must be traversable.");
        }
    }

    public static NavigationSegmentIntent of(
            MovementAction action,
            WorldPoint actionTarget) {
        return new NavigationSegmentIntent(action, actionTarget);
    }
}

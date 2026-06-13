package dev.traveler.core.navigation;

import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.input.MovementIntent;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import java.util.Objects;

public record NavigationControllerState(
        PathProgress progress,
        MovementIntent previousIntent,
        LocomotionExecutionState locomotionState) {
    public NavigationControllerState(PathProgress progress, MovementIntent previousIntent) {
        this(progress, previousIntent, LocomotionExecutionState.start());
    }

    public NavigationControllerState {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(previousIntent, "previousIntent");
        Objects.requireNonNull(locomotionState, "locomotionState");
    }

    public static NavigationControllerState start() {
        return new NavigationControllerState(PathProgress.start(), MovementIntent.idle());
    }
}

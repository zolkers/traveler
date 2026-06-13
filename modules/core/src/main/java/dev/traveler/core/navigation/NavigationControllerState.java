package dev.traveler.core.navigation;

import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.input.MovementIntent;
import java.util.Objects;

public record NavigationControllerState(PathProgress progress, MovementIntent previousIntent) {
    public NavigationControllerState {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(previousIntent, "previousIntent");
    }

    public static NavigationControllerState start() {
        return new NavigationControllerState(PathProgress.start(), MovementIntent.idle());
    }
}

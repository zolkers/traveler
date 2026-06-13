package dev.traveler.core.navigation.follow;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public record MovementTarget(NavigationPoint point, boolean stop) {
    public MovementTarget {
        Objects.requireNonNull(point, "point");
    }

    public static MovementTarget follow(NavigationPoint point) {
        return new MovementTarget(point, false);
    }

    public static MovementTarget stopAt(NavigationPoint point) {
        return new MovementTarget(point, true);
    }
}

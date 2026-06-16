package dev.traveler.core.navigation.follow;

import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;

public record MovementTarget(WorldPoint point, boolean stop) {
    public MovementTarget {
        Objects.requireNonNull(point, "point");
    }

    public static MovementTarget follow(WorldPoint point) {
        return new MovementTarget(point, false);
    }

    public static MovementTarget stopAt(WorldPoint point) {
        return new MovementTarget(point, true);
    }
}

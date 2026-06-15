package dev.traveler.core.world.movement;

import java.util.Objects;

public record TraversalRules(
        boolean allowDiagonal,
        boolean allowVertical,
        TraversalCost defaultCost) {
    public TraversalRules {
        Objects.requireNonNull(defaultCost, "defaultCost");
    }
}

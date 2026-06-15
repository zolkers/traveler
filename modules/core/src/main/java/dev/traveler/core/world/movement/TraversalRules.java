package dev.traveler.core.world.movement;

import java.util.Objects;

public record TraversalRules(
        boolean allowDiagonal,
        boolean allowDiagonalJump,
        boolean allowVertical,
        TraversalCost defaultCost) {
    public TraversalRules {
        Objects.requireNonNull(defaultCost, "defaultCost");
    }

    public TraversalRules(
            boolean allowDiagonal,
            boolean allowVertical,
            TraversalCost defaultCost) {
        this(allowDiagonal, false, allowVertical, defaultCost);
    }
}

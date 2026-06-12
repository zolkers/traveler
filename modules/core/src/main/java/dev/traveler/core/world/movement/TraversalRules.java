package dev.traveler.core.world.movement;

import dev.traveler.core.world.block.BlockPassability;
import java.util.Objects;
import java.util.Set;

public record TraversalRules(
        boolean allowDiagonal,
        boolean allowVertical,
        FluidHandling fluidHandling,
        TraversalCost defaultCost,
        Set<BlockPassability> allowedPassability) {
    public TraversalRules {
        Objects.requireNonNull(fluidHandling, "fluidHandling");
        Objects.requireNonNull(defaultCost, "defaultCost");
        allowedPassability = Set.copyOf(Objects.requireNonNull(allowedPassability, "allowedPassability"));
        if (allowedPassability.isEmpty()) {
            throw new IllegalArgumentException("Allowed passability must not be empty.");
        }
    }
}

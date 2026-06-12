package dev.traveler.core.world;

import java.util.Objects;

public record MovementProfile(EntityDimensions dimensions, MovementCapabilities capabilities, TraversalRules rules) {
    public MovementProfile {
        Objects.requireNonNull(dimensions, "dimensions");
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(rules, "rules");
    }
}

package dev.traveler.core.world.movement;

import java.util.Objects;

public record MovementProfile(EntityDimensions dimensions, MovementCapabilities capabilities, TraversalRules rules) {
    public MovementProfile {
        Objects.requireNonNull(dimensions, "dimensions");
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(rules, "rules");
    }

    public MovementProfile withDimensions(EntityDimensions nextDimensions) {
        return new MovementProfile(nextDimensions, capabilities, rules);
    }

    public MovementProfile withCapabilities(MovementCapabilities nextCapabilities) {
        return new MovementProfile(dimensions, nextCapabilities, rules);
    }

    public MovementProfile withRules(TraversalRules nextRules) {
        return new MovementProfile(dimensions, capabilities, nextRules);
    }
}

package dev.traveler.core.world.behavior.api;

import java.util.Objects;
import java.util.Set;

public record BlockSemantics(
        CollisionSemantics collision,
        SupportSemantics support,
        FluidSemantics fluid,
        Set<TraversalAffordance> affordances,
        Set<BehaviorTag> tags) {
    public BlockSemantics {
        Objects.requireNonNull(collision, "collision");
        Objects.requireNonNull(support, "support");
        Objects.requireNonNull(fluid, "fluid");
        affordances = Set.copyOf(Objects.requireNonNull(affordances, "affordances"));
        tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
    }

    public static BlockSemantics of(
            CollisionSemantics collision,
            SupportSemantics support,
            FluidSemantics fluid,
            Set<TraversalAffordance> affordances) {
        return new BlockSemantics(collision, support, fluid, affordances, Set.of());
    }

    public boolean supports(TraversalAffordance affordance) {
        return affordances.contains(Objects.requireNonNull(affordance, "affordance"));
    }

    public boolean hasTag(BehaviorTag tag) {
        return tags.contains(Objects.requireNonNull(tag, "tag"));
    }
}

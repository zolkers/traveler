package dev.traveler.core.world.movement;

import dev.traveler.core.world.block.BlockPassability;
import java.util.Objects;
import java.util.Set;

public final class MovementProfiles {
    private MovementProfiles() {}

    public static MovementProfile defaultPlayer() {
        return new MovementProfile(defaultPlayerDimensions(), defaultPlayerCapabilities(), defaultPlayerRules());
    }

    public static MovementProfile defaultPlayerWith(MovementCapabilities capabilities) {
        return new MovementProfile(
                defaultPlayerDimensions(),
                Objects.requireNonNull(capabilities, "capabilities"),
                defaultPlayerRules());
    }

    public static EntityDimensions defaultPlayerDimensions() {
        return new EntityDimensions(0.6, 1.8);
    }

    public static MovementCapabilities defaultPlayerCapabilities() {
        return new MovementCapabilities(true, true, false, true, 0.6, 1.25, 3.0);
    }

    public static TraversalRules defaultPlayerRules() {
        return new TraversalRules(
                false,
                true,
                FluidHandling.ALLOW,
                new TraversalCost(1.0),
                Set.of(BlockPassability.WALKABLE, BlockPassability.PASSABLE));
    }
}

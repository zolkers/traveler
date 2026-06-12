package dev.traveler.core.layer;

import dev.traveler.core.world.BlockPassability;
import dev.traveler.core.world.FluidHandling;
import java.util.Objects;

public record BlockClassification(BlockPassability passability, FluidHandling fluidHandling) {
    public BlockClassification {
        Objects.requireNonNull(passability, "passability");
        Objects.requireNonNull(fluidHandling, "fluidHandling");
    }

    public boolean isWalkable() {
        return passability == BlockPassability.WALKABLE || passability == BlockPassability.PASSABLE;
    }
}

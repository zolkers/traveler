package dev.traveler.core.route.api;

import dev.traveler.core.navigation.api.Traversal;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

public record RouteSegment(
        BlockPosition start,
        BlockPosition target,
        Traversal traversal) {
    public RouteSegment {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(traversal, "traversal");
    }
}

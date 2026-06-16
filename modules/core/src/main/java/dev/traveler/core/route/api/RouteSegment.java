package dev.traveler.core.route.api;

import dev.traveler.core.navigation.api.TraversalKind;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

public record RouteSegment(
        int index,
        BlockPosition start,
        BlockPosition end,
        TraversalKind traversalKind) {
    public RouteSegment {
        if (index < 0) {
            throw new IllegalArgumentException("index must be non-negative.");
        }
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(traversalKind, "traversalKind");
    }
}

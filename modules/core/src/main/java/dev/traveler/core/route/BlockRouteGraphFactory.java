package dev.traveler.core.route;

import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.block.BlockPosition;

@FunctionalInterface
public interface BlockRouteGraphFactory {
    Graph<BlockPosition> create(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition goal,
            RouteSearchSettings settings);
}

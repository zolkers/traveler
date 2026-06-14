package dev.traveler.core.route;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.navigation.BlockTraversalGraph;
import java.util.List;
import java.util.Objects;

final class DefaultBlockRouteGraphFactory implements BlockRouteGraphFactory {
    @Override
    public Graph<BlockPosition> create(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition goal,
            RouteSearchSettings settings) {
        if (worldLayer == null) {
            return new DirectBlockGraph(goal);
        }
        return new BlockTraversalGraph(
                worldLayer,
                start,
                goal,
                settings.horizontalMargin(),
                settings.verticalMargin());
    }

    private static final class DirectBlockGraph implements Graph<BlockPosition> {
        private final BlockPosition goal;

        private DirectBlockGraph(BlockPosition goal) {
            this.goal = Objects.requireNonNull(goal, "goal");
        }

        @Override
        public Iterable<Connection<BlockPosition>> outgoingConnections(BlockPosition node) {
            if (node.equals(goal)) {
                return List.of();
            }
            return List.of(new Connection<>(node, goal, RouteSearchService.distance(node, goal)));
        }
    }
}

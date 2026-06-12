package dev.traveler.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockTraversalGraphTest {
    @Test
    void avoidsSolidBlocksInsteadOfConnectingDirectlyToGoal() {
        BlockPosition start = new BlockPosition(0, 64, 0);
        BlockPosition goal = new BlockPosition(2, 64, 0);
        BlockPosition blocked = new BlockPosition(1, 64, 0);
        TestWorldLayer world = new TestWorldLayer(Set.of(blocked, blocked.above()));
        BlockTraversalGraph graph = new BlockTraversalGraph(world, start, goal, 8, 2);

        PathfinderResult<BlockPosition> result = new AStarPathfinder<BlockPosition>()
                .search(new PathfinderRequest<>(graph, start, goal, BlockTraversalGraphTest::distance));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertPathAvoids(result.path(), blocked);
        assertTrue(result.path().nodeCount() > 3);
    }

    @Test
    void exposesOnlyWalkableNeighborConnections() {
        BlockPosition start = new BlockPosition(0, 64, 0);
        BlockPosition blocked = new BlockPosition(1, 64, 0);
        BlockTraversalGraph graph = new BlockTraversalGraph(new TestWorldLayer(Set.of(blocked)), start, blocked, 8, 2);

        for (Connection<BlockPosition> connection : graph.outgoingConnections(start)) {
            assertFalse(connection.to().equals(blocked));
        }
    }

    private static void assertPathAvoids(GraphPath<BlockPosition> path, BlockPosition blocked) {
        for (BlockPosition node : path) {
            assertFalse(node.equals(blocked));
        }
    }

    private static double distance(BlockPosition from, BlockPosition to) {
        return Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y()) + Math.abs(from.z() - to.z());
    }

    private record TestWorldLayer(Set<BlockPosition> blockedFeet) implements WorldLayer {
        private TestWorldLayer {
            blockedFeet = new HashSet<>(blockedFeet);
        }

        @Override
        public BlockClassification classify(BlockPosition position) {
            if (blockedFeet.contains(position)) {
                return new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID);
            }
            if (position.y() == 63) {
                return new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID);
            }
            return new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID);
        }
    }
}

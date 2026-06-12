package dev.traveler.core.world.navigation;

import dev.traveler.core.world.block.BlockPosition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockTraversalGraphTest {
    @Test
    void avoidsSolidBlocksInsteadOfConnectingDirectlyToGoal() {
        BlockPosition start = new BlockPosition(0, 64, 0);
        BlockPosition goal = new BlockPosition(2, 64, 0);
        BlockPosition blocked = new BlockPosition(1, 64, 0);
        BlockedWorldLayer world = new BlockedWorldLayer(Set.of(blocked, blocked.above()));
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
        BlockTraversalGraph graph =
                new BlockTraversalGraph(new BlockedWorldLayer(Set.of(blocked)), start, blocked, 8, 2);

        for (Connection<BlockPosition> connection : graph.outgoingConnections(start)) {
            assertFalse(connection.to().equals(blocked));
        }
    }

    @Test
    void exposesDiagonalConnectionsInOpenSpace() {
        BlockPosition start = new BlockPosition(0, 64, 0);
        BlockPosition diagonal = new BlockPosition(1, 64, 1);
        BlockTraversalGraph graph = new BlockTraversalGraph(new BlockedWorldLayer(Set.of()), start, diagonal, 8, 2);

        Connection<BlockPosition> connection = connectionTo(graph, start, diagonal);

        assertEquals(Math.sqrt(2.0), connection.cost(), 0.0001);
    }

    @Test
    void rejectsDiagonalConnectionsThatCutBlockedCorners() {
        assertNoDiagonalConnectionWhenCornerBlocked(new BlockPosition(1, 64, 0));
    }

    @Test
    void rejectsDiagonalConnectionsWhenSecondCornerIsBlocked() {
        assertNoDiagonalConnectionWhenCornerBlocked(new BlockPosition(0, 64, 1));
    }

    private static void assertNoDiagonalConnectionWhenCornerBlocked(BlockPosition blockedCorner) {
        BlockPosition start = new BlockPosition(0, 64, 0);
        BlockPosition diagonal = new BlockPosition(1, 64, 1);
        BlockTraversalGraph graph =
                new BlockTraversalGraph(new BlockedWorldLayer(Set.of(blockedCorner)), start, diagonal, 8, 2);

        List<Connection<BlockPosition>> connections = connectionsFrom(graph, start);

        assertTrue(connections.stream().noneMatch(connection -> connection.to().equals(diagonal)));
    }

    private static void assertPathAvoids(GraphPath<BlockPosition> path, BlockPosition blocked) {
        for (BlockPosition node : path) {
            assertFalse(node.equals(blocked));
        }
    }

    private static Connection<BlockPosition> connectionTo(
            BlockTraversalGraph graph, BlockPosition start, BlockPosition destination) {
        return connectionsFrom(graph, start).stream()
                .filter(connection -> connection.to().equals(destination))
                .findFirst()
                .orElseThrow();
    }

    private static List<Connection<BlockPosition>> connectionsFrom(BlockTraversalGraph graph, BlockPosition start) {
        List<Connection<BlockPosition>> connections = new ArrayList<>();
        graph.outgoingConnections(start).forEach(connections::add);
        return List.copyOf(connections);
    }

    private static double distance(BlockPosition from, BlockPosition to) {
        return Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y()) + Math.abs(from.z() - to.z());
    }

}

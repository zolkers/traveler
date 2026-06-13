package dev.traveler.core.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.graph.Heuristic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AStarPathfinderTest {
    private static final Heuristic<Point> MANHATTAN = (from, to) -> Math.abs(from.x() - to.x())
            + Math.abs(from.y() - to.y());

    @Test
    void returnsSingleNodePathWhenStartIsGoal() {
        AStarPathfinder<Point> pathfinder = new AStarPathfinder<>();
        Point start = new Point(0, 0);
        PathfinderRequest<Point> request = new PathfinderRequest<>(GridGraph.open(1, 1), start, start, MANHATTAN);

        PathfinderResult<Point> result = pathfinder.search(request);

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertEquals(0.0, result.path().cost());
        assertIterableEquals(List.of(start), result.path());
    }

    @Test
    void findsPathThroughGrid() {
        AStarPathfinder<Point> pathfinder = new AStarPathfinder<>();
        GridGraph graph = GridGraph.withBlocked(3, 3, Set.of(new Point(1, 0), new Point(1, 1)));
        Point start = new Point(0, 0);
        Point goal = new Point(2, 0);

        PathfinderResult<Point> result = pathfinder.search(new PathfinderRequest<>(graph, start, goal, MANHATTAN));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertEquals(6.0, result.path().cost());
        assertEquals(start, result.path().nodeAt(0));
        assertEquals(goal, result.path().nodeAt(result.path().nodeCount() - 1));
        assertFalse(contains(result.path(), new Point(1, 0)));
        assertFalse(contains(result.path(), new Point(1, 1)));
    }

    @Test
    void returnsNotFoundWhenGoalIsBlocked() {
        AStarPathfinder<Point> pathfinder = new AStarPathfinder<>();
        GridGraph graph = GridGraph.withBlocked(3, 1, Set.of(new Point(1, 0)));
        PathfinderRequest<Point> request = new PathfinderRequest<>(
                graph, new Point(0, 0), new Point(2, 0), MANHATTAN);

        PathfinderResult<Point> result = pathfinder.search(request);

        assertEquals(PathfinderStatus.NOT_FOUND, result.status());
        assertTrue(result.path().isEmpty());
    }

    @Test
    void choosesLowerTotalCostOverFewerEdges() {
        AStarPathfinder<String> pathfinder = new AStarPathfinder<>();
        Map<String, List<Connection<String>>> edges = Map.of(
                "a", List.of(new Connection<>("a", "b", 10.0), new Connection<>("a", "c", 1.0)),
                "c", List.of(new Connection<>("c", "d", 1.0)),
                "d", List.of(new Connection<>("d", "b", 1.0)));
        PathfinderRequest<String> request = new PathfinderRequest<>(
                new MapGraph<>(edges), "a", "b", (from, to) -> 0.0);

        PathfinderResult<String> result = pathfinder.search(request);

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertEquals(3.0, result.path().cost());
        assertIterableEquals(List.of("a", "c", "d", "b"), result.path());
    }

    @Test
    void budgetedSearchCanResumeAfterReturningRunning() {
        AStarPathfinder<Point> pathfinder = new AStarPathfinder<>();
        PathfinderRequest<Point> request = new PathfinderRequest<>(
                GridGraph.open(10, 10), new Point(0, 0), new Point(9, 9), MANHATTAN);

        PathfinderResult<Point> first = pathfinder.search(request, 1L);
        PathfinderResult<Point> second = pathfinder.search(request, Long.MAX_VALUE);

        assertEquals(PathfinderStatus.RUNNING, first.status());
        assertTrue(first.path().isEmpty());
        assertEquals(PathfinderStatus.FOUND, second.status());
        assertEquals(18.0, second.path().cost());
        assertEquals(19, second.path().nodeCount());
    }

    @Test
    void returnsRunningWhenThreadIsInterrupted() {
        AStarPathfinder<Point> pathfinder = new AStarPathfinder<>();
        PathfinderRequest<Point> request = new PathfinderRequest<>(
                GridGraph.open(10, 10), new Point(0, 0), new Point(9, 9), MANHATTAN);

        Thread.currentThread().interrupt();
        PathfinderResult<Point> result = pathfinder.search(request);
        Thread.interrupted();

        assertEquals(PathfinderStatus.RUNNING, result.status());
        assertTrue(result.path().isEmpty());
    }

    private static boolean contains(GraphPath<Point> path, Point node) {
        for (Point current : path) {
            if (current.equals(node)) {
                return true;
            }
        }
        return false;
    }

    private record Point(int x, int y) {
    }

    private static final class GridGraph implements Graph<Point> {
        private static final int[][] CARDINAL_DIRECTIONS = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };

        private final int width;
        private final int height;
        private final Set<Point> blocked;

        private GridGraph(int width, int height, Set<Point> blocked) {
            this.width = width;
            this.height = height;
            this.blocked = Set.copyOf(blocked);
        }

        static GridGraph open(int width, int height) {
            return new GridGraph(width, height, Set.of());
        }

        static GridGraph withBlocked(int width, int height, Set<Point> blocked) {
            return new GridGraph(width, height, blocked);
        }

        @Override
        public Iterable<Connection<Point>> outgoingConnections(Point node) {
            if (!isTraversable(node)) {
                return List.of();
            }
            List<Connection<Point>> connections = new ArrayList<>();
            for (int[] direction : CARDINAL_DIRECTIONS) {
                addConnection(connections, node, direction[0], direction[1]);
            }
            return connections;
        }

        private void addConnection(List<Connection<Point>> connections, Point from, int xOffset, int yOffset) {
            Point to = new Point(from.x() + xOffset, from.y() + yOffset);
            if (!isTraversable(to)) {
                return;
            }
            connections.add(new Connection<>(from, to, 1.0));
        }

        private boolean isTraversable(Point point) {
            return point.x() >= 0
                    && point.x() < width
                    && point.y() >= 0
                    && point.y() < height
                    && !blocked.contains(point);
        }
    }

    private static final class MapGraph<N> implements Graph<N> {
        private final Map<N, List<Connection<N>>> edges;

        private MapGraph(Map<N, List<Connection<N>>> edges) {
            this.edges = new HashMap<>(edges);
        }

        @Override
        public Iterable<Connection<N>> outgoingConnections(N node) {
            return edges.getOrDefault(node, List.of());
        }
    }
}

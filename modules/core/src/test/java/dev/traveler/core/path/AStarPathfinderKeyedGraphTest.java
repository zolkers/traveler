package dev.traveler.core.path;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.KeyedGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

class AStarPathfinderKeyedGraphTest {
    @Test
    void keyedGraphsUseStableKeysInsteadOfObjectEquality() {
        Node start = new Node(0, 0);
        Node goal = new Node(1, 0);
        KeyedGraph<Node> graph = new KeyedGraph<>() {
            @Override
            public Iterable<Connection<Node>> outgoingConnections(Node node) {
                if (node.id() != 0) {
                    return List.of();
                }
                return List.of(new Connection<>(node, new Node(1, 1), 1.0));
            }

            @Override
            public long keyOf(Node node) {
                return node.id();
            }
        };

        PathfinderResult<Node> result = new AStarPathfinder<Node>()
                .search(new PathfinderRequest<>(graph, start, goal, (from, to) -> 0.0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertEquals(1, result.path().nodeAt(1).id());
    }

    private record Node(int id, int revision) {}
}

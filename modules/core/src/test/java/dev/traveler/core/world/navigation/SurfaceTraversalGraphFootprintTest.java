package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurfaceTraversalGraphFootprintTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    @Test
    void refusesConnectionsWhenDestinationFootprintOverlapsAdjacentBodyBlock() {
        SurfaceNode start = new SurfaceNode(new BlockPosition(0, 63, 0), 1, 1, 64.0);
        SurfaceNode destination = new SurfaceNode(new BlockPosition(1, 63, 0), 0, 1, 64.0);
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(Map.of(
                start.blockPosition(), SurfaceBlock.solid(BlockShape.fullCube()),
                destination.blockPosition(), SurfaceBlock.solid(BlockShape.fullCube()),
                new BlockPosition(1, 64, 0), SurfaceBlock.solid(BlockShape.fullCube())));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, start, destination, PLAYER, 8, 4);

        List<Connection<SurfaceNode>> connections = connectionsFrom(graph, start);

        assertTrue(connections.stream().noneMatch(connection -> connection.to().sameSubcell(destination)));
    }

    private static List<Connection<SurfaceNode>> connectionsFrom(SurfaceTraversalGraph graph, SurfaceNode start) {
        List<Connection<SurfaceNode>> connections = new ArrayList<>();
        graph.outgoingConnections(start).forEach(connections::add);
        return List.copyOf(connections);
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}

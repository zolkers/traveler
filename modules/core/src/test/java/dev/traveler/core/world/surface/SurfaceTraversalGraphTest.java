package dev.traveler.core.world.surface;

import static dev.traveler.core.world.surface.FakeSurfaceWorldLayer.bottomSlab;
import static dev.traveler.core.world.surface.FakeSurfaceWorldLayer.fullBlock;
import static dev.traveler.core.world.surface.FakeSurfaceWorldLayer.northFacingBottomStair;
import static dev.traveler.core.world.surface.FakeSurfaceWorldLayer.topSlab;
import static dev.traveler.core.world.surface.FakeSurfaceWorldLayer.waterloggedBottomSlab;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.behavior.special.AirBlockBehavior;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.navigation.SurfaceTraversalGraphSettings;
import dev.traveler.core.world.navigation.SurfaceTraversalGraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SurfaceTraversalGraphTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);
    private static final MovementCapabilities LOW_JUMP_PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 0.4, 3.0);

    @Test
    void refusesToWalkIntoAirWithoutSupport() {
        BlockPosition startBlock = new BlockPosition(0, 63, 0);
        SurfaceNode start = new SurfaceNode(startBlock, 1, 1, 64.0);
        SurfaceNode airNeighbor = new SurfaceNode(new BlockPosition(1, 63, 0), 0, 1, 64.0);
        FakeSurfaceWorldLayer world = new FakeSurfaceWorldLayer(Map.of(startBlock, fullBlock()));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, start, airNeighbor, PLAYER, 8, 4);

        List<Connection<SurfaceNode>> connections = connectionsFrom(graph, start);

        assertTrue(connections.stream().noneMatch(connection -> connection.to().sameSubcell(airNeighbor)));
    }

    @Test
    void refusesSurfaceWhenBehaviorDoesNotSupportWalkingAction() {
        BlockPosition startBlock = new BlockPosition(0, 63, 0);
        BlockPosition passThroughBlock = new BlockPosition(1, 63, 0);
        SurfaceNode start = new SurfaceNode(startBlock, 1, 1, 64.0);
        SurfaceNode destination = new SurfaceNode(passThroughBlock, 0, 1, 64.0);
        SurfaceBlock passThroughSurface = new SurfaceBlock(
                fullBlock().classification(),
                BlockShape.fullCube(),
                new AirBlockBehavior());
        FakeSurfaceWorldLayer world =
                new FakeSurfaceWorldLayer(Map.of(startBlock, fullBlock(), passThroughBlock, passThroughSurface));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, start, destination, PLAYER, 8, 4);

        List<Connection<SurfaceNode>> connections = connectionsFrom(graph, start);

        assertTrue(connections.stream().noneMatch(connection -> connection.to().sameSubcell(destination)));
    }

    @Test
    void bottomSlabsProvideHalfHeightWalkableSurfaces() {
        BlockPosition firstSlab = new BlockPosition(0, 63, 0);
        BlockPosition secondSlab = new BlockPosition(1, 63, 0);
        SurfaceNode start = new SurfaceNode(firstSlab, 1, 1, 63.5);
        SurfaceNode goal = new SurfaceNode(secondSlab, 0, 1, 63.5);
        FakeSurfaceWorldLayer world =
                new FakeSurfaceWorldLayer(Map.of(firstSlab, bottomSlab(), secondSlab, bottomSlab()));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, start, goal, PLAYER, 8, 4);

        Connection<SurfaceNode> connection = connectionTo(graph, start, goal);

        assertNotNull(connection);
        assertEquals(63.5, connection.to().floorY());
    }

    @Test
    void oneBlockDropProvidesWalkableSurfaceConnection() {
        BlockPosition highBlock = new BlockPosition(0, 63, 0);
        BlockPosition lowBlock = new BlockPosition(1, 62, 0);
        SurfaceNode start = new SurfaceNode(highBlock, 1, 1, 64.0);
        SurfaceNode destination = new SurfaceNode(lowBlock, 1, 1, 63.0);
        FakeSurfaceWorldLayer world =
                new FakeSurfaceWorldLayer(Map.of(highBlock, fullBlock(), lowBlock, fullBlock()));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, start, destination, PLAYER, 8, 4);

        Connection<SurfaceNode> connection = connectionTo(graph, start, destination);

        assertNotNull(connection);
        assertEquals(63.0, connection.to().floorY());
    }

    @ParameterizedTest
    @MethodSource("specialDropSurfaces")
    void oneBlockDropKeepsDestinationBehavior(
            SurfaceBlock destinationBlock, SurfaceNode destination, double expectedFloorY) {
        BlockPosition highBlock = new BlockPosition(0, 63, 0);
        SurfaceNode start = new SurfaceNode(highBlock, 1, 1, 64.0);
        FakeSurfaceWorldLayer world = new FakeSurfaceWorldLayer(Map.of(
                highBlock, fullBlock(),
                destination.blockPosition(), destinationBlock));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, start, destination, PLAYER, 8, 4);

        Connection<SurfaceNode> connection = connectionTo(graph, start, destination);

        assertNotNull(connection);
        assertEquals(expectedFloorY, connection.to().floorY());
    }

    @Test
    void neighborExpansionDoesNotRereadTheSameSurfaceBlock() {
        BlockPosition startBlock = new BlockPosition(0, 63, 0);
        BlockPosition destinationBlock = new BlockPosition(1, 63, 0);
        SurfaceNode start = new SurfaceNode(startBlock, 1, 1, 64.0);
        SurfaceNode destination = new SurfaceNode(destinationBlock, 0, 1, 64.0);
        CountingSurfaceWorldLayer world =
                new CountingSurfaceWorldLayer(Map.of(startBlock, fullBlock(), destinationBlock, fullBlock()));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, start, destination, PLAYER, 8, 4);

        connectionsFrom(graph, start);

        assertEquals(1, world.readCount(destinationBlock));
    }

    @Test
    void headroomMustBeClearAboveTopSlabSurface() {
        BlockPosition slab = new BlockPosition(0, 63, 0);
        BlockPosition headBlock = new BlockPosition(1, 64, 0);
        SurfaceNode start = new SurfaceNode(slab, 1, 1, 64.0);
        SurfaceNode blockedTop = new SurfaceNode(new BlockPosition(1, 63, 0), 0, 1, 64.0);
        FakeSurfaceWorldLayer world = new FakeSurfaceWorldLayer(Map.of(
                slab, fullBlock(),
                blockedTop.blockPosition(), topSlab(),
                headBlock, fullBlock()));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, start, blockedTop, PLAYER, 8, 4);

        List<Connection<SurfaceNode>> connections = connectionsFrom(graph, start);

        assertTrue(connections.stream().noneMatch(connection -> connection.to().sameSubcell(blockedTop)));
    }

    @Test
    void stairShapesExposeLowAndHighSubcellSurfaces() {
        BlockPosition stair = new BlockPosition(0, 63, 0);
        SurfaceNode lowFront = new SurfaceNode(stair, 1, 0, 63.5);
        SurfaceNode highBack = new SurfaceNode(stair, 1, 1, 64.0);
        FakeSurfaceWorldLayer world = new FakeSurfaceWorldLayer(Map.of(stair, northFacingBottomStair()));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, lowFront, highBack, PLAYER, 8, 4);

        Connection<SurfaceNode> connection = connectionTo(graph, lowFront, highBack);

        assertNotNull(connection);
        assertEquals(64.0, connection.to().floorY());
        assertTrue(connection.cost() < 1.5);
    }

    @Test
    void frontStairStepDoesNotRequireJumpCapability() {
        BlockPosition stair = new BlockPosition(0, 63, 0);
        SurfaceNode lowFront = new SurfaceNode(stair, 1, 0, 63.5);
        SurfaceNode highBack = new SurfaceNode(stair, 1, 1, 64.0);
        FakeSurfaceWorldLayer world = new FakeSurfaceWorldLayer(Map.of(stair, northFacingBottomStair()));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(world, lowFront, highBack, LOW_JUMP_PLAYER, 8, 4);

        Connection<SurfaceNode> connection = connectionTo(graph, lowFront, highBack);

        assertNotNull(connection);
        assertEquals(64.0, connection.to().floorY());
    }

    @Test
    void stairStepsRemainReachableWhenFullBlockSidesAreBlocked() {
        BlockPosition lowSlab = new BlockPosition(0, 63, 0);
        BlockPosition stair = new BlockPosition(0, 63, 1);
        BlockPosition fullBlock = new BlockPosition(1, 63, 0);
        SurfaceNode start = new SurfaceNode(lowSlab, 1, 1, 63.5);
        FakeSurfaceWorldLayer stairWorld =
                new FakeSurfaceWorldLayer(Map.of(lowSlab, bottomSlab(), stair, northFacingBottomStair()));
        FakeSurfaceWorldLayer fullBlockWorld =
                new FakeSurfaceWorldLayer(Map.of(lowSlab, bottomSlab(), fullBlock, fullBlock()));
        SurfaceTraversalGraph stairGraph = new SurfaceTraversalGraph(stairWorld, start, start, PLAYER, 8, 4);
        SurfaceTraversalGraph fullBlockGraph = new SurfaceTraversalGraph(fullBlockWorld, start, start, PLAYER, 8, 4);

        Connection<SurfaceNode> stairConnection = connectionTo(stairGraph, start, new SurfaceNode(stair, 1, 0, 63.5));
        List<Connection<SurfaceNode>> fullBlockConnections = connectionsFrom(fullBlockGraph, start);

        assertNotNull(stairConnection);
        assertTrue(fullBlockConnections.stream()
                .noneMatch(connection -> connection.to().sameSubcell(new SurfaceNode(fullBlock, 0, 1, 64.0))));
    }

    @Test
    void clearanceScoringPrefersOpenGroundOverWallHuggingRoutes() {
        SurfaceNode start = nodeAt(0, 0);
        SurfaceNode goal = nodeAt(5, 0);
        FakeSurfaceWorldLayer world = new FakeSurfaceWorldLayer(wideSurfaceWithSouthWall(0, 5));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(
                world,
                start,
                goal,
                PLAYER,
                SurfaceTraversalGraphSettings.standard(8, 4));

        PathfinderResult<SurfaceNode> result = new AStarPathfinder<SurfaceNode>()
                .search(new PathfinderRequest<>(graph, start, goal, SurfaceTraversalGraphTest::distance));
        GraphPath<SurfaceNode> path = result.path();

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertTrue(path.nodes().stream().anyMatch(node -> node.blockPosition().z() < 0));
    }

    private static Connection<SurfaceNode> connectionTo(
            SurfaceTraversalGraph graph, SurfaceNode start, SurfaceNode destination) {
        return connectionsFrom(graph, start).stream()
                .filter(connection -> connection.to().sameSubcell(destination))
                .findFirst()
                .orElseThrow();
    }

    private static List<Connection<SurfaceNode>> connectionsFrom(SurfaceTraversalGraph graph, SurfaceNode start) {
        List<Connection<SurfaceNode>> connections = new ArrayList<>();
        graph.outgoingConnections(start).forEach(connections::add);
        return List.copyOf(connections);
    }

    private static Map<BlockPosition, SurfaceBlock> wideSurfaceWithSouthWall(int minX, int maxX) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            addSurfaceColumn(blocks, x);
            blocks.put(new BlockPosition(x, 64, 1), fullBlock());
        }
        return blocks;
    }

    private static void addSurfaceColumn(Map<BlockPosition, SurfaceBlock> blocks, int x) {
        for (int z = -2; z <= 0; z++) {
            blocks.put(new BlockPosition(x, 63, z), fullBlock());
        }
    }

    private static SurfaceNode nodeAt(int x, int z) {
        return new SurfaceNode(new BlockPosition(x, 63, z), 0, 0, 64.0);
    }

    private static double distance(SurfaceNode from, SurfaceNode to) {
        return Math.hypot(from.centerX() - to.centerX(), from.centerZ() - to.centerZ());
    }

    private static Stream<Arguments> specialDropSurfaces() {
        BlockPosition lowBlock = new BlockPosition(1, 62, 0);
        return Stream.of(
                Arguments.of(bottomSlab(), new SurfaceNode(lowBlock, 1, 1, 62.5), 62.5),
                Arguments.of(northFacingBottomStair(), new SurfaceNode(lowBlock, 1, 1, 63.0), 63.0),
                Arguments.of(waterloggedBottomSlab(), new SurfaceNode(lowBlock, 1, 1, 62.5), 62.5));
    }

    private static final class CountingSurfaceWorldLayer implements SurfaceWorldLayer {
        private final Map<BlockPosition, SurfaceBlock> blocks;
        private final Map<BlockPosition, Integer> reads = new HashMap<>();

        private CountingSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) {
            this.blocks = Map.copyOf(blocks);
        }

        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            reads.merge(position, 1, Integer::sum);
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }

        private int readCount(BlockPosition position) {
            return reads.getOrDefault(position, 0);
        }
    }
}

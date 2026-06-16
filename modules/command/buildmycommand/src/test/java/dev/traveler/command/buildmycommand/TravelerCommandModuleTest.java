package dev.traveler.command.buildmycommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riege.buildmycommand.api.CommandResult;
import dev.traveler.command.buildmycommand.testing.CommandModuleTestHarness;
import dev.traveler.command.buildmycommand.testing.TestCommandSource;
import dev.traveler.core.debug.snapshots.PathfinderDebugSnapshot;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.navigation.NavigationSession;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.behavior.special.FluidBlockBehavior;
import dev.traveler.core.world.behavior.special.LadderBlockBehavior;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TravelerCommandModuleTest {
    @Test
    void buildMyCommandAdapterOwnsTravelerRoutesAndCommandExecution() {
        TravelerCommandModule module = new TravelerCommandModule();

        String schema = module.framework().schema();
        CommandResult result = module.framework().dispatch(new TestCommandSource(), "traveler path test");

        assertTrue(schema.contains("command traveler path test"));
        assertTrue(schema.contains("command traveler path block"));
        assertTrue(schema.contains("command traveler navigate block"));
        assertTrue(schema.contains("command traveler navigate stop"));
        assertTrue(schema.contains("command traveler debug status"));
        assertTrue(schema.contains("command traveler debug clear"));
        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals("path test status=FOUND nodes=2", result.reply().orElseThrow());
        assertTrue(module.debugState().latestResult().isPresent());
    }

    @Test
    void adapterRoutesCommandSourcePositionIntoCoreHandlers() {
        TravelerCommandModule module = new TravelerCommandModule();
        BlockPosition start = new BlockPosition(8, 70, -4);
        TestCommandSource source = new TestCommandSource(start);

        CommandResult result = CommandModuleTestHarness.dispatchAndDrain(
                module, source, "traveler navigate block 1 2 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(result.reply().orElseThrow().contains("navigate queued id="));
        assertEquals(
                new WorldPoint(1.5, 2.0, 3.5),
                module.navigationState().activeSession().orElseThrow().path().lastNode());
        assertTrue(source.replies().getLast().contains("navigate block"));
    }

    @Test
    void debugRoutesUseProvidedCoreState() {
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerCommandModule module =
                new TravelerCommandModule(debugState, new TravelerNavigationState(), () -> null);
        TestCommandSource source = new TestCommandSource();
        module.framework().dispatch(source, "traveler path test");

        CommandResult status = module.framework().dispatch(source, "traveler debug status");
        CommandResult clear = module.framework().dispatch(source, "traveler debug clear");

        assertEquals(CommandResult.Status.SUCCESS, status.status());
        assertTrue(status.reply().orElseThrow().contains("path status=FOUND"));
        assertEquals(CommandResult.Status.SUCCESS, clear.status());
        assertTrue(debugState.latestSnapshot().isEmpty());
    }

    @Test
    void pathBlockUsesWorldTraversalWhenLayerIsAvailable() {
        BlockPosition start = new BlockPosition(0, 64, 0);
        BlockPosition goal = new BlockPosition(2, 64, 0);
        BlockPosition wall = new BlockPosition(1, 64, 0);
        TravelerCommandModule module = new TravelerCommandModule(new TestWorldLayer(Set.of(wall, wall.above())));

        CommandModuleTestHarness.dispatchAndDrain(
                module, new TestCommandSource(start), "traveler path block 2 64 0");

        GraphPath<BlockPosition> path = module.debugState().latestResult().orElseThrow().path();
        assertTrue(path.nodeCount() > 3);
        assertPathAvoids(path, wall);
        assertEquals(goal, path.nodeAt(path.nodeCount() - 1));
    }

    @Test
    void navigateBlockResolvesLadderTargetToClimbLandingInCommandFlow() {
        BlockPosition startFeet = new BlockPosition(0, 64, 0);
        TravelerCommandModule module = new TravelerCommandModule(new TestSurfaceWorldLayer(tallLadderSurface()));

        CommandModuleTestHarness.dispatchAndDrain(
                module, new TestCommandSource(startFeet), "traveler navigate block 1 82 0");

        NavigationSession session = module.navigationState().activeSession().orElseThrow();
        assertTrue(session.path().segmentActions().contains(MovementAction.CLIMB));
        assertEquals(20, session.path().segmentActions().stream()
                .filter(action -> action == MovementAction.CLIMB)
                .count());
        assertEquals(83.0, session.path().lastNode().y());
        PathfinderDebugSnapshot snapshot = module.debugState().latestSnapshot().orElseThrow();
        assertHasSurfaceNodes(snapshot);
        assertEquals(new BlockPosition(1, 82, 1), snapshot.surfaceNodes().getLast().blockPosition());
    }

    @Test
    void navigateBlockUsesSwimSurfacePathInCommandFlow() {
        BlockPosition startFeet = new BlockPosition(0, 64, 0);
        TravelerCommandModule module = new TravelerCommandModule(new TestSurfaceWorldLayer(waterLane(0, 4, 63)));

        CommandModuleTestHarness.dispatchAndDrain(
                module, new TestCommandSource(startFeet), "traveler navigate block 4 64 0");

        NavigationSession session = module.navigationState().activeSession().orElseThrow();
        assertTrue(session.path().segmentActions().contains(MovementAction.SWIM));
        assertTrue(session.path().nodes().stream().allMatch(point -> point.y() == 64.0));
        PathfinderDebugSnapshot snapshot = module.debugState().latestSnapshot().orElseThrow();
        assertHasSurfaceNodes(snapshot);
        assertTrue(snapshot.surfaceNodes().stream().allMatch(node -> node.floorY() == 64.0));
    }

    private static void assertPathAvoids(GraphPath<BlockPosition> path, BlockPosition blocked) {
        for (BlockPosition node : path) {
            assertTrue(!node.equals(blocked));
        }
    }

    private static void assertHasSurfaceNodes(PathfinderDebugSnapshot snapshot) {
        assertTrue(snapshot.hasSurfaceNodes(), () -> "status="
                + snapshot.result().status()
                + " nodes="
                + snapshot.result().path().nodeCount()
                + " message="
                + snapshot.message());
    }

    private static SurfaceBlock surfaceBlock(BlockShape shape) {
        return SurfaceBlock.solid(shape);
    }

    private static SurfaceBlock passableBlock(BlockShape shape, BlockBehavior behavior, FluidHandling fluidHandling) {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.PASSABLE, fluidHandling),
                shape,
                behavior);
    }

    private static SurfaceBlock ladderBlock(HorizontalFacing facing) {
        return passableBlock(BlockShape.empty(), new LadderBlockBehavior(facing), FluidHandling.AVOID);
    }

    private static SurfaceBlock waterBlock() {
        return passableBlock(BlockShape.empty(), new FluidBlockBehavior(), FluidHandling.ALLOW);
    }

    private static Map<BlockPosition, SurfaceBlock> tallLadderSurface() {
        Map<BlockPosition, SurfaceBlock> blocks = new java.util.HashMap<>();
        blocks.put(new BlockPosition(0, 63, 0), surfaceBlock(BlockShape.fullCube()));
        for (int y = 64; y <= 82; y++) {
            blocks.put(new BlockPosition(1, y, 0), ladderBlock(HorizontalFacing.WEST));
        }
        blocks.put(new BlockPosition(1, 82, 1), surfaceBlock(BlockShape.fullCube()));
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> waterLane(int minX, int maxX, int y) {
        Map<BlockPosition, SurfaceBlock> blocks = new java.util.HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            blocks.put(new BlockPosition(x, y, 0), waterBlock());
        }
        return blocks;
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

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}

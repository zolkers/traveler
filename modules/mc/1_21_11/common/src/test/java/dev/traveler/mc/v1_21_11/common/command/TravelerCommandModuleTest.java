package dev.traveler.mc.v1_21_11.common.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.traveler.core.command.AnnotatedTravelerCommandFeature;
import dev.traveler.core.command.TravelerCommandCatalog;
import dev.traveler.core.debug.PathfinderDebugSnapshot;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.NavigationSession;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import dev.traveler.core.navigation.plan.ActionIntent;
import dev.traveler.core.navigation.plan.MovementVectorIntent;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.plan.SpeedIntent;
import dev.traveler.core.navigation.plan.ToleranceProfile;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.mc.v1_21_11.common.adapter.testing.AbstractTestBlockGetter;
import dev.traveler.mc.v1_21_11.common.adapter.testing.MinecraftTestBootstrap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TravelerCommandModuleTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void exposesModularCommandCatalog() {
        TravelerCommandModule module = new TravelerCommandModule();

        assertEquals(6, module.catalog().routes().size());
        assertTrue(module.catalog().route("traveler path test").isPresent());
        assertTrue(module.catalog().route("traveler path block <x:int> <y:int> <z:int>").isPresent());
        assertTrue(module.catalog().route("traveler navigate block <x:int> <y:int> <z:int>").isPresent());
        assertTrue(module.catalog().route("traveler navigate stop").isPresent());
        assertTrue(module.catalog().route("traveler debug status").isPresent());
        assertTrue(module.catalog().route("traveler debug clear").isPresent());
    }

    @Test
    void pathCommandFeatureCanBeRegisteredFromAnnotations() {
        PathTravelerCommandFeature feature = new PathTravelerCommandFeature(
                new PathfinderDebugState(),
                () -> null);

        TravelerCommandCatalog catalog =
                TravelerCommandCatalog.fromFeatures(AnnotatedTravelerCommandFeature.from(feature));

        assertEquals(2, catalog.routes().size());
        assertTrue(catalog.route("traveler path test").isPresent());
        assertTrue(catalog.route("traveler path block <x:int> <y:int> <z:int>").isPresent());
    }

    @Test
    void registersPathTestCommandAndUpdatesDebugState() {
        TravelerCommandModule module = new TravelerCommandModule();
        TestSource source = new TestSource();

        CommandResult result = module.framework().dispatch(source, "traveler path test");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(module.debugState().latestResult().isPresent());
        assertTrue(module.debugState().latestMessage().orElseThrow().contains("path test"));
        assertEquals(List.of(result.reply().orElseThrow()), source.replies());
    }

    @Test
    void registersPathBlockCommandAndIncludesTargetInDebugMessage() {
        TravelerCommandModule module = new TravelerCommandModule();

        CommandResult result = dispatchAndDrain(module, new TestSource(), "traveler path block 1 2 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(module.debugState().latestResult().isPresent());
        assertTrue(module.debugState().latestMessage().orElseThrow().contains("1,2,3"));
    }

    @Test
    void pathBlockQueuesThenUpdatesDebugWhenDrained() {
        TravelerCommandModule module = new TravelerCommandModule();
        TestSource source = new TestSource();

        CommandResult result = module.framework().dispatch(source, "traveler path block 1 2 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(result.reply().orElseThrow().contains("path queued id="));
        assertTrue(module.debugState().latestResult().isEmpty());

        waitForJobs(module, () -> module.debugState().latestResult().isPresent());

        assertTrue(module.debugState().latestMessage().orElseThrow().contains("1,2,3"));
        assertTrue(source.replies().getLast().contains("path block 1,2,3 status=FOUND"));
    }

    @Test
    void navigateBlockStartsNavigationAndStoresDebugPath() {
        TravelerCommandModule module = new TravelerCommandModule();
        TestSource source = new TestSource();

        CommandResult result = dispatchAndDrain(module, source, "traveler navigate block 1 2 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        NavigationSession session = module.navigationState().activeSession().orElseThrow();
        assertEquals(new NavigationPoint(1.5, 2.0, 3.5), session.path().lastNode());
        assertTrue(module.debugState().latestResult().isPresent());
        assertTrue(result.reply().orElseThrow().contains("navigate queued id="));
        assertTrue(source.replies().getLast().contains("path status=FOUND"));
    }

    @Test
    void navigateBlockStartsNavigationAfterAsyncPathDrains() {
        TravelerCommandModule module = new TravelerCommandModule();
        TestSource source = new TestSource();

        CommandResult result = module.framework().dispatch(source, "traveler navigate block 1 2 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(result.reply().orElseThrow().contains("navigate queued id="));
        assertTrue(module.navigationState().activeSession().isEmpty());

        waitForJobs(module, () -> module.navigationState().activeSession().isPresent());

        assertEquals(
                new NavigationPoint(1.5, 2.0, 3.5),
                module.navigationState().activeSession().orElseThrow().path().lastNode());
        assertTrue(source.replies().getLast().contains("navigate block"));
    }

    @Test
    void navigateStopClearsActiveNavigation() {
        TravelerCommandModule module = new TravelerCommandModule();
        dispatchAndDrain(module, new TestSource(), "traveler navigate block 1 2 3");

        CommandResult result = module.framework().dispatch(new TestSource(), "traveler navigate stop");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(module.navigationState().activeSession().isEmpty());
        assertTrue(module.navigationState().latestMessage().orElseThrow().contains("stopped"));
        assertTrue(result.reply().orElseThrow().contains("debug nav cleared"));
    }

    @Test
    void pathBlockStartsAtCommandSourcePositionWhenAvailable() {
        TravelerCommandModule module = new TravelerCommandModule();
        BlockPosition start = new BlockPosition(8, 70, -4);

        CommandResult result = dispatchAndDrain(module, new TestSource(start), "traveler path block 1 2 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        PathfinderResult<BlockPosition> pathResult = module.debugState().latestResult().orElseThrow();
        GraphPath<BlockPosition> path = pathResult.path();
        assertEquals(start, path.nodeAt(0));
        assertEquals(new BlockPosition(1, 2, 3), path.nodeAt(path.nodeCount() - 1));
    }

    @Test
    void pathBlockUsesWorldTraversalWhenLayerIsAvailable() {
        BlockPosition start = new BlockPosition(0, 64, 0);
        BlockPosition goal = new BlockPosition(2, 64, 0);
        BlockPosition wall = new BlockPosition(1, 64, 0);
        TravelerCommandModule module = new TravelerCommandModule(new TestWorldLayer(Set.of(wall, wall.above())));

        dispatchAndDrain(module, new TestSource(start), "traveler path block 2 64 0");

        GraphPath<BlockPosition> path = module.debugState().latestResult().orElseThrow().path();
        assertTrue(path.nodeCount() > 3);
        assertPathAvoids(path, wall);
        assertEquals(goal, path.nodeAt(path.nodeCount() - 1));
    }

    @Test
    void pathBlockStoresSmoothedPathWhenLineOfWalkIsClear() {
        BlockPosition start = new BlockPosition(0, 64, 0);
        BlockPosition goal = new BlockPosition(4, 64, 4);
        TravelerCommandModule module = new TravelerCommandModule(new TestWorldLayer(Set.of()));

        dispatchAndDrain(module, new TestSource(start), "traveler path block 4 64 4");

        GraphPath<BlockPosition> path = module.debugState().latestResult().orElseThrow().path();
        assertEquals(2, path.nodeCount());
        assertEquals(start, path.nodeAt(0));
        assertEquals(goal, path.nodeAt(path.nodeCount() - 1));
    }

    @Test
    void pathBlockStoresSurfacePathWhenLayerProvidesSurfaceGeometry() {
        BlockPosition startFeet = new BlockPosition(0, 64, 0);
        BlockPosition startSupport = new BlockPosition(0, 63, 0);
        BlockPosition goalSlab = new BlockPosition(1, 63, 0);
        TravelerCommandModule module = new TravelerCommandModule(new TestSurfaceWorldLayer(Map.of(
                startSupport, surfaceBlock(BlockShape.fullCube()),
                goalSlab, surfaceBlock(BlockShape.bottomSlab()))));

        dispatchAndDrain(module, new TestSource(startFeet), "traveler path block 1 63 0");

        PathfinderDebugSnapshot snapshot = module.debugState().latestSnapshot().orElseThrow();
        assertTrue(snapshot.hasSurfaceNodes());
        assertEquals(63.5, snapshot.surfaceNodes().getLast().floorY());
    }

    @Test
    void pathBlockSearchesCapturedMinecraftSnapshot() {
        SnapshotOnlyBlockGetter blockGetter = new SnapshotOnlyBlockGetter();
        TravelerCommandModule module = new TravelerCommandModule(new PathfinderDebugState(), () -> blockGetter);

        CommandResult result = dispatchAndDrain(
                module,
                new TestSource(new BlockPosition(0, 64, 0)), "traveler path block 2 63 0");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(blockGetter.readDuringCapture);
        assertTrue(module.debugState().latestSnapshot().orElseThrow().hasSurfaceNodes());
    }

    @Test
    void pathBlockStoresSmoothedSurfacePathWhenSurfaceLineIsClear() {
        BlockPosition startFeet = new BlockPosition(0, 64, 0);
        TravelerCommandModule module = new TravelerCommandModule(new TestSurfaceWorldLayer(flatSurface(0, 4, -1, 1)));

        dispatchAndDrain(module, new TestSource(startFeet), "traveler path block 4 63 0");

        PathfinderDebugSnapshot snapshot = module.debugState().latestSnapshot().orElseThrow();
        assertTrue(snapshot.hasSurfaceNodes());
        assertEquals(2, snapshot.surfaceNodes().size());
        assertEquals(0.75, snapshot.surfaceNodes().getFirst().centerX());
        assertEquals(4.75, snapshot.surfaceNodes().getLast().centerX());
    }

    @Test
    void smoothedSurfacePathPreservesVerticalMovementLandmarks() {
        BlockPosition startFeet = new BlockPosition(0, 64, 0);
        TravelerCommandModule module = new TravelerCommandModule(new TestSurfaceWorldLayer(Map.of(
                new BlockPosition(0, 63, 0), surfaceBlock(BlockShape.fullCube()),
                new BlockPosition(1, 63, 0), surfaceBlock(BlockShape.fullCube()),
                new BlockPosition(2, 64, 0), surfaceBlock(BlockShape.fullCube()),
                new BlockPosition(3, 64, 0), surfaceBlock(BlockShape.fullCube()))));

        dispatchAndDrain(module, new TestSource(startFeet), "traveler path block 3 64 0");

        PathfinderDebugSnapshot snapshot = module.debugState().latestSnapshot().orElseThrow();
        assertTrue(snapshot.hasSurfaceNodes());
        assertTrue(snapshot.surfaceNodes().size() > 2);
        assertTrue(snapshot.surfaceNodes().stream()
                .anyMatch(node -> node.blockPosition().x() == 2 && node.blockPosition().y() == 64));
    }

    @Test
    void smoothedSurfacePathKeepsDistanceFromAdjacentBlockedBodySpace() {
        BlockPosition startFeet = new BlockPosition(0, 64, 0);
        TravelerCommandModule module = new TravelerCommandModule(new TestSurfaceWorldLayer(walledSurface()));

        dispatchAndDrain(module, new TestSource(startFeet), "traveler path block 4 63 0");

        PathfinderDebugSnapshot snapshot = module.debugState().latestSnapshot().orElseThrow();
        assertTrue(snapshot.hasSurfaceNodes());
        assertTrue(snapshot.surfaceNodes().size() > 2);
    }

    @Test
    void debugStatusPrintsNavigationAndPathStateToChat() {
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerCommandModule module = new TravelerCommandModule(debugState, () -> null);
        TestSource source = new TestSource();
        module.framework().dispatch(source, "traveler path test");
        debugState.updateNavigation(navigationInput(), navigationFrame());

        CommandResult result = module.framework().dispatch(source, "traveler debug status");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        String reply = source.replies().getLast();
        assertTrue(reply.startsWith("traveler debug\n"));
        assertTrue(reply.contains("nav phase=APPROACH"));
        assertTrue(reply.contains("path status=FOUND"));
        assertTrue(reply.contains("pos=(0.00,64.00,0.00)"));
        assertTrue(reply.contains("camera currentYaw=0.0"));
        assertTrue(reply.contains("anomalies=none"));
        assertTrue(result.reply().orElseThrow().contains("nav phase=APPROACH"));
    }

    @Test
    void debugClearPrintsToChatAndClearsDebugState() {
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerCommandModule module = new TravelerCommandModule(debugState, () -> null);
        TestSource source = new TestSource();
        module.framework().dispatch(source, "traveler path test");
        debugState.updateNavigation(navigationInput(), navigationFrame());

        CommandResult result = module.framework().dispatch(source, "traveler debug clear");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(result.reply().orElseThrow().contains("debug cleared"));
        assertTrue(debugState.latestSnapshot().isEmpty());
        assertTrue(debugState.latestNavigation().isEmpty());
    }

    private static void assertPathAvoids(GraphPath<BlockPosition> path, BlockPosition blocked) {
        for (BlockPosition node : path) {
            assertTrue(!node.equals(blocked));
        }
    }

    private static CommandResult dispatchAndDrain(TravelerCommandModule module, TestSource source, String command) {
        CommandResult result = module.framework().dispatch(source, command);
        waitForJobs(module, () -> module.debugState().latestResult().isPresent());
        return result;
    }

    private static void waitForJobs(TravelerCommandModule module, BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(2L).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            module.drainPathJobs();
            Thread.onSpinWait();
        }
        assertTrue(condition.getAsBoolean());
    }

    private static NavigationFrameInput navigationInput() {
        return new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);
    }

    private static NavigationControlFrame navigationFrame() {
        MovementIntent intent = new MovementIntent(true, false, false, false, false, true);
        MovementTarget target = MovementTarget.follow(new NavigationPoint(0.0, 64.0, 4.0));
        NavigationFramePlan plan = new NavigationFramePlan(
                NavigationPhase.APPROACH,
                PathProgress.start(),
                target,
                new MovementVectorIntent(new HorizontalVector(0.0, 1.0), PlannedMovementMode.DIRECT, true),
                new CameraAngles(0.0, 0.0),
                ActionIntent.none(),
                new SpeedIntent(1.0, true),
                ToleranceProfile.standard(),
                LocomotionExecutionState.start(),
                false);
        return new NavigationControlFrame(
                new NavigationControllerState(PathProgress.start(), intent, LocomotionExecutionState.start()),
                intent,
                new CameraAngles(0.0, 0.0),
                target,
                plan,
                false);
    }

    private static final class TestSource implements CommandSource, TravelerCommandPosition {
        private final List<String> replies = new ArrayList<>();
        private final BlockPosition position;

        private TestSource() {
            this(null);
        }

        private TestSource(BlockPosition position) {
            this.position = position;
        }

        private List<String> replies() {
            return List.copyOf(replies);
        }

        @Override
        public Optional<TravelerCommandBlockPosition> blockPosition() {
            return Optional.ofNullable(position)
                    .map(pos -> new TravelerCommandBlockPosition(pos.x(), pos.y(), pos.z()));
        }

        @Override
        public <T> Optional<T> unwrap(Class<T> type) {
            if (!type.isInstance(this)) {
                return Optional.empty();
            }
            return Optional.of(type.cast(this));
        }

        @Override
        public void reply(String message) {
            replies.add(message);
        }
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

    private static SurfaceBlock surfaceBlock(BlockShape shape) {
        return SurfaceBlock.solid(shape);
    }

    private static Map<BlockPosition, SurfaceBlock> walledSurface() {
        Map<BlockPosition, SurfaceBlock> blocks = flatSurface(0, 4, 0, 1);
        for (int x = 0; x <= 4; x++) {
            blocks.put(new BlockPosition(x, 64, 1), surfaceBlock(BlockShape.fullCube()));
        }
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> flatSurface(int minX, int maxX, int minZ, int maxZ) {
        Map<BlockPosition, SurfaceBlock> blocks = new java.util.HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            addFlatRow(blocks, x, minZ, maxZ);
        }
        return blocks;
    }

    private static void addFlatRow(Map<BlockPosition, SurfaceBlock> blocks, int x, int minZ, int maxZ) {
        for (int z = minZ; z <= maxZ; z++) {
            blocks.put(new BlockPosition(x, 63, z), surfaceBlock(BlockShape.fullCube()));
        }
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }

    private static final class SnapshotOnlyBlockGetter extends AbstractTestBlockGetter {
        private boolean readDuringCapture;

        @Override
        public BlockState getBlockState(BlockPos position) {
            requireSnapshotCapture();
            readDuringCapture = true;
            if (position.getY() == 63) {
                return Blocks.STONE.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos position) {
            return getBlockState(position).getFluidState();
        }

        private static void requireSnapshotCapture() {
            boolean captureStack = StackWalker.getInstance().walk(frames -> frames.anyMatch(
                    frame -> frame.getClassName().endsWith("ImmutableMinecraftWorldSnapshot")));
            if (!captureStack) {
                throw new AssertionError("live minecraft world read outside immutable path snapshot capture");
            }
        }
    }
}

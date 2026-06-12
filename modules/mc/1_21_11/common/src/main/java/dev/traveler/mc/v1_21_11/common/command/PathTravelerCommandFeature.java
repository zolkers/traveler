package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommandCatalog;
import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.command.TravelerCommandFeature;
import dev.traveler.core.command.TravelerCommandResult;
import dev.traveler.core.command.TravelerCommandRoute;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.world.BlockPosition;
import dev.traveler.core.world.FluidHandling;
import dev.traveler.mc.v1_21_11.common.adapter.MinecraftWorldSnapshot;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.BlockGetter;

public final class PathTravelerCommandFeature implements TravelerCommandFeature {
    private static final BlockPosition TEST_START = new BlockPosition(0, 64, 0);
    private static final BlockPosition TEST_GOAL = new BlockPosition(3, 64, 0);

    private final PathfinderDebugState debugState;
    private final Supplier<? extends BlockGetter> blockGetterSupplier;

    public PathTravelerCommandFeature(
            PathfinderDebugState debugState,
            Supplier<? extends BlockGetter> blockGetterSupplier) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.blockGetterSupplier = Objects.requireNonNull(blockGetterSupplier, "blockGetterSupplier");
    }

    @Override
    public void register(TravelerCommandCatalog.Builder registry) {
        registry.add(new TravelerCommandRoute(
                "traveler path test",
                "Runs a Traveler path debug search",
                this::pathTest));
        registry.add(new TravelerCommandRoute(
                "traveler path block <x:int> <y:int> <z:int>",
                "Runs a Traveler path debug search for a block",
                this::pathBlock));
    }

    private TravelerCommandResult pathTest(TravelerCommandContext context) {
        PathfinderResult<BlockPosition> result = findPath(TEST_START, TEST_GOAL);
        String message = "path test status=" + result.status() + " nodes=" + result.path().nodeCount();
        debugState.update(result, message);
        return TravelerCommandResult.success(message);
    }

    private TravelerCommandResult pathBlock(TravelerCommandContext context) {
        BlockPosition target = new BlockPosition(
                context.arg("x", int.class),
                context.arg("y", int.class),
                context.arg("z", int.class));
        BlockPosition start = startPosition(context, target);
        PathfinderResult<BlockPosition> result = findPath(start, target);
        String message = blockMessage(target, result);
        debugState.update(result, message);
        return TravelerCommandResult.success(message);
    }

    private static BlockPosition startPosition(TravelerCommandContext context, BlockPosition target) {
        return context.source()
                .unwrap(TravelerCommandPosition.class)
                .flatMap(TravelerCommandPosition::blockPosition)
                .map(TravelerCommandBlockPosition::toCorePosition)
                .orElse(target.above());
    }

    private String blockMessage(BlockPosition target, PathfinderResult<BlockPosition> result) {
        BlockGetter blockGetter = blockGetterSupplier.get();
        if (blockGetter == null) {
            return "path block " + format(target) + " status=" + result.status() + " world=unavailable";
        }
        MinecraftWorldSnapshot snapshot = new MinecraftWorldSnapshot(blockGetter);
        WorldLayer layer = snapshot;
        BlockClassification classification = layer.classify(target);
        return "path block "
                + format(target)
                + " status="
                + result.status()
                + " passability="
                + classification.passability()
                + " fluid="
                + (classification.fluidHandling() == FluidHandling.ALLOW);
    }

    private PathfinderResult<BlockPosition> findPath(BlockPosition start, BlockPosition goal) {
        PathfinderRequest<BlockPosition> request =
                new PathfinderRequest<>(new DirectBlockGraph(goal), start, goal, PathTravelerCommandFeature::distance);
        return new AStarPathfinder<BlockPosition>().search(request);
    }

    private static double distance(BlockPosition from, BlockPosition to) {
        return Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y()) + Math.abs(from.z() - to.z());
    }

    private static String format(BlockPosition position) {
        return position.x() + "," + position.y() + "," + position.z();
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
            return List.of(new Connection<>(node, goal, distance(node, goal)));
        }
    }
}

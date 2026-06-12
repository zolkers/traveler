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
import dev.traveler.core.world.BlockTraversalGraph;
import dev.traveler.core.world.BlockPosition;
import dev.traveler.core.world.FluidHandling;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class PathTravelerCommandFeature implements TravelerCommandFeature {
    private static final BlockPosition TEST_START = new BlockPosition(0, 64, 0);
    private static final BlockPosition TEST_GOAL = new BlockPosition(3, 64, 0);
    private static final int SEARCH_HORIZONTAL_MARGIN = 24;
    private static final int SEARCH_VERTICAL_MARGIN = 8;

    private final PathfinderDebugState debugState;
    private final Supplier<? extends WorldLayer> worldLayerSupplier;

    public PathTravelerCommandFeature(
            PathfinderDebugState debugState,
            Supplier<? extends WorldLayer> worldLayerSupplier) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.worldLayerSupplier = Objects.requireNonNull(worldLayerSupplier, "worldLayerSupplier");
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
        WorldLayer worldLayer = worldLayerSupplier.get();
        BlockPosition goal = goalPosition(worldLayer, target);
        BlockPosition start = startPosition(context, goal);
        PathfinderResult<BlockPosition> result = findPath(worldLayer, start, goal);
        String message = blockMessage(worldLayer, target, result);
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

    private String blockMessage(WorldLayer worldLayer, BlockPosition target, PathfinderResult<BlockPosition> result) {
        if (worldLayer == null) {
            return "path block " + format(target) + " status=" + result.status() + " world=unavailable";
        }
        BlockClassification classification = worldLayer.classify(target);
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
        return findPath(null, start, goal);
    }

    private PathfinderResult<BlockPosition> findPath(WorldLayer worldLayer, BlockPosition start, BlockPosition goal) {
        Graph<BlockPosition> graph = graphFor(worldLayer, start, goal);
        PathfinderRequest<BlockPosition> request =
                new PathfinderRequest<>(graph, start, goal, PathTravelerCommandFeature::distance);
        return new AStarPathfinder<BlockPosition>().search(request);
    }

    private static Graph<BlockPosition> graphFor(WorldLayer worldLayer, BlockPosition start, BlockPosition goal) {
        if (worldLayer == null) {
            return new DirectBlockGraph(goal);
        }
        return new BlockTraversalGraph(
                worldLayer, start, goal, SEARCH_HORIZONTAL_MARGIN, SEARCH_VERTICAL_MARGIN);
    }

    private static BlockPosition goalPosition(WorldLayer worldLayer, BlockPosition target) {
        if (worldLayer == null) {
            return target;
        }
        if (worldLayer.classify(target).passability() == dev.traveler.core.world.BlockPassability.SOLID) {
            return target.above();
        }
        return target;
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

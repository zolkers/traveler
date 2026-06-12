package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommand;
import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.command.TravelerCommandResult;
import dev.traveler.core.command.TravelerSubcommand;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.smooth.PathSmoother;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.navigation.BlockLineOfWalk;
import dev.traveler.core.world.navigation.BlockTraversalGraph;
import dev.traveler.core.world.navigation.SurfaceLineOfWalk;
import dev.traveler.core.world.navigation.SurfaceSmoothingPolicy;
import dev.traveler.core.world.navigation.SurfaceTraversalGraph;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@TravelerCommand(root = "traveler path")
public final class PathTravelerCommandFeature {
    private static final BlockPosition TEST_START = new BlockPosition(0, 64, 0);
    private static final BlockPosition TEST_GOAL = new BlockPosition(3, 64, 0);
    private static final int SEARCH_HORIZONTAL_MARGIN = 24;
    private static final int SEARCH_VERTICAL_MARGIN = 8;
    private static final MovementCapabilities CLIENT_CAPABILITIES =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    private final PathfinderDebugState debugState;
    private final Supplier<? extends WorldLayer> worldLayerSupplier;

    public PathTravelerCommandFeature(
            PathfinderDebugState debugState,
            Supplier<? extends WorldLayer> worldLayerSupplier) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.worldLayerSupplier = Objects.requireNonNull(worldLayerSupplier, "worldLayerSupplier");
    }

    @TravelerSubcommand(route = "test", description = "Runs a Traveler path debug search")
    private TravelerCommandResult pathTest(TravelerCommandContext context) {
        PathfinderResult<BlockPosition> result = findPath(TEST_START, TEST_GOAL);
        String message = "path test status=" + result.status() + " nodes=" + result.path().nodeCount();
        debugState.update(result, message);
        return TravelerCommandResult.success(message);
    }

    @TravelerSubcommand(
            route = "block <x:int> <y:int> <z:int>",
            description = "Runs a Traveler path debug search for a block")
    private TravelerCommandResult pathBlock(TravelerCommandContext context) {
        BlockPosition target = new BlockPosition(
                context.arg("x", int.class),
                context.arg("y", int.class),
                context.arg("z", int.class));
        WorldLayer worldLayer = worldLayerSupplier.get();
        if (worldLayer instanceof SurfaceWorldLayer surfaceWorldLayer) {
            return pathSurfaceBlock(context, surfaceWorldLayer, target);
        }
        BlockPosition goal = goalPosition(worldLayer, target);
        BlockPosition start = startPosition(context, goal);
        PathfinderResult<BlockPosition> result = findPath(worldLayer, start, goal);
        String message = blockMessage(worldLayer, target, result.status());
        debugState.update(result, message);
        return TravelerCommandResult.success(message);
    }

    private TravelerCommandResult pathSurfaceBlock(
            TravelerCommandContext context, SurfaceWorldLayer worldLayer, BlockPosition target) {
        BlockPosition start = startPosition(context, target);
        PathfinderResult<SurfaceNode> result = findSurfacePath(worldLayer, start, target);
        String message = blockMessage(worldLayer, target, result.status());
        debugState.updateSurface(result, message);
        return TravelerCommandResult.success(message);
    }

    private static BlockPosition startPosition(TravelerCommandContext context, BlockPosition target) {
        return context.source()
                .unwrap(TravelerCommandPosition.class)
                .flatMap(TravelerCommandPosition::blockPosition)
                .map(TravelerCommandBlockPosition::toCorePosition)
                .orElse(target.above());
    }

    private String blockMessage(WorldLayer worldLayer, BlockPosition target, PathfinderStatus status) {
        if (worldLayer == null) {
            return "path block " + format(target) + " status=" + status + " world=unavailable";
        }
        BlockClassification classification = worldLayer.classify(target);
        return "path block "
                + format(target)
                + " status="
                + status
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
        PathfinderResult<BlockPosition> result = new AStarPathfinder<BlockPosition>().search(request);
        return smoothedResult(worldLayer, result);
    }

    private PathfinderResult<SurfaceNode> findSurfacePath(
            SurfaceWorldLayer worldLayer, BlockPosition start, BlockPosition target) {
        SurfaceNodeResolver resolver = new SurfaceNodeResolver(worldLayer);
        Optional<SurfaceNode> startNode = resolver.standingSurface(start);
        Optional<SurfaceNode> goalNode = surfaceGoal(resolver, target);
        if (startNode.isEmpty() || goalNode.isEmpty()) {
            return surfaceNotFound();
        }
        SurfaceNode resolvedStart = startNode.orElseThrow();
        SurfaceNode resolvedGoal = goalNode.orElseThrow();
        Graph<SurfaceNode> graph = new SurfaceTraversalGraph(
                worldLayer,
                resolvedStart,
                resolvedGoal,
                CLIENT_CAPABILITIES,
                SEARCH_HORIZONTAL_MARGIN,
                SEARCH_VERTICAL_MARGIN);
        PathfinderRequest<SurfaceNode> request = new PathfinderRequest<>(
                graph, resolvedStart, resolvedGoal, PathTravelerCommandFeature::surfaceDistance);
        PathfinderResult<SurfaceNode> result = new AStarPathfinder<SurfaceNode>().search(request);
        return smoothedSurfaceResult(worldLayer, result);
    }

    private static Graph<BlockPosition> graphFor(WorldLayer worldLayer, BlockPosition start, BlockPosition goal) {
        if (worldLayer == null) {
            return new DirectBlockGraph(goal);
        }
        return new BlockTraversalGraph(
                worldLayer, start, goal, SEARCH_HORIZONTAL_MARGIN, SEARCH_VERTICAL_MARGIN);
    }

    private static Optional<SurfaceNode> surfaceGoal(SurfaceNodeResolver resolver, BlockPosition target) {
        Optional<SurfaceNode> centeredSurface = resolver.centeredSurface(target);
        if (centeredSurface.isPresent()) {
            return centeredSurface;
        }
        return resolver.standingSurface(target);
    }

    private static PathfinderResult<SurfaceNode> surfaceNotFound() {
        return new PathfinderResult<>(PathfinderStatus.NOT_FOUND, new MutableGraphPath<>());
    }

    private static BlockPosition goalPosition(WorldLayer worldLayer, BlockPosition target) {
        if (worldLayer == null) {
            return target;
        }
        if (worldLayer.classify(target).passability() == dev.traveler.core.world.block.BlockPassability.SOLID) {
            return target.above();
        }
        return target;
    }

    private static PathfinderResult<BlockPosition> smoothedResult(
            WorldLayer worldLayer, PathfinderResult<BlockPosition> result) {
        if (worldLayer == null || result.status() != PathfinderStatus.FOUND) {
            return result;
        }
        if (result.path().nodeCount() < 3) {
            return result;
        }
        List<BlockPosition> smoothed =
                new PathSmoother<BlockPosition>(new BlockLineOfWalk(worldLayer)).smooth(result.path().nodes());
        return new PathfinderResult<>(result.status(), graphPath(smoothed, result.path().cost()));
    }

    private static PathfinderResult<SurfaceNode> smoothedSurfaceResult(
            SurfaceWorldLayer worldLayer, PathfinderResult<SurfaceNode> result) {
        if (result.status() != PathfinderStatus.FOUND) {
            return result;
        }
        if (result.path().nodeCount() < 3) {
            return result;
        }
        List<SurfaceNode> nodes = result.path().nodes();
        List<SurfaceNode> smoothed = new PathSmoother<SurfaceNode>(new SurfaceLineOfWalk(
                        worldLayer,
                        nodes.getFirst(),
                        nodes.getLast(),
                        CLIENT_CAPABILITIES,
                        SEARCH_HORIZONTAL_MARGIN,
                        SEARCH_VERTICAL_MARGIN),
                        new SurfaceSmoothingPolicy(worldLayer))
                .smooth(nodes);
        return new PathfinderResult<>(result.status(), graphPath(smoothed, result.path().cost()));
    }

    private static <N> MutableGraphPath<N> graphPath(List<N> nodes, double cost) {
        MutableGraphPath<N> path = new MutableGraphPath<>();
        nodes.forEach(path::addNode);
        path.setCost(cost);
        return path;
    }

    private static double distance(BlockPosition from, BlockPosition to) {
        int deltaX = Math.abs(from.x() - to.x());
        int deltaZ = Math.abs(from.z() - to.z());
        int straight = Math.max(deltaX, deltaZ) - Math.min(deltaX, deltaZ);
        return straight + Math.min(deltaX, deltaZ) * Math.sqrt(2.0) + Math.abs(from.y() - to.y()) * 0.5;
    }

    private static double surfaceDistance(SurfaceNode from, SurfaceNode to) {
        double deltaX = Math.abs(from.centerX() - to.centerX());
        double deltaZ = Math.abs(from.centerZ() - to.centerZ());
        return Math.hypot(deltaX, deltaZ) + Math.abs(from.floorY() - to.floorY()) * 0.5;
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

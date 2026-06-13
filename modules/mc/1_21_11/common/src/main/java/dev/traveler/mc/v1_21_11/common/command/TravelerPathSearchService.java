package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.job.PathJob;
import dev.traveler.core.job.PathJobState;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.smooth.PathSmoother;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.movement.MovementProfiles;
import dev.traveler.core.world.navigation.BlockLineOfWalk;
import dev.traveler.core.world.navigation.BlockTraversalGraph;
import dev.traveler.core.world.navigation.SurfaceLineOfWalk;
import dev.traveler.core.world.navigation.SurfaceLineOfWalkSettings;
import dev.traveler.core.world.navigation.SurfaceSmoothingPolicy;
import dev.traveler.core.world.navigation.SurfaceTraversalGraph;
import dev.traveler.core.world.navigation.SurfaceTraversalGraphSettings;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import dev.traveler.mc.v1_21_11.common.adapter.world.ImmutableMinecraftWorldSnapshot;
import dev.traveler.mc.v1_21_11.common.adapter.world.MinecraftWorldSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

final class TravelerPathSearchService {
    private static final BlockPosition TEST_START = new BlockPosition(0, 64, 0);
    private static final BlockPosition TEST_GOAL = new BlockPosition(3, 64, 0);
    private static final int SEARCH_HORIZONTAL_MARGIN = 24;
    private static final int SEARCH_VERTICAL_MARGIN = 8;
    private static final long MAX_MINECRAFT_SNAPSHOT_BLOCKS = 262_144L;
    private static final MovementCapabilities CLIENT_CAPABILITIES =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);
    private static final MovementProfile CLIENT_PROFILE = MovementProfiles.defaultPlayerWith(CLIENT_CAPABILITIES);

    private final Supplier<? extends WorldLayer> worldLayerSupplier;

    TravelerPathSearchService(Supplier<? extends WorldLayer> worldLayerSupplier) {
        this.worldLayerSupplier = Objects.requireNonNull(worldLayerSupplier, "worldLayerSupplier");
    }

    TravelerPathSearchResult testPath() {
        PathfinderResult<BlockPosition> result = findPath(null, TEST_START, TEST_GOAL);
        String message = "path test status=" + result.status() + " nodes=" + result.path().nodeCount();
        return new TravelerPathSearchResult(result, Optional.empty(), message);
    }

    TravelerPathSearchSubmission blockPathSubmission(
            TravelerCommandContext context, BlockPosition target, String purpose) {
        BlockPosition start = startPosition(context, target);
        WorldLayer worldLayer = worldLayerSupplier.get();
        Optional<TravelerPathSearchResult> rejection = oversizedMinecraftSnapshot(worldLayer, start, target);
        if (rejection.isPresent()) {
            return TravelerPathSearchSubmission.immediate(rejection.orElseThrow());
        }
        if (worldLayer instanceof MinecraftWorldSnapshot minecraftWorldLayer) {
            return TravelerPathSearchSubmission.snapshot(
                    new SnapshotBlockSearch(minecraftWorldLayer, start, target, purpose));
        }
        return TravelerPathSearchSubmission.queued(pathJob(worldLayer, start, target, purpose));
    }

    private static Optional<TravelerPathSearchResult> oversizedMinecraftSnapshot(
            WorldLayer worldLayer, BlockPosition start, BlockPosition target) {
        if (!(worldLayer instanceof MinecraftWorldSnapshot)) {
            return Optional.empty();
        }
        SearchVolume volume = SearchVolume.around(start, target);
        if (volume.blockCount() <= MAX_MINECRAFT_SNAPSHOT_BLOCKS) {
            return Optional.empty();
        }
        return Optional.of(rejectedSearch(target, volume));
    }

    private static TravelerPathSearchResult rejectedSearch(BlockPosition target, SearchVolume volume) {
        PathfinderResult<BlockPosition> result =
                new PathfinderResult<>(PathfinderStatus.NOT_FOUND, new MutableGraphPath<>());
        String message = "path block "
                + format(target)
                + " status=NOT_FOUND reason=search-too-large estimatedBlocks="
                + volume.blockCount()
                + " limit="
                + MAX_MINECRAFT_SNAPSHOT_BLOCKS;
        return new TravelerPathSearchResult(result, Optional.empty(), message);
    }

    private static PathJob<TravelerPathSearchResult> pathJob(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target,
            String purpose) {
        return new PathJob<>(
                purpose,
                () -> searchBlockPath(worldLayer, start, target),
                TravelerPathSearchService::jobState);
    }

    private static TravelerPathSearchResult searchBlockPath(
            WorldLayer worldLayer, BlockPosition start, BlockPosition target) {
        if (worldLayer instanceof SurfaceWorldLayer surfaceWorldLayer) {
            return surfaceBlockPath(start, surfaceWorldLayer, target);
        }
        BlockPosition goal = goalPosition(worldLayer, target);
        PathfinderResult<BlockPosition> result = findPath(worldLayer, start, goal);
        String message = blockMessage(worldLayer, target, result.status());
        return new TravelerPathSearchResult(result, Optional.empty(), message);
    }

    private static TravelerPathSearchResult surfaceBlockPath(
            BlockPosition start, SurfaceWorldLayer worldLayer, BlockPosition target) {
        PathfinderResult<SurfaceNode> surfaceResult = findSurfacePath(worldLayer, start, target);
        PathfinderResult<BlockPosition> blockResult = surfaceResultToBlockResult(surfaceResult);
        String message = blockMessage(worldLayer, target, surfaceResult.status());
        return new TravelerPathSearchResult(blockResult, Optional.of(surfaceResult), message);
    }

    private static BlockPosition startPosition(TravelerCommandContext context, BlockPosition target) {
        return context.source()
                .unwrap(TravelerCommandPosition.class)
                .flatMap(TravelerCommandPosition::blockPosition)
                .map(TravelerCommandBlockPosition::toCorePosition)
                .orElse(target.above());
    }

    private static String blockMessage(WorldLayer worldLayer, BlockPosition target, PathfinderStatus status) {
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

    private static PathfinderResult<BlockPosition> findPath(
            WorldLayer worldLayer, BlockPosition start, BlockPosition goal) {
        Graph<BlockPosition> graph = graphFor(worldLayer, start, goal);
        PathfinderRequest<BlockPosition> request =
                new PathfinderRequest<>(graph, start, goal, TravelerPathSearchService::distance);
        PathfinderResult<BlockPosition> result = new AStarPathfinder<BlockPosition>().search(request);
        return smoothedResult(worldLayer, result);
    }

    private static PathfinderResult<SurfaceNode> findSurfacePath(
            SurfaceWorldLayer worldLayer, BlockPosition start, BlockPosition target) {
        SurfaceNodeResolver resolver = new SurfaceNodeResolver(worldLayer);
        List<SurfaceNode> startNodes = resolver.standingSurfaces(start);
        List<SurfaceNode> goalNodes = surfaceGoals(resolver, target);
        if (startNodes.isEmpty() || goalNodes.isEmpty()) {
            return surfaceNotFound();
        }
        Optional<PathfinderResult<SurfaceNode>> preferred =
                preferredSurfacePath(worldLayer, start, target, startNodes, goalNodes);
        if (preferred.isPresent()) {
            return preferred.orElseThrow();
        }
        return bestSurfacePath(worldLayer, startNodes, goalNodes);
    }

    private static PathfinderResult<SurfaceNode> searchSurfacePath(
            SurfaceWorldLayer worldLayer, SurfaceNode start, SurfaceNode goal) {
        Graph<SurfaceNode> graph = new SurfaceTraversalGraph(
                worldLayer,
                start,
                goal,
                CLIENT_PROFILE,
                SurfaceTraversalGraphSettings.standard(SEARCH_HORIZONTAL_MARGIN, SEARCH_VERTICAL_MARGIN));
        PathfinderRequest<SurfaceNode> request =
                new PathfinderRequest<>(graph, start, goal, TravelerPathSearchService::surfaceDistance);
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

    private static List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver, BlockPosition target) {
        List<SurfaceNode> targetSurfaces = resolver.surfaces(target);
        if (!targetSurfaces.isEmpty()) {
            return targetSurfaces;
        }
        return resolver.standingSurface(target).map(List::of).orElseGet(List::of);
    }

    private static Optional<PathfinderResult<SurfaceNode>> preferredSurfacePath(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target,
            List<SurfaceNode> starts,
            List<SurfaceNode> goals) {
        PathfinderResult<SurfaceNode> result = searchSurfacePath(
                worldLayer,
                nearestSurface(starts, start),
                nearestSurface(goals, target));
        if (!isFound(result)) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private static SurfaceNode nearestSurface(List<SurfaceNode> nodes, BlockPosition position) {
        return nodes.stream()
                .min(Comparator.comparingDouble(node -> centerDistance(node, position)))
                .orElseThrow();
    }

    private static double centerDistance(SurfaceNode node, BlockPosition position) {
        double centerX = position.x() + 0.5;
        double centerZ = position.z() + 0.5;
        return Math.hypot(node.centerX() - centerX, node.centerZ() - centerZ);
    }

    private static PathfinderResult<SurfaceNode> bestSurfacePath(
            SurfaceWorldLayer worldLayer, List<SurfaceNode> starts, List<SurfaceNode> goals) {
        List<PathfinderResult<SurfaceNode>> results = new ArrayList<>(starts.size() * goals.size());
        for (SurfaceNode start : starts) {
            addSurfacePathResults(worldLayer, start, goals, results);
        }
        return bestFoundSurfacePath(results);
    }

    private static void addSurfacePathResults(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            List<SurfaceNode> goals,
            List<PathfinderResult<SurfaceNode>> results) {
        for (SurfaceNode goal : goals) {
            results.add(searchSurfacePath(worldLayer, start, goal));
        }
    }

    private static PathfinderResult<SurfaceNode> bestFoundSurfacePath(List<PathfinderResult<SurfaceNode>> results) {
        return results.stream()
                .filter(TravelerPathSearchService::isFound)
                .min(TravelerPathSearchService::comparePathCost)
                .orElseGet(TravelerPathSearchService::surfaceNotFound);
    }

    private static boolean isFound(PathfinderResult<SurfaceNode> result) {
        return result.status() == PathfinderStatus.FOUND;
    }

    private static int comparePathCost(
            PathfinderResult<SurfaceNode> first,
            PathfinderResult<SurfaceNode> second) {
        return Double.compare(first.path().cost(), second.path().cost());
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

    private static PathfinderResult<BlockPosition> surfaceResultToBlockResult(PathfinderResult<SurfaceNode> result) {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        for (SurfaceNode node : result.path()) {
            path.addNode(node.renderBlockPosition());
        }
        path.setCost(result.path().cost());
        return new PathfinderResult<>(result.status(), path);
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
                        CLIENT_PROFILE,
                        SurfaceLineOfWalkSettings.smoothing(SEARCH_HORIZONTAL_MARGIN, SEARCH_VERTICAL_MARGIN)),
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

    private static PathJobState jobState(TravelerPathSearchResult result) {
        if (result.status() == PathfinderStatus.FOUND) {
            return PathJobState.FOUND;
        }
        return PathJobState.NOT_FOUND;
    }

    private record SearchVolume(long width, long height, long depth) {
        private static SearchVolume around(BlockPosition start, BlockPosition target) {
            long width = span(start.x(), target.x(), SEARCH_HORIZONTAL_MARGIN);
            long height = span(start.y(), target.y(), SEARCH_VERTICAL_MARGIN);
            long depth = span(start.z(), target.z(), SEARCH_HORIZONTAL_MARGIN);
            return new SearchVolume(width, height, depth);
        }

        private long blockCount() {
            return Math.multiplyExact(Math.multiplyExact(width, height), depth);
        }

        private static long span(int first, int second, int margin) {
            return Math.abs((long) first - second) + margin * 2L + 1L;
        }
    }

    static final class SnapshotBlockSearch {
        private final ImmutableMinecraftWorldSnapshot.CaptureSession captureSession;
        private final BlockPosition start;
        private final BlockPosition target;
        private final String purpose;

        private SnapshotBlockSearch(
                MinecraftWorldSnapshot worldLayer,
                BlockPosition start,
                BlockPosition target,
                String purpose) {
            this.captureSession = ImmutableMinecraftWorldSnapshot.captureSession(
                    worldLayer,
                    start,
                    target,
                    SEARCH_HORIZONTAL_MARGIN,
                    SEARCH_VERTICAL_MARGIN);
            this.start = Objects.requireNonNull(start, "start");
            this.target = Objects.requireNonNull(target, "target");
            this.purpose = Objects.requireNonNull(purpose, "purpose");
        }

        boolean captureNext(int blockBudget, long deadlineNanos) {
            return captureSession.captureNext(blockBudget, deadlineNanos);
        }

        long blockCount() {
            return captureSession.blockCount();
        }

        long capturedBlocks() {
            return captureSession.capturedBlocks();
        }

        PathJob<TravelerPathSearchResult> pathJob() {
            return new PathJob<>(
                    purpose,
                    () -> searchBlockPath(captureSession.snapshot(), start, target),
                    TravelerPathSearchService::jobState);
        }
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

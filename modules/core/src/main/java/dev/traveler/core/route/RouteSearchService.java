package dev.traveler.core.route;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.smooth.PathSmoother;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.navigation.BlockLineOfWalk;
import dev.traveler.core.world.navigation.BlockTraversalGraph;
import dev.traveler.core.world.navigation.SurfaceLineOfWalk;
import dev.traveler.core.world.navigation.SurfaceLineOfWalkSettings;
import dev.traveler.core.world.navigation.SurfaceMovementEvaluator;
import dev.traveler.core.world.navigation.SurfaceSmoothingPolicy;
import dev.traveler.core.world.navigation.SurfaceTraversalGraph;
import dev.traveler.core.world.navigation.SurfaceTraversalGraphSettings;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RouteSearchService {
    private final RouteSearchSettings settings;

    public RouteSearchService(RouteSearchSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public RouteSearchResult search(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target) {
        BlockPosition safeStart = Objects.requireNonNull(start, "start");
        BlockPosition safeTarget = Objects.requireNonNull(target, "target");
        if (worldLayer instanceof SurfaceWorldLayer surfaceWorldLayer) {
            return surfaceSearch(surfaceWorldLayer, safeStart, safeTarget);
        }
        return blockSearch(worldLayer, safeStart, safeTarget);
    }

    private RouteSearchResult surfaceSearch(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target) {
        SurfaceNodeResolver resolver = new SurfaceNodeResolver(worldLayer);
        List<SurfaceNode> startNodes = resolver.standingSurfaces(start);
        List<SurfaceNode> goalNodes = surfaceGoals(resolver, target);
        if (startNodes.isEmpty()) {
            return surfaceRejected(goalNodes.size(), RouteSearchDiagnostics::noStartSurface);
        }
        if (goalNodes.isEmpty()) {
            return surfaceRejected(startNodes.size(), RouteSearchDiagnostics::noGoalSurface);
        }
        return surfaceSearch(worldLayer, start, target, startNodes, goalNodes);
    }

    private RouteSearchResult surfaceSearch(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target,
            List<SurfaceNode> startNodes,
            List<SurfaceNode> goalNodes) {
        PathfinderResult<SurfaceNode> surfaceResult =
                findSurfacePath(worldLayer, start, target, startNodes, goalNodes);
        PathfinderResult<BlockPosition> blockResult = surfaceResultToBlockResult(surfaceResult);
        RouteSearchDiagnostics diagnostics = surfaceDiagnostics(
                surfaceResult,
                startNodes.size(),
                goalNodes.size());
        Optional<RoutePath> route = routeFromSurfaceResult(worldLayer, surfaceResult);
        return new RouteSearchResult(blockResult, Optional.of(surfaceResult), route, diagnostics);
    }

    private RouteSearchResult blockSearch(WorldLayer worldLayer, BlockPosition start, BlockPosition target) {
        BlockPosition goal = goalPosition(worldLayer, target);
        PathfinderResult<BlockPosition> result = findPath(worldLayer, start, goal);
        RouteSearchDiagnostics diagnostics = blockDiagnostics(worldLayer, result);
        return new RouteSearchResult(result, Optional.empty(), Optional.empty(), diagnostics);
    }

    private PathfinderResult<BlockPosition> findPath(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition goal) {
        Graph<BlockPosition> graph = graphFor(worldLayer, start, goal);
        PathfinderRequest<BlockPosition> request =
                new PathfinderRequest<>(graph, start, goal, RouteSearchService::distance);
        PathfinderResult<BlockPosition> result = new AStarPathfinder<BlockPosition>().search(request);
        return smoothedResult(worldLayer, result);
    }

    private PathfinderResult<SurfaceNode> findSurfacePath(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target,
            List<SurfaceNode> starts,
            List<SurfaceNode> goals) {
        Optional<PathfinderResult<SurfaceNode>> preferred =
                preferredSurfacePath(worldLayer, start, target, starts, goals);
        if (preferred.isPresent()) {
            return preferred.orElseThrow();
        }
        return bestSurfacePath(worldLayer, starts, goals);
    }

    private Optional<PathfinderResult<SurfaceNode>> preferredSurfacePath(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target,
            List<SurfaceNode> starts,
            List<SurfaceNode> goals) {
        PathfinderResult<SurfaceNode> result = searchSurfacePath(
                worldLayer,
                nearestSurface(starts, start),
                nearestSurface(goals, target));
        if (result.status() != PathfinderStatus.FOUND) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private PathfinderResult<SurfaceNode> bestSurfacePath(
            SurfaceWorldLayer worldLayer,
            List<SurfaceNode> starts,
            List<SurfaceNode> goals) {
        List<PathfinderResult<SurfaceNode>> results = new ArrayList<>(starts.size() * goals.size());
        for (SurfaceNode start : starts) {
            addSurfacePathResults(worldLayer, start, goals, results);
        }
        return bestFoundSurfacePath(results);
    }

    private void addSurfacePathResults(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            List<SurfaceNode> goals,
            List<PathfinderResult<SurfaceNode>> results) {
        for (SurfaceNode goal : goals) {
            results.add(searchSurfacePath(worldLayer, start, goal));
        }
    }

    private PathfinderResult<SurfaceNode> searchSurfacePath(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            SurfaceNode goal) {
        Graph<SurfaceNode> graph = new SurfaceTraversalGraph(
                worldLayer,
                start,
                goal,
                settings.movementProfile(),
                SurfaceTraversalGraphSettings.standard(
                        settings.horizontalMargin(),
                        settings.verticalMargin()));
        PathfinderRequest<SurfaceNode> request =
                new PathfinderRequest<>(graph, start, goal, RouteSearchService::surfaceDistance);
        PathfinderResult<SurfaceNode> result = new AStarPathfinder<SurfaceNode>().search(request);
        return smoothedSurfaceResult(worldLayer, result);
    }

    private Optional<RoutePath> routeFromSurfaceResult(
            SurfaceWorldLayer worldLayer,
            PathfinderResult<SurfaceNode> result) {
        if (result.status() != PathfinderStatus.FOUND || result.path().nodeCount() < 2) {
            return Optional.empty();
        }
        return Optional.of(routeFromNodes(worldLayer, result.path().nodes()));
    }

    private RoutePath routeFromNodes(SurfaceWorldLayer worldLayer, List<SurfaceNode> nodes) {
        List<RouteStep> steps = new ArrayList<>(nodes.size() - 1);
        SurfaceMovementEvaluator evaluator =
                new SurfaceMovementEvaluator(settings.movementProfile().capabilities());
        for (int index = 1; index < nodes.size(); index++) {
            steps.add(routeStep(worldLayer, evaluator, nodes.get(index - 1), nodes.get(index)));
        }
        return RoutePath.of(steps);
    }

    private static RouteStep routeStep(
            SurfaceWorldLayer worldLayer,
            SurfaceMovementEvaluator evaluator,
            SurfaceNode from,
            SurfaceNode to) {
        SurfaceBlock block = worldLayer.surfaceBlock(to.blockPosition());
        MovementDecision decision = evaluator.decision(from, to, block);
        return new RouteStep(from, to, decision.action(), surfaceDistance(from, to));
    }

    private RouteSearchResult surfaceRejected(
            int knownSurfaceCount,
            SurfaceDiagnosticsFactory diagnosticsFactory) {
        PathfinderResult<SurfaceNode> surfaceResult = surfaceNotFound();
        PathfinderResult<BlockPosition> blockResult = surfaceResultToBlockResult(surfaceResult);
        RouteSearchDiagnostics diagnostics = diagnosticsFactory.create(knownSurfaceCount);
        return new RouteSearchResult(blockResult, Optional.of(surfaceResult), Optional.empty(), diagnostics);
    }

    private RouteSearchDiagnostics surfaceDiagnostics(
            PathfinderResult<SurfaceNode> result,
            int startSurfaceCount,
            int goalSurfaceCount) {
        if (result.status() == PathfinderStatus.FOUND) {
            return RouteSearchDiagnostics.none(startSurfaceCount, goalSurfaceCount);
        }
        return RouteSearchDiagnostics.surfaceNotFound(startSurfaceCount, goalSurfaceCount);
    }

    private static RouteSearchDiagnostics blockDiagnostics(
            WorldLayer worldLayer,
            PathfinderResult<BlockPosition> result) {
        if (worldLayer == null) {
            return RouteSearchDiagnostics.worldUnavailable();
        }
        if (result.status() == PathfinderStatus.FOUND) {
            return RouteSearchDiagnostics.none(0, 0);
        }
        return RouteSearchDiagnostics.blockNotFound();
    }

    private Graph<BlockPosition> graphFor(WorldLayer worldLayer, BlockPosition start, BlockPosition goal) {
        if (worldLayer == null) {
            return new DirectBlockGraph(goal);
        }
        return new BlockTraversalGraph(
                worldLayer,
                start,
                goal,
                settings.horizontalMargin(),
                settings.verticalMargin());
    }

    private static List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver, BlockPosition target) {
        List<SurfaceNode> targetSurfaces = resolver.surfaces(target);
        if (!targetSurfaces.isEmpty()) {
            return targetSurfaces;
        }
        return resolver.standingSurface(target).map(List::of).orElseGet(List::of);
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

    private static PathfinderResult<SurfaceNode> bestFoundSurfacePath(
            List<PathfinderResult<SurfaceNode>> results) {
        return results.stream()
                .filter(result -> result.status() == PathfinderStatus.FOUND)
                .min(RouteSearchService::comparePathCost)
                .orElseGet(RouteSearchService::surfaceNotFound);
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
        if (worldLayer.classify(target).passability() == BlockPassability.SOLID) {
            return target.above();
        }
        return target;
    }

    private static PathfinderResult<BlockPosition> surfaceResultToBlockResult(
            PathfinderResult<SurfaceNode> result) {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        for (SurfaceNode node : result.path()) {
            path.addNode(node.renderBlockPosition());
        }
        path.setCost(result.path().cost());
        return new PathfinderResult<>(result.status(), path);
    }

    private static PathfinderResult<BlockPosition> smoothedResult(
            WorldLayer worldLayer,
            PathfinderResult<BlockPosition> result) {
        if (worldLayer == null || result.status() != PathfinderStatus.FOUND) {
            return result;
        }
        if (result.path().nodeCount() < 3) {
            return result;
        }
        List<BlockPosition> smoothed =
                new PathSmoother<BlockPosition>(new BlockLineOfWalk(worldLayer))
                        .smooth(result.path().nodes());
        return new PathfinderResult<>(result.status(), graphPath(smoothed, result.path().cost()));
    }

    private PathfinderResult<SurfaceNode> smoothedSurfaceResult(
            SurfaceWorldLayer worldLayer,
            PathfinderResult<SurfaceNode> result) {
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
                        settings.movementProfile(),
                        SurfaceLineOfWalkSettings.smoothing(
                                settings.horizontalMargin(),
                                settings.verticalMargin())),
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

    @FunctionalInterface
    private interface SurfaceDiagnosticsFactory {
        RouteSearchDiagnostics create(int knownSurfaceCount);
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

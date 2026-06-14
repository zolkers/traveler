package dev.traveler.core.route;

import dev.traveler.core.graph.Graph;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.smooth.PathNodePreservation;
import dev.traveler.core.smooth.PathSmoother;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.navigation.BlockLineOfWalk;
import dev.traveler.core.world.navigation.SurfaceLineOfWalk;
import dev.traveler.core.world.navigation.SurfaceLineOfWalkSettings;
import dev.traveler.core.world.navigation.SurfaceClimbTraversal;
import dev.traveler.core.world.navigation.SurfaceSmoothingPolicy;
import dev.traveler.core.world.navigation.SurfaceTransitionEvaluator;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RouteSearchService {
    private final RouteSearchSettings settings;
    private final RouteSearchComponents components;

    public RouteSearchService(RouteSearchSettings settings) {
        this(settings, RouteSearchComponents.standard());
    }

    public RouteSearchService(RouteSearchSettings settings, RouteSearchComponents components) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.components = Objects.requireNonNull(components, "components");
    }

    public RouteSearchResult search(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target) {
        BlockPosition safeStart = Objects.requireNonNull(start, "start");
        BlockPosition safeTarget = Objects.requireNonNull(target, "target");
        return search(worldLayer, safeStart, RouteGoal.blockTarget(safeTarget));
    }

    public RouteSearchResult search(
            WorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal) {
        BlockPosition safeStart = Objects.requireNonNull(start, "start");
        RouteGoal safeGoal = Objects.requireNonNull(goal, "goal");
        if (worldLayer instanceof SurfaceWorldLayer surfaceWorldLayer) {
            return surfaceSearch(surfaceWorldLayer, safeStart, safeGoal);
        }
        return blockSearch(worldLayer, safeStart, safeGoal);
    }

    private RouteSearchResult surfaceSearch(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal) {
        SurfaceNodeResolver resolver = new SurfaceNodeResolver(worldLayer);
        List<SurfaceNode> startNodes = resolver.standingSurfaces(start);
        List<SurfaceNode> goalNodes = goal.surfaceGoals(resolver);
        if (startNodes.isEmpty()) {
            return surfaceRejected(goal, goalNodes.size(), RouteSearchDiagnostics::noStartSurface);
        }
        if (goalNodes.isEmpty()) {
            return surfaceRejected(goal, startNodes.size(), RouteSearchDiagnostics::noGoalSurface);
        }
        return surfaceSearch(worldLayer, start, goal, startNodes, goalNodes);
    }

    private RouteSearchResult surfaceSearch(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            List<SurfaceNode> startNodes,
            List<SurfaceNode> goalNodes) {
        PathfinderResult<SurfaceNode> surfaceResult =
                findSurfacePath(worldLayer, start, goal, startNodes, goalNodes);
        PathfinderResult<BlockPosition> blockResult = surfaceResultToBlockResult(surfaceResult);
        RouteSearchDiagnostics diagnostics = surfaceDiagnostics(
                surfaceResult,
                startNodes.size(),
                goalNodes.size());
        Optional<RoutePath> route = routeFromSurfaceResult(worldLayer, surfaceResult);
        return new RouteSearchResult(goal, blockResult, Optional.of(surfaceResult), route, diagnostics);
    }

    private RouteSearchResult blockSearch(WorldLayer worldLayer, BlockPosition start, RouteGoal goal) {
        BlockPosition blockGoal = goal.blockGoal(worldLayer);
        PathfinderResult<BlockPosition> result = findPath(worldLayer, start, blockGoal);
        RouteSearchDiagnostics diagnostics = blockDiagnostics(worldLayer, result);
        return new RouteSearchResult(goal, result, Optional.empty(), Optional.empty(), diagnostics);
    }

    private PathfinderResult<BlockPosition> findPath(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition goal) {
        Graph<BlockPosition> graph = graphFor(worldLayer, start, goal);
        PathfinderRequest<BlockPosition> request =
                new PathfinderRequest<>(graph, start, goal, RouteSearchService::distance);
        PathfinderResult<BlockPosition> result = components.blockPathfinder().search(request);
        return smoothedResult(worldLayer, result);
    }

    private PathfinderResult<SurfaceNode> findSurfacePath(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            List<SurfaceNode> starts,
            List<SurfaceNode> goals) {
        Optional<PathfinderResult<SurfaceNode>> preferred =
                preferredSurfacePath(worldLayer, start, goal, starts, goals);
        if (preferred.isPresent()) {
            return preferred.orElseThrow();
        }
        return bestSurfacePath(worldLayer, starts, goals);
    }

    private Optional<PathfinderResult<SurfaceNode>> preferredSurfacePath(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            List<SurfaceNode> starts,
            List<SurfaceNode> goals) {
        PathfinderResult<SurfaceNode> result = searchSurfacePath(
                worldLayer,
                nearestSurface(starts, start),
                nearestSurface(goals, goal.preferredPosition(start)));
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
        Graph<SurfaceNode> graph = components.surfaceGraphFactory().create(worldLayer, start, goal, settings);
        PathfinderRequest<SurfaceNode> request =
                new PathfinderRequest<>(graph, start, goal, RouteSearchService::surfaceDistance);
        PathfinderResult<SurfaceNode> result = components.surfacePathfinder().search(request);
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
        MovementCapabilities capabilities = settings.movementProfile().capabilities();
        SurfaceTransitionEvaluator evaluator = new SurfaceTransitionEvaluator(capabilities);
        for (int index = 1; index < nodes.size(); index++) {
            steps.add(routeStep(worldLayer, evaluator, capabilities, nodes.get(index - 1), nodes.get(index)));
        }
        return RoutePath.of(steps);
    }

    private static RouteStep routeStep(
            SurfaceWorldLayer worldLayer,
            SurfaceTransitionEvaluator evaluator,
            MovementCapabilities capabilities,
            SurfaceNode from,
            SurfaceNode to) {
        MovementDecision decision = evaluator.decision(worldLayer, from, to);
        return new RouteStep(
                from,
                to,
                decision.action(),
                surfaceDistance(from, to),
                targetPoint(worldLayer, capabilities, decision.action(), from, to));
    }

    private static NavigationPoint targetPoint(
            SurfaceWorldLayer worldLayer,
            MovementCapabilities capabilities,
            MovementAction action,
            SurfaceNode from,
            SurfaceNode to) {
        if (action != MovementAction.CLIMB) {
            return pointOf(to);
        }
        return SurfaceClimbTraversal.climbTarget(
                        worldLayer,
                        from,
                        to,
                        capabilities)
                .orElseGet(() -> pointOf(to));
    }

    private RouteSearchResult surfaceRejected(
            RouteGoal goal,
            int knownSurfaceCount,
            SurfaceDiagnosticsFactory diagnosticsFactory) {
        PathfinderResult<SurfaceNode> surfaceResult = surfaceNotFound();
        PathfinderResult<BlockPosition> blockResult = surfaceResultToBlockResult(surfaceResult);
        RouteSearchDiagnostics diagnostics = diagnosticsFactory.create(knownSurfaceCount);
        return new RouteSearchResult(goal, blockResult, Optional.of(surfaceResult), Optional.empty(), diagnostics);
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
        return components.blockGraphFactory().create(worldLayer, start, goal, settings);
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

    private static PathfinderResult<BlockPosition> surfaceResultToBlockResult(
            PathfinderResult<SurfaceNode> result) {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        for (SurfaceNode node : result.path()) {
            path.addNode(node.renderBlockPosition());
        }
        path.setCost(result.path().cost());
        return new PathfinderResult<>(result.status(), path);
    }

    private PathfinderResult<BlockPosition> smoothedResult(
            WorldLayer worldLayer,
            PathfinderResult<BlockPosition> result) {
        if (worldLayer == null || result.status() != PathfinderStatus.FOUND) {
            return result;
        }
        if (result.path().nodeCount() < 3) {
            return result;
        }
        List<BlockPosition> smoothed =
                new PathSmoother<BlockPosition>(
                                new BlockLineOfWalk(worldLayer),
                                PathNodePreservation.none(),
                                components.blockSmoothingSelector())
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
                        new SurfaceSmoothingPolicy(worldLayer, settings.movementProfile().capabilities()),
                        components.surfaceSmoothingSelector())
                .smooth(nodes);
        return new PathfinderResult<>(result.status(), graphPath(smoothed, result.path().cost()));
    }

    private static <N> MutableGraphPath<N> graphPath(List<N> nodes, double cost) {
        MutableGraphPath<N> path = new MutableGraphPath<>();
        nodes.forEach(path::addNode);
        path.setCost(cost);
        return path;
    }

    static double distance(BlockPosition from, BlockPosition to) {
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

    private static NavigationPoint pointOf(SurfaceNode node) {
        return new NavigationPoint(node.centerX(), node.floorY(), node.centerZ());
    }
    @FunctionalInterface
    private interface SurfaceDiagnosticsFactory {
        RouteSearchDiagnostics create(int knownSurfaceCount);
    }
}

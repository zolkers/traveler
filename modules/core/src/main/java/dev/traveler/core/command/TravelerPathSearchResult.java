package dev.traveler.core.command;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.NavigationSegmentIntent;
import dev.traveler.core.navigation.NavigationGoalPlan;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.route.longdistance.LongDistanceRoutePlan;
import dev.traveler.core.route.RoutePath;
import dev.traveler.core.route.RouteSearchResult;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TravelerPathSearchResult(
        RouteSearchResult searchResult,
        String message,
        Optional<LongDistanceRoutePlan> routePlan,
        Optional<WorldPoint> navigationStartOverride) {
    public TravelerPathSearchResult(RouteSearchResult searchResult, String message) {
        this(searchResult, message, Optional.empty(), Optional.empty());
    }

    public TravelerPathSearchResult(RouteSearchResult searchResult, String message, LongDistanceRoutePlan routePlan) {
        this(
                searchResult,
                message,
                Optional.of(Objects.requireNonNull(routePlan, "routePlan")),
                Optional.empty());
    }

    public TravelerPathSearchResult(
            RouteSearchResult searchResult,
            String message,
            Optional<LongDistanceRoutePlan> routePlan) {
        this(searchResult, message, routePlan, Optional.empty());
    }

    public TravelerPathSearchResult {
        Objects.requireNonNull(searchResult, "searchResult");
        Objects.requireNonNull(message, "message");
        routePlan = Objects.requireNonNull(routePlan, "routePlan");
        navigationStartOverride = Objects.requireNonNull(navigationStartOverride, "navigationStartOverride");
    }

    public void updateDebug(PathfinderDebugState debugState) {
        PathfinderDebugState state = Objects.requireNonNull(debugState, "debugState");
        if (searchResult.surfaceResult().isPresent()) {
            if (searchResult.route().isPresent()) {
                state.updateSurface(
                        searchResult.surfaceResult().orElseThrow(),
                        searchResult.route().orElseThrow().executionPoints(),
                        message);
                return;
            }
            state.updateSurface(searchResult.surfaceResult().orElseThrow(), message);
            return;
        }
        state.update(searchResult.blockResult(), message);
    }

    public Optional<NavigationPath> navigationPath() {
        if (status() != PathfinderStatus.FOUND) {
            return Optional.empty();
        }
        Optional<NavigationPath> routePath =
                searchResult.route().map(route -> navigationPathFromRoute(route, navigationStartOverride));
        if (routePath.isPresent()) {
            return routePath;
        }
        return navigationPath(anchoredPoints(fallbackWorldPoints(), navigationStartOverride));
    }

    public Optional<NavigationGoalPlan> navigationGoalPlan() {
        return routePlan.map(NavigationGoalPlan::from);
    }

    public boolean alreadyAtTarget() {
        return status() == PathfinderStatus.FOUND && navigationPointCount() == 1;
    }

    public PathfinderStatus status() {
        return searchResult.status();
    }

    private List<WorldPoint> fallbackWorldPoints() {
        return searchResult.surfaceResult()
                .map(result -> result.path().nodes().stream()
                        .map(TravelerPathSearchResult::surfacePoint)
                        .toList())
                .orElseGet(() -> searchResult.blockResult().path().nodes().stream()
                        .map(TravelerPathSearchResult::blockCenter)
                        .toList());
    }

    private int navigationPointCount() {
        return searchResult.route()
                .map(route -> route.points().size())
                .or(() -> searchResult.surfaceResult().map(result -> result.path().nodeCount()))
                .orElseGet(() -> searchResult.blockResult().path().nodeCount());
    }

    private static Optional<NavigationPath> navigationPath(List<WorldPoint> points) {
        if (points.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(NavigationPath.of(points));
    }

    private static NavigationPath navigationPathFromRoute(
            RoutePath route,
            Optional<WorldPoint> startOverride) {
        return NavigationPath.withIntents(
                anchoredPoints(route.points(), startOverride),
                route.steps().stream()
                        .map(TravelerPathSearchResult::segmentIntent)
                        .toList());
    }

    private static List<WorldPoint> anchoredPoints(
            List<WorldPoint> points,
            Optional<WorldPoint> startOverride) {
        List<WorldPoint> safePoints = List.copyOf(Objects.requireNonNull(points, "points"));
        Optional<WorldPoint> override = Objects.requireNonNull(startOverride, "startOverride");
        if (override.isEmpty() || safePoints.isEmpty()) {
            return safePoints;
        }
        java.util.ArrayList<WorldPoint> anchored = new java.util.ArrayList<>(safePoints);
        anchored.set(0, override.orElseThrow());
        return List.copyOf(anchored);
    }

    private static NavigationSegmentIntent segmentIntent(dev.traveler.core.route.RouteStep step) {
        return NavigationSegmentIntent.of(step.action(), step.targetPoint());
    }

    private static WorldPoint surfacePoint(SurfaceNode node) {
        return new WorldPoint(node.centerX(), node.floorY(), node.centerZ());
    }

    private static WorldPoint blockCenter(BlockPosition position) {
        return new WorldPoint(position.x() + 0.5, position.y(), position.z() + 0.5);
    }
}

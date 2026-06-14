package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.NavigationSegmentAction;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.route.RoutePath;
import dev.traveler.core.route.RouteSearchResult;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

record TravelerPathSearchResult(
        RouteSearchResult searchResult,
        String message) {
    TravelerPathSearchResult {
        Objects.requireNonNull(searchResult, "searchResult");
        Objects.requireNonNull(message, "message");
    }

    void updateDebug(PathfinderDebugState debugState) {
        PathfinderDebugState state = Objects.requireNonNull(debugState, "debugState");
        if (searchResult.surfaceResult().isPresent()) {
            state.updateSurface(searchResult.surfaceResult().orElseThrow(), message);
            return;
        }
        state.update(searchResult.blockResult(), message);
    }

    Optional<NavigationPath> navigationPath() {
        if (status() != PathfinderStatus.FOUND) {
            return Optional.empty();
        }
        Optional<NavigationPath> routePath =
                searchResult.route().map(TravelerPathSearchResult::navigationPathFromRoute);
        if (routePath.isPresent()) {
            return routePath;
        }
        return navigationPath(fallbackNavigationPoints());
    }

    boolean alreadyAtTarget() {
        return status() == PathfinderStatus.FOUND && navigationPointCount() == 1;
    }

    PathfinderStatus status() {
        return searchResult.status();
    }

    private List<NavigationPoint> fallbackNavigationPoints() {
        return searchResult.surfaceResult()
                .map(result -> result.path().nodes().stream()
                        .map(TravelerPathSearchResult::surfacePoint)
                        .toList())
                .orElseGet(() -> searchResult.blockResult().path().nodes().stream()
                        .map(NavigationPoint::blockCenter)
                        .toList());
    }

    private int navigationPointCount() {
        return searchResult.route()
                .map(route -> route.points().size())
                .or(() -> searchResult.surfaceResult().map(result -> result.path().nodeCount()))
                .orElseGet(() -> searchResult.blockResult().path().nodeCount());
    }

    private static Optional<NavigationPath> navigationPath(List<NavigationPoint> points) {
        if (points.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(NavigationPath.of(points));
    }

    private static NavigationPath navigationPathFromRoute(RoutePath route) {
        return NavigationPath.of(
                route.points(),
                route.actions().stream()
                        .map(TravelerPathSearchResult::segmentAction)
                        .toList());
    }

    private static NavigationSegmentAction segmentAction(MovementAction action) {
        return switch (action) {
            case WALK -> NavigationSegmentAction.WALK;
            case STEP_UP -> NavigationSegmentAction.STEP_UP;
            case JUMP -> NavigationSegmentAction.JUMP;
            case DROP -> NavigationSegmentAction.DROP;
            case CLIMB -> NavigationSegmentAction.CLIMB;
            case SWIM -> NavigationSegmentAction.WALK;
            case BLOCKED -> NavigationSegmentAction.INFER;
        };
    }

    private static NavigationPoint surfacePoint(SurfaceNode node) {
        return new NavigationPoint(node.centerX(), node.floorY(), node.centerZ());
    }
}

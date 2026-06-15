package dev.traveler.mc.v1_21_11.common.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.route.RoutePath;
import dev.traveler.core.route.RouteSearchDiagnostics;
import dev.traveler.core.route.RouteSearchResult;
import dev.traveler.core.route.RouteStep;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TravelerPathSearchResultTest {
    @Test
    void navigationPathKeepsRouteSegmentActions() {
        SurfaceNode start = node(0);
        SurfaceNode goal = node(1);
        RoutePath route = RoutePath.of(List.of(new RouteStep(start, goal, MovementAction.JUMP, 1.0)));
        TravelerPathSearchResult result = new TravelerPathSearchResult(
                new RouteSearchResult(
                        blockResult(start, goal),
                        Optional.empty(),
                        Optional.of(route),
                        RouteSearchDiagnostics.none(1, 1)),
                "path");

        NavigationPath path = result.navigationPath().orElseThrow();

        assertEquals(MovementAction.JUMP, path.actionBeforeNode(1));
    }

    @Test
    void navigationPathKeepsClimbRouteSegmentAction() {
        SurfaceNode start = node(0);
        SurfaceNode goal = node(1);
        RoutePath route = RoutePath.of(List.of(new RouteStep(start, goal, MovementAction.CLIMB, 1.0)));
        TravelerPathSearchResult result = new TravelerPathSearchResult(
                new RouteSearchResult(
                        blockResult(start, goal),
                        Optional.empty(),
                        Optional.of(route),
                        RouteSearchDiagnostics.none(1, 1)),
                "path");

        NavigationPath path = result.navigationPath().orElseThrow();

        assertEquals(MovementAction.CLIMB, path.actionBeforeNode(1));
    }

    @Test
    void navigationPathKeepsSwimRouteSegmentAction() {
        SurfaceNode start = node(0);
        SurfaceNode goal = node(1);
        RoutePath route = RoutePath.of(List.of(new RouteStep(start, goal, MovementAction.SWIM, 1.0)));
        TravelerPathSearchResult result = new TravelerPathSearchResult(
                new RouteSearchResult(
                        blockResult(start, goal),
                        Optional.empty(),
                        Optional.of(route),
                        RouteSearchDiagnostics.none(1, 1)),
                "path");

        NavigationPath path = result.navigationPath().orElseThrow();

        assertEquals(MovementAction.SWIM, path.actionBeforeNode(1));
    }

    @Test
    void navigationPathKeepsLandingNodeSeparateFromClimbActionTarget() {
        SurfaceNode start = node(0);
        SurfaceNode landing = node(1);
        NavigationPoint climbFace = new NavigationPoint(0.7, 64.0, 0.5);
        RoutePath route = RoutePath.of(List.of(new RouteStep(start, landing, MovementAction.CLIMB, 1.0, climbFace)));
        TravelerPathSearchResult result = new TravelerPathSearchResult(
                new RouteSearchResult(
                        blockResult(start, landing),
                        Optional.empty(),
                        Optional.of(route),
                        RouteSearchDiagnostics.none(1, 1)),
                "path");

        NavigationPath path = result.navigationPath().orElseThrow();

        assertEquals(new NavigationPoint(1.25, 64.0, 0.25), path.nodeAt(1));
        assertEquals(climbFace, path.actionTargetBeforeNode(1));
    }

    @Test
    void debugSnapshotKeepsRouteExecutionPointsForRendering() {
        SurfaceNode start = node(0);
        SurfaceNode landing = node(1);
        NavigationPoint climbFace = new NavigationPoint(0.7, 64.0, 0.5);
        RoutePath route = RoutePath.of(List.of(new RouteStep(start, landing, MovementAction.CLIMB, 1.0, climbFace)));
        TravelerPathSearchResult result = new TravelerPathSearchResult(
                new RouteSearchResult(
                        blockResult(start, landing),
                        Optional.of(surfaceResult(start, landing)),
                        Optional.of(route),
                        RouteSearchDiagnostics.none(1, 1)),
                "path");
        PathfinderDebugState debugState = new PathfinderDebugState();

        result.updateDebug(debugState);

        assertEquals(List.of(new NavigationPoint(0.25, 64.0, 0.25), climbFace),
                debugState.latestSnapshot().orElseThrow().routePoints());
    }

    private static PathfinderResult<BlockPosition> blockResult(SurfaceNode start, SurfaceNode goal) {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        path.addNode(start.renderBlockPosition());
        path.addNode(goal.renderBlockPosition());
        return new PathfinderResult<>(PathfinderStatus.FOUND, path);
    }

    private static PathfinderResult<SurfaceNode> surfaceResult(SurfaceNode start, SurfaceNode goal) {
        MutableGraphPath<SurfaceNode> path = new MutableGraphPath<>();
        path.addNode(start);
        path.addNode(goal);
        return new PathfinderResult<>(PathfinderStatus.FOUND, path);
    }

    private static SurfaceNode node(int x) {
        BlockPosition position = new BlockPosition(x, 63, 0);
        return new SurfaceNode(position, 0, 0, 64.0);
    }
}

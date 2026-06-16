package dev.traveler.core.navigation.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteProgressPolicyTest {
    private final RouteProgressPolicy policy = new RouteProgressPolicy(0.45);

    @Test
    void advancesWhenAgentHasPassedNodeWithoutTouchingItsCenter() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 64.0, 2.0),
                point(0.0, 64.0, 6.0)));
        WorldPoint missedNode = point(0.7, 64.0, 2.4);

        PathProgress progress = policy.progress(path, missedNode, PathProgress.start());

        assertEquals(2, progress.nextNodeIndex());
    }

    @Test
    void advancesWhenAgentOvershootsNodeOutsideTheLocalGate() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 64.0, 2.0),
                point(0.0, 64.0, 6.0)));
        WorldPoint overshotNode = point(0.9, 64.0, 4.0);

        PathProgress progress = policy.progress(path, overshotNode, PathProgress.start());

        assertEquals(2, progress.nextNodeIndex());
    }

    @Test
    void keepsCurrentNodeWhenAgentIsOnlyBesideItBeforeTheGate() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 64.0, 2.0),
                point(0.0, 64.0, 6.0)));
        WorldPoint beforeNode = point(0.7, 64.0, 1.6);

        PathProgress progress = policy.progress(path, beforeNode, PathProgress.start());

        assertEquals(1, progress.nextNodeIndex());
    }

    @Test
    void keepsCornerNodeWhenAgentHasNotCrossedIncomingGate() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 64.0, 2.0),
                point(2.0, 64.0, 2.0)));
        WorldPoint beforeCorner = point(0.7, 64.0, 1.8);

        PathProgress progress = policy.progress(path, beforeCorner, PathProgress.start());

        assertEquals(1, progress.nextNodeIndex());
    }

    @Test
    void keepsSpecialActionNodeUntilLandingIsActuallyReached() {
        NavigationPath path = NavigationPath.of(
                List.of(
                        point(0.0, 64.0, 0.0),
                        point(0.0, 65.0, 1.0),
                        point(1.0, 65.0, 1.0)),
                List.of(MovementAction.JUMP, MovementAction.WALK));
        WorldPoint airbornePastLanding = point(0.7, 65.0, 1.2);

        PathProgress progress = policy.progress(path, airbornePastLanding, PathProgress.start());

        assertEquals(1, progress.nextNodeIndex());
    }

    @Test
    void advancesSpecialActionNodeWhenLandingIsCloseEnough() {
        NavigationPath path = NavigationPath.of(
                List.of(
                        point(0.0, 64.0, 0.0),
                        point(0.0, 65.0, 1.0),
                        point(1.0, 65.0, 1.0)),
                List.of(MovementAction.JUMP, MovementAction.WALK));
        WorldPoint landed = point(0.2, 65.0, 1.1);

        PathProgress progress = policy.progress(path, landed, PathProgress.start());

        assertEquals(2, progress.nextNodeIndex());
    }

    private static WorldPoint point(double x, double y, double z) {
        return new WorldPoint(x, y, z);
    }
}

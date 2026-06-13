package dev.traveler.core.navigation.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.spatial.NavigationPoint;
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
        NavigationPoint missedNode = point(0.7, 64.0, 2.4);

        PathProgress progress = policy.progress(path, missedNode, PathProgress.start());

        assertEquals(2, progress.nextNodeIndex());
    }

    @Test
    void advancesWhenAgentOvershootsNodeOutsideTheLocalGate() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 64.0, 2.0),
                point(0.0, 64.0, 6.0)));
        NavigationPoint overshotNode = point(0.9, 64.0, 4.0);

        PathProgress progress = policy.progress(path, overshotNode, PathProgress.start());

        assertEquals(2, progress.nextNodeIndex());
    }

    @Test
    void keepsCurrentNodeWhenAgentIsOnlyBesideItBeforeTheGate() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 64.0, 2.0),
                point(0.0, 64.0, 6.0)));
        NavigationPoint beforeNode = point(0.7, 64.0, 1.6);

        PathProgress progress = policy.progress(path, beforeNode, PathProgress.start());

        assertEquals(1, progress.nextNodeIndex());
    }

    @Test
    void keepsCornerNodeWhenAgentHasNotCrossedIncomingGate() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 64.0, 2.0),
                point(2.0, 64.0, 2.0)));
        NavigationPoint beforeCorner = point(0.7, 64.0, 1.8);

        PathProgress progress = policy.progress(path, beforeCorner, PathProgress.start());

        assertEquals(1, progress.nextNodeIndex());
    }

    private static NavigationPoint point(double x, double y, double z) {
        return new NavigationPoint(x, y, z);
    }
}

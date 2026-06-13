package dev.traveler.core.navigation.follow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathFollowControllerTest {
    private final PathFollowController controller = new PathFollowController(
            new PathFollowSettings(0.45, 2.0, 2.5, 0.35));

    @Test
    void targetsLookaheadPointAlongCurrentSegment() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 0.0),
                point(4.0, 0.0),
                point(8.0, 0.0)));

        PathFollowFrame frame = controller.update(path, point(0.0, 0.0), PathProgress.start());

        assertEquals(point(2.0, 0.0), frame.target());
        assertEquals(new PathProgress(1), frame.progress());
        assertFalse(frame.completed());
    }

    @Test
    void advancesPastReachedIntermediateNode() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 0.0),
                point(1.0, 0.0),
                point(4.0, 0.0)));

        PathFollowFrame frame = controller.update(path, point(1.0, 0.0), new PathProgress(1));

        assertEquals(new PathProgress(2), frame.progress());
        assertEquals(point(3.0, 0.0), frame.target());
    }

    @Test
    void slowsDownInsideArrivalRadius() {
        NavigationPath path = NavigationPath.of(List.of(point(0.0, 0.0), point(4.0, 0.0)));

        PathFollowFrame frame = controller.update(path, point(3.0, 0.0), new PathProgress(1));

        assertTrue(frame.speedScale() < 1.0);
        assertTrue(frame.speedScale() >= 0.35);
    }

    @Test
    void completesWhenAgentIsCloseEnoughToFinalNode() {
        NavigationPath path = NavigationPath.of(List.of(point(0.0, 0.0), point(4.0, 0.0)));

        PathFollowFrame frame = controller.update(path, point(4.1, 0.0), new PathProgress(1));

        assertTrue(frame.completed());
        assertEquals(MovementTarget.stopAt(point(4.0, 0.0)), frame.movementTarget());
    }

    private static NavigationPoint point(double x, double z) {
        return new NavigationPoint(x, 64.0, z);
    }
}

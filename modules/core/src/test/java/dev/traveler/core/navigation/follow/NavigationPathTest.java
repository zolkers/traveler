package dev.traveler.core.navigation.follow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class NavigationPathTest {
    @Test
    void pointOnlyPathUsesInferredSegmentActions() {
        NavigationPath path = NavigationPath.of(List.of(point(0.0), point(1.0), point(2.0)));

        assertEquals(NavigationSegmentAction.INFER, path.actionBeforeNode(1));
        assertEquals(NavigationSegmentAction.INFER, path.actionBeforeNode(2));
    }

    @Test
    void routeActionsAreStoredPerSegment() {
        NavigationPath path = NavigationPath.of(
                List.of(point(0.0), point(1.0), point(2.0)),
                List.of(NavigationSegmentAction.WALK, NavigationSegmentAction.JUMP));

        assertEquals(NavigationSegmentAction.WALK, path.actionBeforeNode(1));
        assertEquals(NavigationSegmentAction.JUMP, path.actionBeforeNode(2));
    }

    @Test
    void rejectsActionCountThatDoesNotMatchSegments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NavigationPath.of(
                        List.of(point(0.0), point(1.0)),
                        List.of(NavigationSegmentAction.WALK, NavigationSegmentAction.JUMP)));
    }

    private static NavigationPoint point(double x) {
        return new NavigationPoint(x, 64.0, 0.0);
    }
}

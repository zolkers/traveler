package dev.traveler.core.navigation.follow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.List;
import org.junit.jupiter.api.Test;

class NavigationPathTest {
    @Test
    void pointOnlyPathUsesWalkSegmentActions() {
        NavigationPath path = NavigationPath.of(List.of(point(0.0), point(1.0), point(2.0)));

        assertEquals(MovementAction.WALK, path.actionBeforeNode(1));
        assertEquals(MovementAction.WALK, path.actionBeforeNode(2));
    }

    @Test
    void routeActionsAreStoredPerSegment() {
        NavigationPath path = NavigationPath.of(
                List.of(point(0.0), point(1.0), point(2.0)),
                List.of(MovementAction.WALK, MovementAction.JUMP));

        assertEquals(MovementAction.WALK, path.actionBeforeNode(1));
        assertEquals(MovementAction.JUMP, path.actionBeforeNode(2));
    }

    @Test
    void explicitActionTargetsDoNotReplaceRouteNodes() {
        WorldPoint landing = point(1.0);
        WorldPoint climbFace = new WorldPoint(0.7, 64.0, 0.5);
        NavigationPath path = NavigationPath.of(
                List.of(point(0.0), landing),
                List.of(MovementAction.CLIMB),
                List.of(climbFace));

        assertEquals(landing, path.nodeAt(1));
        assertEquals(climbFace, path.actionTargetBeforeNode(1));
    }

    @Test
    void segmentIntentGroupsActionAndActionTarget() {
        WorldPoint climbFace = new WorldPoint(0.7, 64.0, 0.5);
        NavigationPath path = NavigationPath.withIntents(
                List.of(point(0.0), point(1.0)),
                List.of(NavigationSegmentIntent.of(MovementAction.CLIMB, climbFace)));

        NavigationSegmentIntent intent = path.segmentIntentBeforeNode(1);

        assertEquals(MovementAction.CLIMB, intent.action());
        assertEquals(climbFace, intent.actionTarget());
    }

    @Test
    void rejectsActionCountThatDoesNotMatchSegments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NavigationPath.of(
                        List.of(point(0.0), point(1.0)),
                        List.of(MovementAction.WALK, MovementAction.JUMP)));
    }

    private static WorldPoint point(double x) {
        return new WorldPoint(x, 64.0, 0.0);
    }
}

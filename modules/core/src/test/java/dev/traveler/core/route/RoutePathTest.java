package dev.traveler.core.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoutePathTest {
    @Test
    void exposesOrderedNodesActionsAndCost() {
        SurfaceNode first = node(0, 64, 0);
        SurfaceNode second = node(1, 64, 0);
        SurfaceNode third = node(2, 65, 0);
        RoutePath route = RoutePath.of(List.of(
                new RouteStep(first, second, MovementAction.WALK, 1.0),
                new RouteStep(second, third, MovementAction.JUMP, 2.5)));

        assertEquals(List.of(first, second, third), route.nodes());
        assertEquals(List.of(MovementAction.WALK, MovementAction.JUMP), route.actions());
        assertEquals(3.5, route.cost());
        assertEquals(3, route.points().size());
        assertEquals(1, route.actionCount(MovementAction.WALK));
        assertEquals(1, route.actionCount(MovementAction.JUMP));
    }

    @Test
    void executionPointsUseStepTargetsAfterTheStartPoint() {
        SurfaceNode first = node(0, 64, 0);
        SurfaceNode second = node(1, 64, 0);
        SurfaceNode third = node(2, 65, 0);
        NavigationPoint climbFace = new NavigationPoint(1.3, 64.0, 0.5);
        NavigationPoint landing = new NavigationPoint(2.25, 66.0, 0.25);
        RoutePath route = RoutePath.of(List.of(
                new RouteStep(first, second, MovementAction.CLIMB, 1.0, climbFace),
                new RouteStep(second, third, MovementAction.CLIMB, 1.0, landing)));

        assertEquals(
                List.of(new NavigationPoint(0.25, 65.0, 0.25), climbFace, landing),
                route.executionPoints());
    }

    @Test
    void rejectsDisconnectedSteps() {
        SurfaceNode first = node(0, 64, 0);
        SurfaceNode second = node(1, 64, 0);
        SurfaceNode third = node(2, 64, 0);
        SurfaceNode fourth = node(3, 64, 0);

        List<RouteStep> steps = List.of(
                new RouteStep(first, second, MovementAction.WALK, 1.0),
                new RouteStep(third, fourth, MovementAction.WALK, 1.0));

        assertThrows(IllegalArgumentException.class, () -> RoutePath.of(steps));
    }

    @Test
    void rejectsNegativeStepCost() {
        SurfaceNode first = node(0, 64, 0);
        SurfaceNode second = node(1, 64, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> new RouteStep(first, second, MovementAction.WALK, -0.1));
    }

    private static SurfaceNode node(int x, int y, int z) {
        BlockPosition position = new BlockPosition(x, y, z);
        return new SurfaceNode(position, 0, 0, y + 1.0);
    }
}

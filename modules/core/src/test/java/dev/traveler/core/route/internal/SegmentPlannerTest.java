package dev.traveler.core.route.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.route.RoutePath;
import dev.traveler.core.route.RouteStep;
import dev.traveler.core.route.api.RouteTraversalHint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class SegmentPlannerTest {
    @Test
    void segmentsRouteStepsIntoRouteTraversalHints() {
        SurfaceNode start = node(0, 64, 0);
        SurfaceNode jump = node(1, 64, 0);
        SurfaceNode climb = node(1, 65, 0);
        RoutePath path = RoutePath.of(List.of(
                new RouteStep(start, jump, MovementAction.JUMP, 1.0),
                new RouteStep(jump, climb, MovementAction.CLIMB, 1.0)));

        var segments = new SegmentPlanner().segments(path);

        assertEquals(RouteTraversalHint.JUMP, segments.get(0).traversalHint());
        assertEquals(RouteTraversalHint.CLIMB, segments.get(1).traversalHint());
        assertEquals(start.blockPosition(), segments.get(0).start());
        assertEquals(climb.blockPosition(), segments.get(1).end());
    }

    private static SurfaceNode node(int x, int y, int z) {
        return new SurfaceNode(new BlockPosition(x, y, z), 0, 0, y + 1.0);
    }
}

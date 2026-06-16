package dev.traveler.core.navigation.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.api.TraversalKind;
import dev.traveler.core.route.api.RouteSegment;
import dev.traveler.core.route.api.RouteTraversalHint;
import dev.traveler.core.world.block.BlockPosition;
import org.junit.jupiter.api.Test;

class DefaultTraversalPlannerTest {
    @Test
    void mapsRouteTraversalHintsToNavigationTraversalKinds() {
        RouteSegment segment = new RouteSegment(
                0,
                new BlockPosition(0, 64, 0),
                new BlockPosition(1, 65, 0),
                RouteTraversalHint.JUMP);

        var traversal = new DefaultTraversalPlanner().traversalFor(segment);

        assertEquals(TraversalKind.JUMP, traversal.kind());
    }
}

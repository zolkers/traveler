package dev.traveler.core.route.longdistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import org.junit.jupiter.api.Test;

class LongDistanceRoutePlannerTest {
    @Test
    void keepsShortGoalsAsFinalSegments() {
        LongDistanceRouteSettings settings = new LongDistanceRouteSettings(96.0, 128.0, 72, 12.0);
        LongDistanceRoutePlanner planner = new LongDistanceRoutePlanner(settings);
        RouteGoal goal = RouteGoal.xyz(20, 64, 0);

        LongDistanceRoutePlan plan = planner.plan(new BlockPosition(0, 64, 0), goal);

        assertTrue(plan.finalSegment());
        assertSame(goal, plan.activeGoal());
    }

    @Test
    void clipsVeryLongGoalsToSnapshotSizedIntermediateGoals() {
        LongDistanceRouteSettings settings = new LongDistanceRouteSettings(96.0, 128.0, 72, 12.0);
        LongDistanceRoutePlanner planner = new LongDistanceRoutePlanner(settings);

        LongDistanceRoutePlan plan = planner.plan(
                new BlockPosition(0, 64, 0),
                RouteGoal.xyz(10_000, 64, 10_000));

        BlockPosition activeBlockGoal = plan.activeGoal().blockGoal(null, new BlockPosition(0, 64, 0));
        assertFalse(plan.finalSegment());
        assertEquals(72, Math.abs(activeBlockGoal.x()));
        assertEquals(72, Math.abs(activeBlockGoal.z()));
    }
}

package dev.traveler.core.route.longdistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.WorldNavigationBudget;
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
        LongDistanceRouteSettings settings = new LongDistanceRouteSettings(96.0, 102.0, 72, 24.0);
        LongDistanceRoutePlanner planner = new LongDistanceRoutePlanner(settings);

        LongDistanceRoutePlan plan = planner.plan(
                new BlockPosition(0, 64, 0),
                RouteGoal.xyz(10_000, 64, 10_000));

        BlockPosition activeBlockGoal = plan.activeGoal().blockGoal(null, new BlockPosition(0, 64, 0));
        assertFalse(plan.finalSegment());
        assertEquals(52, Math.abs(activeBlockGoal.x()));
        assertEquals(52, Math.abs(activeBlockGoal.z()));
        assertTrue(plan.activeGoal().displayName().startsWith("frontier"));
    }

    @Test
    void adaptsIntermediateGoalToVisibleWorldBudget() {
        LongDistanceRouteSettings settings = new LongDistanceRouteSettings(96.0, 102.0, 72, 24.0);
        LongDistanceRoutePlanner planner = new LongDistanceRoutePlanner(settings);
        WorldNavigationBudget budget = new WorldNavigationBudget(64);

        LongDistanceRoutePlan plan = planner.plan(
                new BlockPosition(0, 64, 0),
                RouteGoal.xyz(10_000, 64, 10_000),
                budget);

        BlockPosition activeBlockGoal = plan.activeGoal().blockGoal(null, new BlockPosition(0, 64, 0));
        assertFalse(plan.finalSegment());
        assertEquals(32, Math.abs(activeBlockGoal.x()));
        assertEquals(32, Math.abs(activeBlockGoal.z()));
        assertEquals(32, plan.settings().maxSegmentAxisDelta());
    }

    @Test
    void capsIntermediateGoalToTargetSnapshotBudgetWhenVisibilityAllowsMore() {
        LongDistanceRouteSettings settings = new LongDistanceRouteSettings(96.0, 102.0, 72, 24.0);
        LongDistanceRoutePlanner planner = new LongDistanceRoutePlanner(settings);
        WorldNavigationBudget budget = new WorldNavigationBudget(192);

        LongDistanceRoutePlan plan = planner.plan(
                new BlockPosition(0, 64, 0),
                RouteGoal.xyz(10_000, 64, 10_000),
                budget);

        BlockPosition activeBlockGoal = plan.activeGoal().blockGoal(null, new BlockPosition(0, 64, 0));
        assertEquals(52, Math.abs(activeBlockGoal.x()));
        assertEquals(52, Math.abs(activeBlockGoal.z()));
        assertEquals(52, plan.settings().maxSegmentAxisDelta());
    }

    @Test
    void navigationLookaheadDistanceScalesWithIntermediateSegmentLength() {
        LongDistanceRouteSettings settings = new LongDistanceRouteSettings(96.0, 102.0, 72, 24.0);
        LongDistanceRoutePlanner planner = new LongDistanceRoutePlanner(settings);

        LongDistanceRoutePlan plan = planner.plan(
                new BlockPosition(0, 64, 0),
                RouteGoal.xyz(10_000, 64, 10_000));

        assertEquals(48.0, plan.navigationGoalPlan().lookaheadReplanDistance(), 1.0E-6);
    }

    @Test
    void navigationLookaheadDistanceIsBoundedByVisibleWorldBudget() {
        LongDistanceRouteSettings settings = new LongDistanceRouteSettings(96.0, 102.0, 72, 24.0);
        LongDistanceRoutePlanner planner = new LongDistanceRoutePlanner(settings);

        LongDistanceRoutePlan plan = planner.plan(
                new BlockPosition(0, 64, 0),
                RouteGoal.xyz(10_000, 64, 10_000),
                new WorldNavigationBudget(64));

        assertEquals(Math.hypot(32, 32) * 0.75,
                plan.navigationGoalPlan().lookaheadReplanDistance(),
                1.0E-6);
    }
}

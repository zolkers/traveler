package dev.traveler.core.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.layer.WorldNavigationBudget;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.route.longdistance.LongDistanceRoutePlan;
import dev.traveler.core.route.longdistance.LongDistanceRoutePlanner;
import dev.traveler.core.route.longdistance.LongDistanceRouteSettings;
import dev.traveler.core.world.block.BlockPosition;
import org.junit.jupiter.api.Test;

class NavigationGoalPlanTest {
    @Test
    void lookaheadDistanceScalesWithIntermediateSegmentLength() {
        LongDistanceRoutePlan plan = longDistancePlan(new WorldNavigationBudget(128));

        assertEquals(48.0, NavigationGoalPlan.from(plan).lookaheadReplanDistance(), 1.0E-6);
    }

    @Test
    void lookaheadDistanceIsBoundedByVisibleWorldBudget() {
        LongDistanceRoutePlan plan = longDistancePlan(new WorldNavigationBudget(64));

        assertEquals(
                Math.hypot(32, 32) * 0.75,
                NavigationGoalPlan.from(plan).lookaheadReplanDistance(),
                1.0E-6);
    }

    private static LongDistanceRoutePlan longDistancePlan(WorldNavigationBudget budget) {
        LongDistanceRouteSettings settings = new LongDistanceRouteSettings(96.0, 102.0, 72, 24.0);
        LongDistanceRoutePlanner planner = new LongDistanceRoutePlanner(settings);
        return planner.plan(
                new BlockPosition(0, 64, 0),
                RouteGoal.xyz(10_000, 64, 10_000),
                budget);
    }
}

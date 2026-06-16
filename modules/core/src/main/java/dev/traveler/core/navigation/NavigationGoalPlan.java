package dev.traveler.core.navigation;

import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.route.longdistance.LongDistanceRoutePlan;
import java.util.Objects;

public record NavigationGoalPlan(
        RouteGoal requestedGoal,
        RouteGoal activeGoal,
        boolean finalSegment,
        double lookaheadReplanDistance) {
    public NavigationGoalPlan(RouteGoal requestedGoal, RouteGoal activeGoal, boolean finalSegment) {
        this(requestedGoal, activeGoal, finalSegment, 0.0);
    }

    public NavigationGoalPlan {
        Objects.requireNonNull(requestedGoal, "requestedGoal");
        Objects.requireNonNull(activeGoal, "activeGoal");
        if (!Double.isFinite(lookaheadReplanDistance) || lookaheadReplanDistance < 0.0) {
            throw new IllegalArgumentException("lookaheadReplanDistance must be finite and non-negative.");
        }
    }

    public static NavigationGoalPlan from(LongDistanceRoutePlan routePlan) {
        LongDistanceRoutePlan plan = Objects.requireNonNull(routePlan, "routePlan");
        return new NavigationGoalPlan(
                plan.requestedGoal(),
                plan.activeGoal(),
                plan.finalSegment(),
                plan.settings().lookaheadReplanDistance(plan.activeSegmentHorizontalDistance()));
    }

    public boolean needsReplanAfterCompletion() {
        return !finalSegment;
    }

    public boolean needsLookaheadReplan(double remainingDistance) {
        return needsReplanAfterCompletion()
                && lookaheadReplanDistance > 0.0
                && remainingDistance <= lookaheadReplanDistance;
    }
}

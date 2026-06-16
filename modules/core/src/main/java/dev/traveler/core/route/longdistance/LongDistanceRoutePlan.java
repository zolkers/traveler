package dev.traveler.core.route.longdistance;

import dev.traveler.core.navigation.NavigationGoalPlan;
import dev.traveler.core.route.RouteGoal;
import java.util.Objects;

public record LongDistanceRoutePlan(
        RouteGoal requestedGoal,
        RouteGoal activeGoal,
        boolean finalSegment,
        double remainingHorizontalDistance,
        double activeSegmentHorizontalDistance,
        LongDistanceRouteSettings settings) {
    public LongDistanceRoutePlan {
        Objects.requireNonNull(requestedGoal, "requestedGoal");
        Objects.requireNonNull(activeGoal, "activeGoal");
        Objects.requireNonNull(settings, "settings");
        if (!Double.isFinite(remainingHorizontalDistance) || remainingHorizontalDistance < 0.0) {
            throw new IllegalArgumentException("remainingHorizontalDistance must be finite and non-negative.");
        }
        if (!Double.isFinite(activeSegmentHorizontalDistance) || activeSegmentHorizontalDistance < 0.0) {
            throw new IllegalArgumentException("activeSegmentHorizontalDistance must be finite and non-negative.");
        }
    }

    public NavigationGoalPlan navigationGoalPlan() {
        return new NavigationGoalPlan(
                requestedGoal,
                activeGoal,
                finalSegment,
                settings.lookaheadReplanDistance(activeSegmentHorizontalDistance));
    }
}

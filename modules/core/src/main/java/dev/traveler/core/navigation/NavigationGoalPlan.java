package dev.traveler.core.navigation;

import dev.traveler.core.route.RouteGoal;
import java.util.Objects;

public record NavigationGoalPlan(RouteGoal requestedGoal, RouteGoal activeGoal, boolean finalSegment) {
    public NavigationGoalPlan {
        Objects.requireNonNull(requestedGoal, "requestedGoal");
        Objects.requireNonNull(activeGoal, "activeGoal");
    }

    public boolean needsReplanAfterCompletion() {
        return !finalSegment;
    }
}

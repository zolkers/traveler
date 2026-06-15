package dev.traveler.core.navigation;

import dev.traveler.core.route.RouteGoal;
import java.time.Instant;
import java.util.Objects;

public record NavigationReplanRequest(RouteGoal goal, String reason, Instant requestedAt) {
    public NavigationReplanRequest {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(requestedAt, "requestedAt");
    }
}

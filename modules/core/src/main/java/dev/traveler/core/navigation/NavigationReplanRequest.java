package dev.traveler.core.navigation;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.route.RouteGoal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record NavigationReplanRequest(
        RouteGoal goal,
        String reason,
        Instant requestedAt,
        boolean preserveActiveSession,
        Optional<NavigationPoint> startOverride) {
    public NavigationReplanRequest(RouteGoal goal, String reason, Instant requestedAt) {
        this(goal, reason, requestedAt, false, Optional.empty());
    }

    public NavigationReplanRequest(
            RouteGoal goal,
            String reason,
            Instant requestedAt,
            NavigationPoint startOverride) {
        this(goal, reason, requestedAt, false, Optional.of(Objects.requireNonNull(startOverride, "startOverride")));
    }

    public NavigationReplanRequest(
            RouteGoal goal,
            String reason,
            Instant requestedAt,
            boolean preserveActiveSession) {
        this(goal, reason, requestedAt, preserveActiveSession, Optional.empty());
    }

    public NavigationReplanRequest(
            RouteGoal goal,
            String reason,
            Instant requestedAt,
            boolean preserveActiveSession,
            NavigationPoint startOverride) {
        this(
                goal,
                reason,
                requestedAt,
                preserveActiveSession,
                Optional.of(Objects.requireNonNull(startOverride, "startOverride")));
    }

    public NavigationReplanRequest {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(requestedAt, "requestedAt");
        startOverride = Objects.requireNonNull(startOverride, "startOverride");
    }
}

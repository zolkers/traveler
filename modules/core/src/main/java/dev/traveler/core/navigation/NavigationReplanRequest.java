package dev.traveler.core.navigation;

import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.route.RouteGoal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record NavigationReplanRequest(
        RouteGoal goal,
        String reason,
        Instant requestedAt,
        NavigationReplanActivation activation,
        Optional<WorldPoint> startOverride,
        Optional<NavigationGoalPlan> goalPlanOverride) {
    public NavigationReplanRequest(RouteGoal goal, String reason, Instant requestedAt) {
        this(
                goal,
                reason,
                requestedAt,
                NavigationReplanActivation.START_NEW_SESSION,
                Optional.empty(),
                Optional.empty());
    }

    public NavigationReplanRequest(
            RouteGoal goal,
            String reason,
            Instant requestedAt,
            WorldPoint startOverride) {
        this(
                goal,
                reason,
                requestedAt,
                NavigationReplanActivation.START_NEW_SESSION,
                Optional.of(Objects.requireNonNull(startOverride, "startOverride")),
                Optional.empty());
    }

    public NavigationReplanRequest(
            RouteGoal goal,
            String reason,
            Instant requestedAt,
            boolean preserveActiveSession) {
        this(
                goal,
                reason,
                requestedAt,
                activationFor(preserveActiveSession),
                Optional.empty(),
                Optional.empty());
    }

    public NavigationReplanRequest(
            RouteGoal goal,
            String reason,
            Instant requestedAt,
            boolean preserveActiveSession,
            WorldPoint startOverride) {
        this(
                goal,
                reason,
                requestedAt,
                activationFor(preserveActiveSession),
                Optional.of(Objects.requireNonNull(startOverride, "startOverride")),
                Optional.empty());
    }

    public NavigationReplanRequest(
            RouteGoal goal,
            String reason,
            Instant requestedAt,
            NavigationReplanActivation activation,
            WorldPoint startOverride,
            NavigationGoalPlan goalPlanOverride) {
        this(
                goal,
                reason,
                requestedAt,
                Objects.requireNonNull(activation, "activation"),
                Optional.of(Objects.requireNonNull(startOverride, "startOverride")),
                Optional.of(Objects.requireNonNull(goalPlanOverride, "goalPlanOverride")));
    }

    public NavigationReplanRequest {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(activation, "activation");
        startOverride = Objects.requireNonNull(startOverride, "startOverride");
        goalPlanOverride = Objects.requireNonNull(goalPlanOverride, "goalPlanOverride");
    }

    public boolean preserveActiveSession() {
        return activation.preservesActiveSession();
    }

    private static NavigationReplanActivation activationFor(boolean preserveActiveSession) {
        return preserveActiveSession
                ? NavigationReplanActivation.PREPARE_LOOKAHEAD
                : NavigationReplanActivation.START_NEW_SESSION;
    }
}

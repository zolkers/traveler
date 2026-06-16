package dev.traveler.core.navigation.api;

import dev.traveler.core.navigation.NavigationReplanActivation;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.route.RouteGoal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record NavigationSnapshot(
        Optional<NavigationSessionSnapshot> active,
        Optional<NavigationSessionSnapshot> prepared,
        Optional<NavigationReplanRequestSnapshot> pending,
        Optional<String> latestMessage) {
    public NavigationSnapshot {
        active = safe(active, "active");
        prepared = safe(prepared, "prepared");
        pending = safe(pending, "pending");
        latestMessage = safe(latestMessage, "latestMessage");
    }

    public static NavigationSnapshot empty() {
        return new NavigationSnapshot(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static <T> Optional<T> safe(Optional<T> value, String name) {
        return Optional.ofNullable(Objects.requireNonNull(value, name).orElse(null));
    }

    public record NavigationSessionSnapshot(
            java.util.List<NavigationPoint> nodes,
            String message,
            Instant startedAt,
            Optional<NavigationGoalPlanSnapshot> goalPlan) {
        public NavigationSessionSnapshot {
            nodes = java.util.List.copyOf(Objects.requireNonNull(nodes, "nodes"));
            Objects.requireNonNull(message, "message");
            Objects.requireNonNull(startedAt, "startedAt");
            goalPlan = safe(goalPlan, "goalPlan");
        }
    }

    public record NavigationReplanRequestSnapshot(
            RouteGoal goal,
            String reason,
            Instant requestedAt,
            NavigationReplanActivation activation,
            Optional<NavigationPoint> startOverride,
            Optional<NavigationGoalPlanSnapshot> goalPlanOverride) {
        public NavigationReplanRequestSnapshot {
            Objects.requireNonNull(goal, "goal");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(requestedAt, "requestedAt");
            Objects.requireNonNull(activation, "activation");
            startOverride = safe(startOverride, "startOverride");
            goalPlanOverride = safe(goalPlanOverride, "goalPlanOverride");
        }
    }

    public record NavigationGoalPlanSnapshot(
            RouteGoal requestedGoal,
            RouteGoal activeGoal,
            boolean finalSegment,
            double lookaheadReplanDistance) {
        public NavigationGoalPlanSnapshot {
            Objects.requireNonNull(requestedGoal, "requestedGoal");
            Objects.requireNonNull(activeGoal, "activeGoal");
            if (!Double.isFinite(lookaheadReplanDistance) || lookaheadReplanDistance < 0.0) {
                throw new IllegalArgumentException("lookaheadReplanDistance must be finite and non-negative.");
            }
        }
    }
}

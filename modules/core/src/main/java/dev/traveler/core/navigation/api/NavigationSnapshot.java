package dev.traveler.core.navigation.api;

import dev.traveler.core.navigation.NavigationGoalPlan;
import dev.traveler.core.navigation.NavigationReplanActivation;
import dev.traveler.core.navigation.NavigationReplanRequest;
import dev.traveler.core.navigation.NavigationSession;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.route.RouteGoal;
import java.time.Instant;
import java.util.List;
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

    public static NavigationSnapshot of(
            NavigationSession activeSession,
            NavigationSession preparedSession,
            NavigationReplanRequest pendingRequest,
            String latestMessage) {
        return new NavigationSnapshot(
                snapshotOf(activeSession),
                snapshotOf(preparedSession),
                snapshotOf(pendingRequest),
                Optional.ofNullable(latestMessage));
    }

    private static <T> Optional<T> safe(Optional<T> value, String name) {
        return Optional.ofNullable(Objects.requireNonNull(value, name).orElse(null));
    }

    private static Optional<NavigationSessionSnapshot> snapshotOf(NavigationSession session) {
        if (session == null) {
            return Optional.empty();
        }
        return Optional.of(new NavigationSessionSnapshot(
                List.copyOf(session.path().nodes()),
                session.message(),
                session.startedAt(),
                session.goalPlan().map(NavigationGoalPlanSnapshot::new)));
    }

    private static Optional<NavigationReplanRequestSnapshot> snapshotOf(NavigationReplanRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        return Optional.of(new NavigationReplanRequestSnapshot(
                request.goal(),
                request.reason(),
                request.requestedAt(),
                request.activation(),
                request.startOverride(),
                request.goalPlanOverride().map(NavigationGoalPlanSnapshot::new)));
    }

    public record NavigationSessionSnapshot(
            List<NavigationPoint> nodes,
            String message,
            Instant startedAt,
            Optional<NavigationGoalPlanSnapshot> goalPlan) {
        public NavigationSessionSnapshot {
            nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
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
        public NavigationGoalPlanSnapshot(NavigationGoalPlan goalPlan) {
            this(
                    Objects.requireNonNull(goalPlan, "goalPlan").requestedGoal(),
                    goalPlan.activeGoal(),
                    goalPlan.finalSegment(),
                    goalPlan.lookaheadReplanDistance());
        }

        public NavigationGoalPlanSnapshot {
            Objects.requireNonNull(requestedGoal, "requestedGoal");
            Objects.requireNonNull(activeGoal, "activeGoal");
            if (!Double.isFinite(lookaheadReplanDistance) || lookaheadReplanDistance < 0.0) {
                throw new IllegalArgumentException("lookaheadReplanDistance must be finite and non-negative.");
            }
        }
    }
}

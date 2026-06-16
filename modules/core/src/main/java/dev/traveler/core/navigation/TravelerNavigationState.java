package dev.traveler.core.navigation;

import dev.traveler.core.navigation.api.NavigationSnapshot;
import dev.traveler.core.navigation.api.NavigationSnapshot.NavigationGoalPlanSnapshot;
import dev.traveler.core.navigation.api.NavigationSnapshot.NavigationReplanRequestSnapshot;
import dev.traveler.core.navigation.api.NavigationSnapshot.NavigationSessionSnapshot;
import dev.traveler.core.navigation.api.NavigationSnapshot.ReplanActivation;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.common.geometry.WorldPoint;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TravelerNavigationState {
    private NavigationSession activeSession;
    private NavigationSession preparedLookaheadSession;
    private NavigationReplanRequest pendingReplanRequest;
    private String latestMessage;

    public synchronized void start(NavigationPath path, String message) {
        activate(new NavigationSession(
                Objects.requireNonNull(path, "path"),
                requireMessage(message),
                Instant.now()));
    }

    public synchronized void start(NavigationPath path, String message, NavigationGoalPlan goalPlan) {
        activate(new NavigationSession(
                Objects.requireNonNull(path, "path"),
                requireMessage(message),
                Instant.now(),
                Objects.requireNonNull(goalPlan, "goalPlan")));
    }

    public synchronized void stop(String message) {
        clearState(requireMessage(message));
    }

    public synchronized void requestReplan(NavigationGoalPlan goalPlan, String message) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        String safeMessage = requireMessage(message);
        clearActiveNavigation();
        pendingReplanRequest = new NavigationReplanRequest(plan.requestedGoal(), safeMessage, Instant.now());
        latestMessage = safeMessage;
    }

    public synchronized void requestReplan(
            NavigationGoalPlan goalPlan,
            String message,
            WorldPoint startOverride) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        String safeMessage = requireMessage(message);
        clearActiveNavigation();
        pendingReplanRequest = new NavigationReplanRequest(
                plan.requestedGoal(),
                safeMessage,
                Instant.now(),
                Objects.requireNonNull(startOverride, "startOverride"));
        latestMessage = safeMessage;
    }

    public synchronized void requestSegmentRepair(
            NavigationGoalPlan goalPlan,
            String message,
            WorldPoint startOverride) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        String safeMessage = requireMessage(message);
        if (pendingReplanRequest != null || preparedLookaheadSession != null) {
            latestMessage = safeMessage;
            return;
        }
        pendingReplanRequest = new NavigationReplanRequest(
                plan.activeGoal(),
                safeMessage,
                Instant.now(),
                NavigationReplanActivation.REPLACE_ACTIVE_SESSION,
                Objects.requireNonNull(startOverride, "startOverride"),
                plan);
        latestMessage = safeMessage;
    }

    public synchronized void requestLookaheadReplan(NavigationGoalPlan goalPlan, String message) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        String safeMessage = requireMessage(message);
        if (pendingReplanRequest != null || preparedLookaheadSession != null) {
            return;
        }
        pendingReplanRequest = new NavigationReplanRequest(plan.requestedGoal(), safeMessage, Instant.now(), true);
        latestMessage = safeMessage;
    }

    public synchronized void requestLookaheadReplan(
            NavigationGoalPlan goalPlan,
            String message,
            WorldPoint startOverride) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        String safeMessage = requireMessage(message);
        if (pendingReplanRequest != null || preparedLookaheadSession != null) {
            return;
        }
        pendingReplanRequest = new NavigationReplanRequest(
                plan.requestedGoal(),
                safeMessage,
                Instant.now(),
                true,
                Objects.requireNonNull(startOverride, "startOverride"));
        latestMessage = safeMessage;
    }

    public synchronized void prepareLookahead(NavigationPath path, String message, NavigationGoalPlan goalPlan) {
        Objects.requireNonNull(path, "path");
        String safeMessage = requireMessage(message);
        Objects.requireNonNull(goalPlan, "goalPlan");
        if (activeSession == null) {
            return;
        }
        preparedLookaheadSession = new NavigationSession(
                path,
                safeMessage,
                Instant.now(),
                goalPlan);
        pendingReplanRequest = null;
        latestMessage = safeMessage;
    }

    public synchronized void replaceActiveSession(NavigationPath path, String message, NavigationGoalPlan goalPlan) {
        Objects.requireNonNull(path, "path");
        String safeMessage = requireMessage(message);
        Objects.requireNonNull(goalPlan, "goalPlan");
        if (activeSession == null) {
            return;
        }
        activate(new NavigationSession(
                path,
                safeMessage,
                Instant.now(),
                goalPlan));
    }

    public synchronized boolean activatePreparedLookahead() {
        if (preparedLookaheadSession == null) {
            return false;
        }
        activeSession = preparedLookaheadSession;
        preparedLookaheadSession = null;
        pendingReplanRequest = null;
        latestMessage = activeSession.message();
        return true;
    }

    public synchronized Optional<NavigationSession> activeSession() {
        return Optional.ofNullable(activeSession);
    }

    public synchronized Optional<NavigationSession> preparedLookaheadSession() {
        return Optional.ofNullable(preparedLookaheadSession);
    }

    public synchronized Optional<NavigationReplanRequest> pendingReplanRequest() {
        return Optional.ofNullable(pendingReplanRequest);
    }

    public synchronized boolean hasPendingReplanRequest() {
        return pendingReplanRequest != null;
    }

    public synchronized Optional<NavigationReplanRequest> consumeReplanRequest() {
        Optional<NavigationReplanRequest> request = pendingReplanRequest();
        pendingReplanRequest = null;
        return request;
    }

    public synchronized Optional<String> latestMessage() {
        return Optional.ofNullable(latestMessage);
    }

    public synchronized NavigationSnapshot snapshot() {
        return new NavigationSnapshot(
                snapshotOf(activeSession),
                snapshotOf(preparedLookaheadSession),
                snapshotOf(pendingReplanRequest),
                Optional.ofNullable(latestMessage));
    }

    private static Optional<NavigationSessionSnapshot> snapshotOf(NavigationSession session) {
        if (session == null) {
            return Optional.empty();
        }
        return Optional.of(new NavigationSessionSnapshot(
                List.copyOf(session.path().nodes()),
                session.message(),
                session.startedAt(),
                session.goalPlan().map(TravelerNavigationState::snapshotOf)));
    }

    private static Optional<NavigationReplanRequestSnapshot> snapshotOf(NavigationReplanRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        return Optional.of(new NavigationReplanRequestSnapshot(
                request.goal(),
                request.reason(),
                request.requestedAt(),
                snapshotOf(request.activation()),
                request.startOverride(),
                request.goalPlanOverride().map(TravelerNavigationState::snapshotOf)));
    }

    private static NavigationGoalPlanSnapshot snapshotOf(NavigationGoalPlan goalPlan) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        return new NavigationGoalPlanSnapshot(
                plan.requestedGoal(),
                plan.activeGoal(),
                plan.finalSegment(),
                plan.lookaheadReplanDistance());
    }

    private static ReplanActivation snapshotOf(NavigationReplanActivation activation) {
        return switch (Objects.requireNonNull(activation, "activation")) {
            case START_NEW_SESSION -> ReplanActivation.START_NEW_SESSION;
            case PREPARE_LOOKAHEAD -> ReplanActivation.PREPARE_LOOKAHEAD;
            case REPLACE_ACTIVE_SESSION -> ReplanActivation.REPLACE_ACTIVE_SESSION;
        };
    }

    private static String requireMessage(String message) {
        return Objects.requireNonNull(message, "message");
    }

    private void activate(NavigationSession session) {
        activeSession = Objects.requireNonNull(session, "session");
        preparedLookaheadSession = null;
        pendingReplanRequest = null;
        latestMessage = session.message();
    }

    private void clearState(String message) {
        clearActiveNavigation();
        latestMessage = message;
    }

    private void clearActiveNavigation() {
        activeSession = null;
        preparedLookaheadSession = null;
        pendingReplanRequest = null;
    }
}

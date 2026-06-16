package dev.traveler.core.navigation;

import dev.traveler.core.navigation.api.NavigationSnapshot;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class TravelerNavigationState {
    private NavigationSession activeSession;
    private NavigationSession preparedLookaheadSession;
    private NavigationReplanRequest pendingReplanRequest;
    private String latestMessage;

    public synchronized void start(NavigationPath path, String message) {
        activeSession = new NavigationSession(
                Objects.requireNonNull(path, "path"),
                message,
                Instant.now());
        preparedLookaheadSession = null;
        pendingReplanRequest = null;
        latestMessage = message;
    }

    public synchronized void start(NavigationPath path, String message, NavigationGoalPlan goalPlan) {
        activeSession = new NavigationSession(
                Objects.requireNonNull(path, "path"),
                message,
                Instant.now(),
                Objects.requireNonNull(goalPlan, "goalPlan"));
        preparedLookaheadSession = null;
        pendingReplanRequest = null;
        latestMessage = message;
    }

    public synchronized void stop(String message) {
        activeSession = null;
        preparedLookaheadSession = null;
        pendingReplanRequest = null;
        latestMessage = message;
    }

    public synchronized void requestReplan(NavigationGoalPlan goalPlan, String message) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        activeSession = null;
        preparedLookaheadSession = null;
        pendingReplanRequest = new NavigationReplanRequest(plan.requestedGoal(), message, Instant.now());
        latestMessage = message;
    }

    public synchronized void requestReplan(
            NavigationGoalPlan goalPlan,
            String message,
            NavigationPoint startOverride) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        activeSession = null;
        preparedLookaheadSession = null;
        pendingReplanRequest = new NavigationReplanRequest(
                plan.requestedGoal(),
                message,
                Instant.now(),
                Objects.requireNonNull(startOverride, "startOverride"));
        latestMessage = message;
    }

    public synchronized void requestSegmentRepair(
            NavigationGoalPlan goalPlan,
            String message,
            NavigationPoint startOverride) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        if (pendingReplanRequest != null || preparedLookaheadSession != null) {
            latestMessage = message;
            return;
        }
        pendingReplanRequest = new NavigationReplanRequest(
                plan.activeGoal(),
                message,
                Instant.now(),
                NavigationReplanActivation.REPLACE_ACTIVE_SESSION,
                Objects.requireNonNull(startOverride, "startOverride"),
                plan);
        latestMessage = message;
    }

    public synchronized void requestLookaheadReplan(NavigationGoalPlan goalPlan, String message) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        if (pendingReplanRequest != null || preparedLookaheadSession != null) {
            return;
        }
        pendingReplanRequest = new NavigationReplanRequest(plan.requestedGoal(), message, Instant.now(), true);
        latestMessage = message;
    }

    public synchronized void requestLookaheadReplan(
            NavigationGoalPlan goalPlan,
            String message,
            NavigationPoint startOverride) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        if (pendingReplanRequest != null || preparedLookaheadSession != null) {
            return;
        }
        pendingReplanRequest = new NavigationReplanRequest(
                plan.requestedGoal(),
                message,
                Instant.now(),
                true,
                Objects.requireNonNull(startOverride, "startOverride"));
        latestMessage = message;
    }

    public synchronized void prepareLookahead(NavigationPath path, String message, NavigationGoalPlan goalPlan) {
        if (activeSession == null) {
            return;
        }
        preparedLookaheadSession = new NavigationSession(
                Objects.requireNonNull(path, "path"),
                message,
                Instant.now(),
                Objects.requireNonNull(goalPlan, "goalPlan"));
        pendingReplanRequest = null;
        latestMessage = message;
    }

    public synchronized void replaceActiveSession(NavigationPath path, String message, NavigationGoalPlan goalPlan) {
        if (activeSession == null) {
            return;
        }
        activeSession = new NavigationSession(
                Objects.requireNonNull(path, "path"),
                message,
                Instant.now(),
                Objects.requireNonNull(goalPlan, "goalPlan"));
        preparedLookaheadSession = null;
        pendingReplanRequest = null;
        latestMessage = message;
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
                Optional.ofNullable(activeSession),
                Optional.ofNullable(preparedLookaheadSession),
                Optional.ofNullable(pendingReplanRequest),
                Optional.ofNullable(latestMessage));
    }
}

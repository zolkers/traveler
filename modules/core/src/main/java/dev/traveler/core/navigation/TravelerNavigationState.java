package dev.traveler.core.navigation;

import dev.traveler.core.navigation.follow.NavigationPath;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class TravelerNavigationState {
    private NavigationSession activeSession;
    private NavigationReplanRequest pendingReplanRequest;
    private String latestMessage;

    public synchronized void start(NavigationPath path, String message) {
        activeSession = new NavigationSession(
                Objects.requireNonNull(path, "path"),
                message,
                Instant.now());
        pendingReplanRequest = null;
        latestMessage = message;
    }

    public synchronized void start(NavigationPath path, String message, NavigationGoalPlan goalPlan) {
        activeSession = new NavigationSession(
                Objects.requireNonNull(path, "path"),
                message,
                Instant.now(),
                Objects.requireNonNull(goalPlan, "goalPlan"));
        pendingReplanRequest = null;
        latestMessage = message;
    }

    public synchronized void stop(String message) {
        activeSession = null;
        pendingReplanRequest = null;
        latestMessage = message;
    }

    public synchronized void requestReplan(NavigationGoalPlan goalPlan, String message) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        activeSession = null;
        pendingReplanRequest = new NavigationReplanRequest(plan.requestedGoal(), message, Instant.now());
        latestMessage = message;
    }

    public synchronized void requestLookaheadReplan(NavigationGoalPlan goalPlan, String message) {
        NavigationGoalPlan plan = Objects.requireNonNull(goalPlan, "goalPlan");
        if (pendingReplanRequest != null) {
            return;
        }
        pendingReplanRequest = new NavigationReplanRequest(plan.requestedGoal(), message, Instant.now(), true);
        latestMessage = message;
    }

    public synchronized Optional<NavigationSession> activeSession() {
        return Optional.ofNullable(activeSession);
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
}

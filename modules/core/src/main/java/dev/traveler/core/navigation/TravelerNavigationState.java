package dev.traveler.core.navigation;

import dev.traveler.core.navigation.follow.NavigationPath;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class TravelerNavigationState {
    private NavigationSession activeSession;
    private String latestMessage;

    public synchronized void start(NavigationPath path, String message) {
        activeSession = new NavigationSession(
                Objects.requireNonNull(path, "path"),
                message,
                Instant.now());
        latestMessage = message;
    }

    public synchronized void stop(String message) {
        activeSession = null;
        latestMessage = message;
    }

    public synchronized Optional<NavigationSession> activeSession() {
        return Optional.ofNullable(activeSession);
    }

    public synchronized Optional<String> latestMessage() {
        return Optional.ofNullable(latestMessage);
    }
}

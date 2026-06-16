package dev.traveler.core.navigation.api;

import dev.traveler.core.navigation.NavigationReplanRequest;
import dev.traveler.core.navigation.NavigationSession;
import java.util.Objects;
import java.util.Optional;

public record NavigationSnapshot(
        Optional<NavigationSession> active,
        Optional<NavigationSession> prepared,
        Optional<NavigationReplanRequest> pending,
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
}

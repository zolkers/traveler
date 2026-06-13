package dev.traveler.core.navigation;

import dev.traveler.core.navigation.follow.NavigationPath;
import java.time.Instant;
import java.util.Objects;

public record NavigationSession(NavigationPath path, String message, Instant startedAt) {
    public NavigationSession {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(startedAt, "startedAt");
    }
}

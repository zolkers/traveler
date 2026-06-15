package dev.traveler.core.navigation;

import dev.traveler.core.navigation.follow.NavigationPath;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record NavigationSession(
        NavigationPath path,
        String message,
        Instant startedAt,
        Optional<NavigationGoalPlan> goalPlan) {
    public NavigationSession(NavigationPath path, String message, Instant startedAt) {
        this(path, message, startedAt, Optional.empty());
    }

    public NavigationSession(NavigationPath path, String message, Instant startedAt, NavigationGoalPlan goalPlan) {
        this(path, message, startedAt, Optional.of(Objects.requireNonNull(goalPlan, "goalPlan")));
    }

    public NavigationSession {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(startedAt, "startedAt");
        goalPlan = Objects.requireNonNull(goalPlan, "goalPlan");
    }
}

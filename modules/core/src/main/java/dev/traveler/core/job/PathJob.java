package dev.traveler.core.job;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

public record PathJob<T>(String purpose, Callable<T> action, Function<T, PathJobState> stateResolver) {
    public PathJob(String purpose, Callable<T> action) {
        this(purpose, action, value -> PathJobState.FOUND);
    }

    public PathJob {
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(stateResolver, "stateResolver");
    }

    PathJobState completedState(T value) {
        PathJobState state = Objects.requireNonNull(stateResolver.apply(value), "resolved state");
        if (state == PathJobState.NOT_FOUND) {
            return state;
        }
        return PathJobState.FOUND;
    }
}

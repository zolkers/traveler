package dev.traveler.core.job;

import java.util.Optional;

public record PathJobResult<T>(long id, String purpose, PathJobState state, T value, Throwable failure) {
    public static <T> PathJobResult<T> found(long id, String purpose, T value) {
        return new PathJobResult<>(id, purpose, PathJobState.FOUND, value, null);
    }

    public static <T> PathJobResult<T> notFound(long id, String purpose, T value) {
        return new PathJobResult<>(id, purpose, PathJobState.NOT_FOUND, value, null);
    }

    public static <T> PathJobResult<T> failed(long id, String purpose, Throwable failure) {
        return new PathJobResult<>(id, purpose, PathJobState.FAILED, null, failure);
    }

    public static <T> PathJobResult<T> cancelled(long id, String purpose) {
        return new PathJobResult<>(id, purpose, PathJobState.CANCELLED, null, null);
    }

    public Optional<Throwable> failureCause() {
        return Optional.ofNullable(failure);
    }
}

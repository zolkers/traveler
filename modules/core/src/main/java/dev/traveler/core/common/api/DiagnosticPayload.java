package dev.traveler.core.common.api;

import java.util.Objects;

public record DiagnosticPayload<T>(String key, T value) {
    public DiagnosticPayload {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
    }
}

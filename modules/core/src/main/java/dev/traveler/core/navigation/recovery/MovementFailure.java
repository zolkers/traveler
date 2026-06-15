package dev.traveler.core.navigation.recovery;

import java.util.Objects;

public record MovementFailure(MovementFailureKind kind, String message) {
    public MovementFailure {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(message, "message");
    }
}

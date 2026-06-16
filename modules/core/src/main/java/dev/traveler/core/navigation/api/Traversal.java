package dev.traveler.core.navigation.api;

import java.util.Objects;

public record Traversal(
        TraversalKind kind,
        TraversalGeometry geometry) {
    public Traversal {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(geometry, "geometry");
    }
}

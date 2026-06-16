package dev.traveler.core.navigation.internal;

import dev.traveler.core.navigation.api.Traversal;
import dev.traveler.core.navigation.api.TraversalGeometry;
import dev.traveler.core.navigation.api.TraversalKind;
import java.util.Objects;

record PlannedTraversal(TraversalKind kind, TraversalGeometry geometry) implements Traversal {
    PlannedTraversal {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(geometry, "geometry");
    }
}

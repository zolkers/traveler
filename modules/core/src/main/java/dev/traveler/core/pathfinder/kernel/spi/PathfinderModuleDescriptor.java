package dev.traveler.core.pathfinder.kernel.spi;

import java.util.Objects;

public record PathfinderModuleDescriptor(String id, PathfinderModuleType type, int priority, boolean enabled) {
    public PathfinderModuleDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank.");
        }
    }
}

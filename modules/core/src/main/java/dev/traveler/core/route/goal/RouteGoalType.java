package dev.traveler.core.route.goal;

import java.util.Objects;

public record RouteGoalType(String id, String description) {
    public RouteGoalType {
        requireId(id);
        Objects.requireNonNull(description, "description");
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Goal type id must not be blank.");
        }
    }
}

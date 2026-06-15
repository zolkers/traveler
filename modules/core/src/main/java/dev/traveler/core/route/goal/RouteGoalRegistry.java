package dev.traveler.core.route.goal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RouteGoalRegistry {
    private final Map<String, RouteGoalType> types;

    private RouteGoalRegistry(Map<String, RouteGoalType> types) {
        this.types = Map.copyOf(Objects.requireNonNull(types, "types"));
    }

    public static RouteGoalRegistry standard() {
        return new Builder()
                .register(new RouteGoalType("xyz", "Exact block-space target."))
                .register(new RouteGoalType("block", "Exact block-space target alias."))
                .register(new RouteGoalType("xz", "Horizontal column target at the agent's current height."))
                .register(new RouteGoalType("y", "Vertical level target in the agent's current column."))
                .build();
    }

    public Optional<RouteGoalType> find(String id) {
        return Optional.ofNullable(types.get(Objects.requireNonNull(id, "id")));
    }

    public Collection<RouteGoalType> types() {
        return types.values();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        types.values().forEach(builder::register);
        return builder;
    }

    public static final class Builder {
        private final Map<String, RouteGoalType> types = new LinkedHashMap<>();

        public Builder register(RouteGoalType type) {
            RouteGoalType safeType = Objects.requireNonNull(type, "type");
            types.put(safeType.id(), safeType);
            return this;
        }

        public RouteGoalRegistry build() {
            return new RouteGoalRegistry(types);
        }
    }
}

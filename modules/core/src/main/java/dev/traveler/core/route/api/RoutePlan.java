package dev.traveler.core.route.api;

import java.util.List;
import java.util.Objects;

public record RoutePlan(List<RouteSegment> segments) {
    public RoutePlan {
        segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
    }
}

package dev.traveler.core.route.api;

import java.util.List;
import java.util.Objects;

public record RoutePlan(List<RouteSegment> segments) {
    public RoutePlan {
        segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
        for (int index = 0; index < segments.size(); index++) {
            if (segments.get(index).index() != index) {
                throw new IllegalArgumentException("segment indexes must match list order.");
            }
        }
    }
}

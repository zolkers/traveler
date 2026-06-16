package dev.traveler.core.route.internal;

import dev.traveler.core.route.RoutePath;
import dev.traveler.core.route.RouteStep;
import dev.traveler.core.route.api.RouteSegment;
import dev.traveler.core.route.api.RouteTraversalHint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SegmentPlanner {
    public List<RouteSegment> segments(RoutePath route) {
        RoutePath safeRoute = Objects.requireNonNull(route, "route");
        List<RouteSegment> segments = new ArrayList<>(safeRoute.steps().size());
        for (int index = 0; index < safeRoute.steps().size(); index++) {
            segments.add(segment(index, safeRoute.steps().get(index)));
        }
        return List.copyOf(segments);
    }

    private static RouteSegment segment(int index, RouteStep step) {
        return new RouteSegment(
                index,
                step.from().blockPosition(),
                step.to().blockPosition(),
                traversalHint(step.action()));
    }

    private static RouteTraversalHint traversalHint(MovementAction action) {
        return switch (Objects.requireNonNull(action, "action")) {
            case WALK, STEP_UP -> RouteTraversalHint.WALK;
            case DROP -> RouteTraversalHint.DROP;
            case JUMP -> RouteTraversalHint.JUMP;
            case SWIM -> RouteTraversalHint.SWIM;
            case CLIMB -> RouteTraversalHint.CLIMB;
            case BLOCKED -> throw new IllegalArgumentException("Blocked movement cannot become a route segment.");
        };
    }
}

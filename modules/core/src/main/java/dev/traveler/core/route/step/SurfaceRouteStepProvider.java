package dev.traveler.core.route.step;

import dev.traveler.core.route.RouteStep;
import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface SurfaceRouteStepProvider {
    Optional<List<RouteStep>> routeSteps(SurfaceRouteStepContext context);
}

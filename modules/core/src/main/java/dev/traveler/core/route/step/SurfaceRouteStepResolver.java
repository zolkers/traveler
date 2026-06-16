package dev.traveler.core.route.step;

import dev.traveler.core.route.RouteStep;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SurfaceRouteStepResolver(List<SurfaceRouteStepProvider> providers) {
    public SurfaceRouteStepResolver {
        providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    public List<RouteStep> routeSteps(SurfaceRouteStepContext context) {
        SurfaceRouteStepContext safeContext = Objects.requireNonNull(context, "context");
        for (SurfaceRouteStepProvider provider : providers) {
            Optional<List<RouteStep>> steps = provider.routeSteps(safeContext);
            if (steps.isPresent()) {
                return List.copyOf(steps.orElseThrow());
            }
        }
        throw new IllegalStateException("No surface route step provider handled the transition.");
    }
}

package dev.traveler.core.route.step;

import dev.traveler.core.route.RouteStep;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DefaultSurfaceRouteStepProvider implements SurfaceRouteStepProvider {
    @Override
    public Optional<List<RouteStep>> routeSteps(SurfaceRouteStepContext context) {
        SurfaceRouteStepContext safeContext = Objects.requireNonNull(context, "context");
        MovementDecision decision = safeContext.decision();
        if (!decision.allowed() || decision.action() == MovementAction.CLIMB) {
            return Optional.empty();
        }
        return Optional.of(List.of(new RouteStep(
                safeContext.from(),
                safeContext.to(),
                decision.action(),
                safeContext.transitionCost(),
                safeContext.pointOf(safeContext.to()))));
    }
}

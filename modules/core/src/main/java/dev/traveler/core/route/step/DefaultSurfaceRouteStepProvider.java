package dev.traveler.core.route.step;

import dev.traveler.core.common.geometry.WorldPoint;
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
                targetPoint(safeContext, decision.action()))));
    }

    private static WorldPoint targetPoint(
            SurfaceRouteStepContext context,
            MovementAction action) {
        if (action == MovementAction.JUMP || action == MovementAction.STEP_UP) {
            return context.stableLandingPointOf(context.to());
        }
        return context.pointOf(context.to());
    }
}

package dev.traveler.core.world.navigation;

import dev.traveler.core.world.behavior.decision.MovementDecision;
import java.util.Objects;
import java.util.Optional;

public final class ClimbSurfaceTransitionProvider implements SurfaceTransitionProvider {
    @Override
    public Optional<MovementDecision> decision(SurfaceTransitionContext context) {
        SurfaceTransitionContext safeContext = Objects.requireNonNull(context, "context");
        if (!SurfaceClimbTraversal.canClimb(
                safeContext.worldLayer(),
                safeContext.from(),
                safeContext.to(),
                safeContext.capabilities())) {
            return Optional.empty();
        }
        return Optional.of(MovementDecision.climb());
    }
}

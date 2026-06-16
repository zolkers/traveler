package dev.traveler.core.world.navigation;

import dev.traveler.core.world.behavior.decision.MovementDecision;
import java.util.Objects;
import java.util.Optional;

public final class DefaultSurfaceTransitionProvider implements SurfaceTransitionProvider {
    @Override
    public Optional<MovementDecision> decision(SurfaceTransitionContext context) {
        SurfaceTransitionContext safeContext = Objects.requireNonNull(context, "context");
        SurfaceMovementEvaluator evaluator = new SurfaceMovementEvaluator(safeContext.capabilities());
        return safeContext.horizontalDirection()
                .map(direction -> evaluator.decision(
                        safeContext.from(),
                        safeContext.to(),
                        safeContext.worldLayer().surfaceBlock(safeContext.from().blockPosition()),
                        safeContext.destinationBlock(),
                        direction))
                .or(() -> Optional.of(MovementDecision.blocked()));
    }
}

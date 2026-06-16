package dev.traveler.core.world.navigation;

import dev.traveler.core.world.behavior.decision.MovementDecision;
import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface SurfaceTransitionProvider {
    Optional<MovementDecision> decision(SurfaceTransitionContext context);

    static List<SurfaceTransitionProvider> standard() {
        return List.of(
                new ClimbSurfaceTransitionProvider(),
                new SwimSurfaceTransitionProvider(),
                new DefaultSurfaceTransitionProvider());
    }
}

package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.navigation.recovery.ClimbMovementHealthPolicy;
import dev.traveler.core.route.start.ClimbSurfaceRouteStartProvider;
import dev.traveler.core.route.step.ClimbSurfaceRouteStepProvider;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.navigation.ClimbSurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import java.util.List;
import java.util.Map;

public final class ClimbTraversalModule extends AbstractTraversalModule {
    public ClimbTraversalModule() {
        super(
                "traversal.climb",
                0,
                List.of(SurfaceConnectionProviders.climb()),
                List.of(new ClimbSurfaceRouteStartProvider()),
                List.of(new ClimbSurfaceTransitionProvider()),
                List.of(new ClimbSurfaceRouteStepProvider()),
                List.of(recoveryContributor()));
    }

    private static TraversalRecoveryContributor recoveryContributor() {
        return () -> Map.of(MovementAction.CLIMB, new ClimbMovementHealthPolicy());
    }
}

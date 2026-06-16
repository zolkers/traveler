package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.navigation.recovery.WalkMovementHealthPolicy;
import dev.traveler.core.route.start.StandingSurfaceRouteStartProvider;
import dev.traveler.core.route.step.DefaultSurfaceRouteStepProvider;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.navigation.DefaultSurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import java.util.List;
import java.util.Map;

public final class WalkTraversalModule extends AbstractTraversalModule {
    public WalkTraversalModule() {
        super(
                "traversal.walk",
                2,
                List.of(SurfaceConnectionProviders.adjacent()),
                List.of(new StandingSurfaceRouteStartProvider()),
                List.of(new DefaultSurfaceTransitionProvider()),
                List.of(new DefaultSurfaceRouteStepProvider()),
                List.of(recoveryContributor()));
    }

    private static TraversalRecoveryContributor recoveryContributor() {
        WalkMovementHealthPolicy walkPolicy = new WalkMovementHealthPolicy();
        return () -> Map.of(
                MovementAction.WALK, walkPolicy,
                MovementAction.BLOCKED, walkPolicy);
    }
}

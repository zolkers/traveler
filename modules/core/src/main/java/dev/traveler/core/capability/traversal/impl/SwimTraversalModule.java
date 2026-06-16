package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.navigation.recovery.SwimMovementHealthPolicy;
import dev.traveler.core.route.start.SubmergedSwimRouteStartProvider;
import dev.traveler.core.route.step.DefaultSurfaceRouteStepProvider;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import dev.traveler.core.world.navigation.SwimSurfaceTransitionProvider;
import java.util.List;
import java.util.Map;

public final class SwimTraversalModule extends AbstractTraversalModule {
    public SwimTraversalModule() {
        super(
                "traversal.swim",
                1,
                List.of(SurfaceConnectionProviders.swim()),
                List.of(new SubmergedSwimRouteStartProvider()),
                List.of(new SwimSurfaceTransitionProvider()),
                List.of(new DefaultSurfaceRouteStepProvider()),
                List.of(recoveryContributor()));
    }

    private static TraversalRecoveryContributor recoveryContributor() {
        return () -> Map.of(MovementAction.SWIM, new SwimMovementHealthPolicy());
    }
}

package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.navigation.recovery.DropMovementHealthPolicy;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import java.util.List;
import java.util.Map;

public final class DropTraversalModule extends AbstractTraversalModule {
    public DropTraversalModule() {
        super(
                "traversal.drop",
                3,
                List.of(SurfaceConnectionProviders.drop()),
                List.of(),
                List.of(),
                List.of(),
                List.of(recoveryContributor()));
    }

    private static TraversalRecoveryContributor recoveryContributor() {
        return () -> Map.of(MovementAction.DROP, new DropMovementHealthPolicy());
    }
}

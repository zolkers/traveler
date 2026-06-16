package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.navigation.recovery.JumpMovementHealthPolicy;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import java.util.List;
import java.util.Map;

public final class JumpTraversalModule extends AbstractTraversalModule {
    public JumpTraversalModule() {
        super(
                "traversal.jump",
                4,
                List.of(SurfaceConnectionProviders.jump()),
                List.of(),
                List.of(),
                List.of(),
                List.of(recoveryContributor()));
    }

    private static TraversalRecoveryContributor recoveryContributor() {
        JumpMovementHealthPolicy jumpPolicy = new JumpMovementHealthPolicy();
        return () -> Map.of(
                MovementAction.JUMP, jumpPolicy,
                MovementAction.STEP_UP, jumpPolicy);
    }
}

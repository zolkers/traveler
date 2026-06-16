package dev.traveler.core.capability.traversal.spi;

import dev.traveler.core.navigation.recovery.MovementHealthPolicy;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Map;

public interface TraversalRecoveryContributor {
    Map<MovementAction, MovementHealthPolicy> movementHealthPolicies();
}

package dev.traveler.core.navigation.recovery;

import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class MovementHealthPolicyRegistry {
    private final Map<MovementAction, MovementHealthPolicy> policies;

    public MovementHealthPolicyRegistry(Map<MovementAction, MovementHealthPolicy> policies) {
        this.policies = Map.copyOf(Objects.requireNonNull(policies, "policies"));
    }

    public static MovementHealthPolicyRegistry standard() {
        EnumMap<MovementAction, MovementHealthPolicy> policies = new EnumMap<>(MovementAction.class);
        MovementHealthPolicy walk = new WalkMovementHealthPolicy();
        policies.put(MovementAction.WALK, walk);
        policies.put(MovementAction.BLOCKED, walk);
        policies.put(MovementAction.JUMP, new JumpMovementHealthPolicy());
        policies.put(MovementAction.STEP_UP, new JumpMovementHealthPolicy());
        policies.put(MovementAction.CLIMB, new ClimbMovementHealthPolicy());
        policies.put(MovementAction.DROP, new DropMovementHealthPolicy());
        policies.put(MovementAction.SWIM, new SwimMovementHealthPolicy());
        return new MovementHealthPolicyRegistry(policies);
    }

    public MovementHealthPolicy policyFor(MovementAction action) {
        MovementAction movementAction = Objects.requireNonNull(action, "action");
        MovementHealthPolicy policy = policies.get(movementAction);
        if (policy == null) {
            throw new IllegalArgumentException("No movement health policy registered for " + movementAction + '.');
        }
        return policy;
    }
}

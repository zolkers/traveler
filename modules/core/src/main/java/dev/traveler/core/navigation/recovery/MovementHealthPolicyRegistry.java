package dev.traveler.core.navigation.recovery;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MovementHealthPolicyRegistry {
    private final Map<MovementAction, MovementHealthPolicy> policies;
    private final MovementHealthPolicy fallbackPolicy;

    public MovementHealthPolicyRegistry(Map<MovementAction, MovementHealthPolicy> policies) {
        this.policies = Map.copyOf(Objects.requireNonNull(policies, "policies"));
        fallbackPolicy = this.policies.getOrDefault(MovementAction.WALK, new WalkMovementHealthPolicy());
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

    public static MovementHealthPolicyRegistry fromTraversalModules(List<? extends TraversalModule> modules) {
        EnumMap<MovementAction, MovementHealthPolicy> policies = new EnumMap<>(MovementAction.class);
        for (TraversalModule module : enabledTraversalModules(modules)) {
            addMovementHealthPolicies(module, policies);
        }
        return new MovementHealthPolicyRegistry(policies);
    }

    public MovementHealthPolicy policyFor(MovementAction action) {
        MovementAction movementAction = Objects.requireNonNull(action, "action");
        MovementHealthPolicy policy = policies.get(movementAction);
        return policy == null ? fallbackPolicy : policy;
    }

    private static List<TraversalModule> enabledTraversalModules(List<? extends TraversalModule> modules) {
        List<TraversalModule> enabledModules = new ArrayList<>();
        for (TraversalModule module : List.copyOf(Objects.requireNonNull(modules, "modules"))) {
            if (Objects.requireNonNull(module.descriptor(), "descriptor").enabled()) {
                enabledModules.add(module);
            }
        }
        enabledModules.sort(Comparator.comparingInt(module -> module.descriptor().priority()));
        return List.copyOf(enabledModules);
    }

    private static void addMovementHealthPolicies(
            TraversalModule module,
            EnumMap<MovementAction, MovementHealthPolicy> policies) {
        for (TraversalRecoveryContributor contributor : safeRecoveryContributors(module)) {
            Map<MovementAction, MovementHealthPolicy> contribution =
                    Objects.requireNonNull(contributor.movementHealthPolicies(), "movementHealthPolicies");
            for (Map.Entry<MovementAction, MovementHealthPolicy> entry : contribution.entrySet()) {
                policies.put(
                        Objects.requireNonNull(entry.getKey(), "movementAction"),
                        Objects.requireNonNull(entry.getValue(), "movementHealthPolicy"));
            }
        }
    }

    private static List<TraversalRecoveryContributor> safeRecoveryContributors(TraversalModule module) {
        return List.copyOf(Objects.requireNonNull(module.recoveryContributors(), "recoveryContributors"));
    }
}

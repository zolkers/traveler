package dev.traveler.core.navigation.recovery;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.impl.StandardTraversalModules;
import dev.traveler.core.capability.traversal.impl.WalkTraversalModule;
import dev.traveler.core.capability.traversal.noop.NoTraversalModule;
import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalDebugContributor;
import dev.traveler.core.capability.traversal.spi.TraversalExecutionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MovementHealthPolicyRegistryTest {
    @Test
    void standardRegistryKeepsExistingMovementPolicies() {
        MovementHealthPolicyRegistry registry = MovementHealthPolicyRegistry.standard();

        assertInstanceOf(WalkMovementHealthPolicy.class, registry.policyFor(MovementAction.WALK));
        assertInstanceOf(WalkMovementHealthPolicy.class, registry.policyFor(MovementAction.BLOCKED));
        assertInstanceOf(JumpMovementHealthPolicy.class, registry.policyFor(MovementAction.JUMP));
        assertInstanceOf(JumpMovementHealthPolicy.class, registry.policyFor(MovementAction.STEP_UP));
        assertInstanceOf(ClimbMovementHealthPolicy.class, registry.policyFor(MovementAction.CLIMB));
        assertInstanceOf(DropMovementHealthPolicy.class, registry.policyFor(MovementAction.DROP));
        assertInstanceOf(SwimMovementHealthPolicy.class, registry.policyFor(MovementAction.SWIM));
    }

    @Test
    void registryCanBeBuiltFromTraversalModules() {
        MovementHealthPolicyRegistry registry =
                MovementHealthPolicyRegistry.fromTraversalModules(StandardTraversalModules.modules());

        assertInstanceOf(WalkMovementHealthPolicy.class, registry.policyFor(MovementAction.WALK));
        assertInstanceOf(WalkMovementHealthPolicy.class, registry.policyFor(MovementAction.BLOCKED));
        assertInstanceOf(JumpMovementHealthPolicy.class, registry.policyFor(MovementAction.JUMP));
        assertInstanceOf(JumpMovementHealthPolicy.class, registry.policyFor(MovementAction.STEP_UP));
        assertInstanceOf(ClimbMovementHealthPolicy.class, registry.policyFor(MovementAction.CLIMB));
        assertInstanceOf(DropMovementHealthPolicy.class, registry.policyFor(MovementAction.DROP));
    }

    @Test
    void missingTraversalPolicyFallsBackToWalkPolicy() {
        MovementHealthPolicyRegistry registry =
                MovementHealthPolicyRegistry.fromTraversalModules(List.of(new NoTraversalModule("traversal.none")));

        assertInstanceOf(WalkMovementHealthPolicy.class, registry.policyFor(MovementAction.JUMP));
        assertInstanceOf(WalkMovementHealthPolicy.class, registry.policyFor(MovementAction.CLIMB));
        assertInstanceOf(WalkMovementHealthPolicy.class, registry.policyFor(MovementAction.DROP));
        assertInstanceOf(WalkMovementHealthPolicy.class, registry.policyFor(MovementAction.SWIM));
    }

    @Test
    void disabledTraversalModulesAreIgnored() {
        MovementHealthPolicyRegistry registry = MovementHealthPolicyRegistry.fromTraversalModules(List.of(
                recoveryModule(
                        "traversal.disabled",
                        0,
                        false,
                        jumpContributor(new DisabledJumpPolicy())),
                new WalkTraversalModule()));

        assertInstanceOf(WalkMovementHealthPolicy.class, registry.policyFor(MovementAction.JUMP));
    }

    @Test
    void traversalPolicyMergeUsesSortedModuleOrder() {
        MovementHealthPolicy priorityOnePolicy = new PriorityOneJumpPolicy();
        MovementHealthPolicy priorityTenPolicy = new PriorityTenJumpPolicy();

        MovementHealthPolicyRegistry registry = MovementHealthPolicyRegistry.fromTraversalModules(List.of(
                recoveryModule(
                        "traversal.priority.ten",
                        10,
                        true,
                        jumpContributor(priorityTenPolicy)),
                recoveryModule(
                        "traversal.priority.one",
                        1,
                        true,
                        jumpContributor(priorityOnePolicy))));

        assertSame(priorityTenPolicy, registry.policyFor(MovementAction.JUMP));
    }

    @Test
    void traversalPolicyMergePreservesContributorOrderWithinModule() {
        MovementHealthPolicy firstContributorPolicy = new FirstContributorJumpPolicy();
        MovementHealthPolicy secondContributorPolicy = new SecondContributorJumpPolicy();

        MovementHealthPolicyRegistry registry = MovementHealthPolicyRegistry.fromTraversalModules(List.of(
                recoveryModule(
                        "traversal.ordered",
                        0,
                        true,
                        jumpContributor(firstContributorPolicy),
                        jumpContributor(secondContributorPolicy))));

        assertSame(secondContributorPolicy, registry.policyFor(MovementAction.JUMP));
    }

    @Test
    void rejectsNullTraversalModules() {
        assertThrows(NullPointerException.class, () -> MovementHealthPolicyRegistry.fromTraversalModules(null));
        assertThrows(
                NullPointerException.class,
                () -> MovementHealthPolicyRegistry.fromTraversalModules(
                        Arrays.asList(new WalkTraversalModule(), null)));
    }

    private static TraversalRecoveryContributor jumpContributor(MovementHealthPolicy policy) {
        return () -> Map.of(MovementAction.JUMP, policy);
    }

    private static TraversalModule recoveryModule(
            String id,
            int priority,
            boolean enabled,
            TraversalRecoveryContributor... recoveryContributors) {
        return new RecoveryModule(id, priority, enabled, List.of(recoveryContributors));
    }

    private static final class RecoveryModule implements TraversalModule {
        private final PathfinderModuleDescriptor descriptor;
        private final List<TraversalRecoveryContributor> recoveryContributors;

        private RecoveryModule(
                String id,
                int priority,
                boolean enabled,
                List<TraversalRecoveryContributor> recoveryContributors) {
            descriptor = new PathfinderModuleDescriptor(id, PathfinderModuleType.TRAVERSAL, priority, enabled);
            this.recoveryContributors = List.copyOf(recoveryContributors);
        }

        @Override
        public PathfinderModuleDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public List<TraversalConnectionContributor> connectionContributors() {
            return List.of();
        }

        @Override
        public List<TraversalRouteContributor> routeContributors() {
            return List.of();
        }

        @Override
        public List<TraversalExecutionContributor> executionContributors() {
            return List.of();
        }

        @Override
        public List<TraversalRecoveryContributor> recoveryContributors() {
            return recoveryContributors;
        }

        @Override
        public List<TraversalDebugContributor> debugContributors() {
            return List.of();
        }
    }

    private static final class DisabledJumpPolicy implements MovementHealthPolicy {
        @Override
        public MovementHealthEvaluation evaluate(MovementHealthSnapshot snapshot, MovementHealthSettings settings) {
            return MovementHealthEvaluation.idle();
        }
    }

    private static final class PriorityOneJumpPolicy implements MovementHealthPolicy {
        @Override
        public MovementHealthEvaluation evaluate(MovementHealthSnapshot snapshot, MovementHealthSettings settings) {
            return MovementHealthEvaluation.idle();
        }
    }

    private static final class PriorityTenJumpPolicy implements MovementHealthPolicy {
        @Override
        public MovementHealthEvaluation evaluate(MovementHealthSnapshot snapshot, MovementHealthSettings settings) {
            return MovementHealthEvaluation.idle();
        }
    }

    private static final class FirstContributorJumpPolicy implements MovementHealthPolicy {
        @Override
        public MovementHealthEvaluation evaluate(MovementHealthSnapshot snapshot, MovementHealthSettings settings) {
            return MovementHealthEvaluation.idle();
        }
    }

    private static final class SecondContributorJumpPolicy implements MovementHealthPolicy {
        @Override
        public MovementHealthEvaluation evaluate(MovementHealthSnapshot snapshot, MovementHealthSettings settings) {
            return MovementHealthEvaluation.idle();
        }
    }
}

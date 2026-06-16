package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.api.CollisionSemantics;
import dev.traveler.core.world.behavior.api.FluidSemantics;
import dev.traveler.core.world.behavior.api.SupportSemantics;
import dev.traveler.core.world.behavior.api.TraversalAffordance;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

final class SurfaceMovementRules {
    private SurfaceMovementRules() {}

    static boolean supportsWalking(MovementCapabilities capabilities) {
        return Objects.requireNonNull(capabilities, "capabilities").canWalk();
    }

    static MovementDecision walkStepOrJump(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        if (!safeContext.capabilities().canWalk()) {
            return MovementDecision.blocked();
        }
        return upwardMovementDecision(safeContext, true);
    }

    static BlockSemantics walkStepOrJumpSemantics(SurfaceMovementContext context) {
        return walkableSemantics(context, true);
    }

    static MovementDecision walkOrJump(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        if (!safeContext.capabilities().canWalk()) {
            return MovementDecision.blocked();
        }
        return upwardMovementDecision(safeContext, false);
    }

    static BlockSemantics walkOrJumpSemantics(SurfaceMovementContext context) {
        return walkableSemantics(context, false);
    }

    static BlockSemantics blockedWalkableSemantics() {
        return new BlockSemantics(
                CollisionSemantics.SOLID,
                SupportSemantics.STANDABLE,
                FluidSemantics.NONE,
                Set.of(),
                Set.of());
    }

    private static MovementDecision upwardMovementDecision(
            SurfaceMovementContext context, boolean allowStepUp) {
        double delta = context.floorDelta();
        if (delta <= 0.0) {
            return safeFallDecision(context.capabilities(), delta);
        }
        if (context.startsInFluid() && !context.endsInFluid()) {
            return MovementDecision.blocked();
        }
        if (allowStepUp && delta <= context.capabilities().maxStepUp()) {
            return MovementDecision.stepUp();
        }
        if (delta <= context.capabilities().maxJumpHeight()) {
            return MovementDecision.jump();
        }
        return MovementDecision.blocked();
    }

    private static MovementDecision safeFallDecision(MovementCapabilities capabilities, double delta) {
        double descent = -delta;
        if (descent <= capabilities.maxStepUp()) {
            return MovementDecision.walk();
        }
        if (descent > capabilities.maxSafeFallDistance()) {
            return MovementDecision.blocked();
        }
        if (delta < 0.0) {
            return MovementDecision.drop();
        }
        return MovementDecision.walk();
    }

    private static BlockSemantics walkableSemantics(SurfaceMovementContext context, boolean allowStepUp) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        EnumSet<TraversalAffordance> affordances = EnumSet.noneOf(TraversalAffordance.class);
        if (safeContext.capabilities().canWalk()) {
            addWalkAffordances(safeContext, allowStepUp, affordances);
        }
        return new BlockSemantics(
                CollisionSemantics.SOLID,
                SupportSemantics.STANDABLE,
                FluidSemantics.NONE,
                affordances,
                Set.of());
    }

    private static void addWalkAffordances(
            SurfaceMovementContext context,
            boolean allowStepUp,
            EnumSet<TraversalAffordance> affordances) {
        double delta = context.floorDelta();
        if (delta <= 0.0) {
            addSafeFallAffordances(context.capabilities(), delta, affordances);
            return;
        }
        if (context.startsInFluid() && !context.endsInFluid()) {
            return;
        }
        if (allowStepUp && delta <= context.capabilities().maxStepUp()) {
            affordances.add(TraversalAffordance.STEP_UP);
            return;
        }
        if (delta <= context.capabilities().maxJumpHeight()) {
            affordances.add(TraversalAffordance.JUMP);
        }
    }

    private static void addSafeFallAffordances(
            MovementCapabilities capabilities,
            double delta,
            EnumSet<TraversalAffordance> affordances) {
        double descent = -delta;
        if (descent <= capabilities.maxStepUp()) {
            affordances.add(TraversalAffordance.WALK);
            return;
        }
        if (descent > capabilities.maxSafeFallDistance()) {
            return;
        }
        if (delta < 0.0) {
            affordances.add(TraversalAffordance.DROP);
            return;
        }
        affordances.add(TraversalAffordance.WALK);
    }
}

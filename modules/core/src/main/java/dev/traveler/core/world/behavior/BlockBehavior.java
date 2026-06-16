package dev.traveler.core.world.behavior;

import dev.traveler.core.world.behavior.api.BehaviorTag;
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

public interface BlockBehavior {
    BlockBehaviorKey key();

    default SupportSemantics supportSemantics(MovementCapabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        return SupportSemantics.NONE;
    }

    default FluidSemantics fluidSemantics() {
        return FluidSemantics.NONE;
    }

    default boolean supportsStanding(MovementCapabilities capabilities) {
        return supportSemantics(capabilities).supportsStanding();
    }

    default BlockSemantics describe(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        MovementDecision decision = evaluateMovement(safeContext);
        EnumSet<TraversalAffordance> affordances = EnumSet.noneOf(TraversalAffordance.class);
        switch (decision.action()) {
            case WALK -> affordances.add(TraversalAffordance.WALK);
            case STEP_UP -> affordances.add(TraversalAffordance.STEP_UP);
            case DROP -> affordances.add(TraversalAffordance.DROP);
            case JUMP -> affordances.add(TraversalAffordance.JUMP);
            case SWIM -> affordances.add(TraversalAffordance.SWIM);
            case CLIMB -> affordances.add(TraversalAffordance.CLIMB);
            case BLOCKED -> {
            }
        }
        SupportSemantics support = supportSemantics(safeContext.capabilities());
        CollisionSemantics collision = support.supportsStanding()
                ? CollisionSemantics.SOLID
                : CollisionSemantics.PASSABLE;
        FluidSemantics fluid = fluidSemantics();
        Set<BehaviorTag> tags = preservesRouteGeometry(safeContext.capabilities())
                ? Set.of(BehaviorTag.PRESERVE_ROUTE_GEOMETRY)
                : Set.of();
        return new BlockSemantics(collision, support, fluid, affordances, tags);
    }

    default MovementDecision evaluateMovement(SurfaceMovementContext context) {
        return adaptMovementDecision(context, describe(context));
    }

    default boolean allowsRouteSmoothing(MovementCapabilities capabilities) {
        return true;
    }

    default boolean preservesRouteGeometry(MovementCapabilities capabilities) {
        return !allowsRouteSmoothing(capabilities);
    }

    static MovementDecision adaptMovementDecision(SurfaceMovementContext context, BlockSemantics semantics) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        BlockSemantics safeSemantics = Objects.requireNonNull(semantics, "semantics");
        if (safeSemantics.supports(TraversalAffordance.CLIMB)) {
            return MovementDecision.climb();
        }
        if (safeSemantics.supports(TraversalAffordance.SWIM)) {
            return safeContext.capabilities().canSwim() ? MovementDecision.swim() : MovementDecision.blocked();
        }
        if (safeSemantics.supports(TraversalAffordance.STEP_UP)) {
            return MovementDecision.stepUp();
        }
        if (safeSemantics.supports(TraversalAffordance.JUMP)) {
            return MovementDecision.jump();
        }
        if (safeSemantics.supports(TraversalAffordance.DROP)) {
            return MovementDecision.drop();
        }
        if (safeSemantics.supports(TraversalAffordance.WALK)) {
            return MovementDecision.walk();
        }
        return MovementDecision.blocked();
    }
}

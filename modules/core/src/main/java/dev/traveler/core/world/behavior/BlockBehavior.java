package dev.traveler.core.world.behavior;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.api.FluidSemantics;
import dev.traveler.core.world.behavior.api.SupportSemantics;
import dev.traveler.core.world.behavior.api.TraversalAffordance;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public interface BlockBehavior {
    BlockPosition PROBE_POSITION = new BlockPosition(0, 0, 0);
    SurfaceNode PROBE_NODE = new SurfaceNode(PROBE_POSITION, 0, 0, 0.0);
    MovementCapabilities PROBE_CAPABILITIES = new MovementCapabilities(true, false, false, false, 0.0, 0.0, 0.0);

    BlockBehaviorKey key();

    default SupportSemantics supportSemantics(MovementCapabilities capabilities) {
        return describe(probeContext(Objects.requireNonNull(capabilities, "capabilities"))).support();
    }

    default FluidSemantics fluidSemantics() {
        return describe(probeContext(PROBE_CAPABILITIES)).fluid();
    }

    default boolean supportsStanding(MovementCapabilities capabilities) {
        return supportSemantics(capabilities).supportsStanding();
    }

    BlockSemantics describe(SurfaceMovementContext context);

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

    private static SurfaceMovementContext probeContext(MovementCapabilities capabilities) {
        SurfaceBlock probeBlock = SurfaceBlock.empty();
        return new SurfaceMovementContext(
                PROBE_NODE,
                PROBE_NODE,
                probeBlock,
                probeBlock,
                capabilities,
                MovementDirection.north());
    }
}

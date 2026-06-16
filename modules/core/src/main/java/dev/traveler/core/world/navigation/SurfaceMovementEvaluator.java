package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public final class SurfaceMovementEvaluator {
    private final MovementCapabilities capabilities;

    public SurfaceMovementEvaluator(MovementCapabilities capabilities) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    public MovementDecision decision(SurfaceNode from, SurfaceNode to, SurfaceBlock destinationBlock) {
        return decision(
                from,
                to,
                SurfaceBlock.solid(BlockShape.fullCube()),
                destinationBlock,
                directionBetween(from, to));
    }

    MovementDecision decision(
            SurfaceNode from,
            SurfaceNode to,
            SurfaceBlock sourceBlock,
            SurfaceBlock destinationBlock,
            MovementDirection direction) {
        SurfaceNode safeFrom = Objects.requireNonNull(from, "from");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        SurfaceBlock safeSourceBlock = Objects.requireNonNull(sourceBlock, "sourceBlock");
        SurfaceBlock block = Objects.requireNonNull(destinationBlock, "destinationBlock");
        MovementDirection movementDirection = Objects.requireNonNull(direction, "direction");
        SurfaceMovementContext context =
                new SurfaceMovementContext(
                        safeFrom,
                        safeTo,
                        safeSourceBlock,
                        block,
                        capabilities,
                        movementDirection);
        return dev.traveler.core.world.behavior.BlockBehavior.adaptMovementDecision(
                context,
                block.behavior().describe(context));
    }

    private static MovementDirection directionBetween(SurfaceNode from, SurfaceNode to) {
        SurfaceNode safeFrom = Objects.requireNonNull(from, "from");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        int xOffset = Double.compare(safeTo.centerX(), safeFrom.centerX());
        int zOffset = Double.compare(safeTo.centerZ(), safeFrom.centerZ());
        return MovementDirection.fromOffset(xOffset, zOffset);
    }
}

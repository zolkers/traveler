package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;
import java.util.Optional;

public final class SwimSurfaceTransitionProvider implements SurfaceTransitionProvider {
    @Override
    public Optional<MovementDecision> decision(SurfaceTransitionContext context) {
        SurfaceTransitionContext safeContext = Objects.requireNonNull(context, "context");
        if (!safeContext.capabilities().canSwim()) {
            return Optional.empty();
        }
        if (isFluid(safeContext.destinationBlock())) {
            return Optional.of(MovementDecision.swim());
        }
        if (sameHorizontalCell(safeContext.from(), safeContext.to())
                && safeContext.to().floorY() > safeContext.from().floorY()
                && isFluid(safeContext.worldLayer().surfaceBlock(safeContext.from().blockPosition()))) {
            return Optional.of(MovementDecision.swim());
        }
        return Optional.empty();
    }

    private static boolean sameHorizontalCell(SurfaceNode from, SurfaceNode to) {
        return from.blockPosition().x() == to.blockPosition().x()
                && from.blockPosition().z() == to.blockPosition().z()
                && from.cellX() == to.cellX()
                && from.cellZ() == to.cellZ();
    }

    private static boolean isFluid(SurfaceBlock block) {
        return block.classification().fluidHandling() == FluidHandling.ALLOW;
    }
}

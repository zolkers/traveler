package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;

final class SurfaceMovementTestSupport {
    private SurfaceMovementTestSupport() {}

    static SurfaceMovementContext context(
            SurfaceNode from,
            SurfaceNode to,
            MovementDirection direction,
            MovementCapabilities capabilities) {
        return new SurfaceMovementContext(from, to, capabilities, direction);
    }
}

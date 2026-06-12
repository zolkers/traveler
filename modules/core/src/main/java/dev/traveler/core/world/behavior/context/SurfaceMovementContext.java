package dev.traveler.core.world.behavior.context;

import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public record SurfaceMovementContext(
        SurfaceNode from,
        SurfaceNode to,
        MovementCapabilities capabilities,
        MovementDirection direction) {
    public SurfaceMovementContext {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(direction, "direction");
    }

    public double floorDelta() {
        return to.floorY() - from.floorY();
    }
}

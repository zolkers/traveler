package dev.traveler.core.world.behavior.context;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.api.FluidSemantics;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public record SurfaceMovementContext(
        SurfaceNode from,
        SurfaceNode to,
        SurfaceBlock fromBlock,
        SurfaceBlock toBlock,
        MovementCapabilities capabilities,
        MovementDirection direction) {
    public SurfaceMovementContext {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(fromBlock, "fromBlock");
        Objects.requireNonNull(toBlock, "toBlock");
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(direction, "direction");
    }

    public SurfaceMovementContext(
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities,
            MovementDirection direction) {
        this(from, to, dev.traveler.core.layer.SurfaceBlock.empty(), dev.traveler.core.layer.SurfaceBlock.empty(),
                capabilities, direction);
    }

    public double floorDelta() {
        return to.floorY() - from.floorY();
    }

    public boolean startsInFluid() {
        return fromBlock.behavior().fluidSemantics() == FluidSemantics.SWIMMABLE;
    }

    public boolean endsInFluid() {
        return toBlock.behavior().fluidSemantics() == FluidSemantics.SWIMMABLE;
    }
}

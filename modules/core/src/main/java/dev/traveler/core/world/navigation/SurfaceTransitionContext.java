package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;
import java.util.Optional;

public record SurfaceTransitionContext(
        SurfaceWorldLayer worldLayer,
        SurfaceNode from,
        SurfaceNode to,
        MovementCapabilities capabilities,
        SurfaceBlock destinationBlock,
        MovementDirection direction) {
    public SurfaceTransitionContext {
        Objects.requireNonNull(worldLayer, "worldLayer");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(destinationBlock, "destinationBlock");
    }

    public SurfaceTransitionContext(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        this(
                worldLayer,
                from,
                to,
                capabilities,
                Objects.requireNonNull(worldLayer, "worldLayer").surfaceBlock(
                        Objects.requireNonNull(to, "to").blockPosition()),
                null);
    }

    public Optional<MovementDirection> horizontalDirection() {
        return direction == null ? directionBetween(from, to) : Optional.of(direction);
    }

    public MovementDirection direction() {
        return horizontalDirection().orElseThrow(() ->
                new IllegalArgumentException("Movement direction cannot be zero."));
    }

    private static Optional<MovementDirection> directionBetween(SurfaceNode from, SurfaceNode to) {
        SurfaceNode safeFrom = Objects.requireNonNull(from, "from");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        int xOffset = Double.compare(safeTo.centerX(), safeFrom.centerX());
        int zOffset = Double.compare(safeTo.centerZ(), safeFrom.centerZ());
        if (xOffset == 0 && zOffset == 0) {
            return Optional.empty();
        }
        return Optional.of(MovementDirection.fromOffset(xOffset, zOffset));
    }
}

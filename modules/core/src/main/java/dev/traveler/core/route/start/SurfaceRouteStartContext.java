package dev.traveler.core.route.start;

import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.Objects;

public record SurfaceRouteStartContext(
        SurfaceWorldLayer worldLayer,
        SurfaceNodeResolver surfaceNodes,
        BlockPosition feetPosition,
        MovementProfile movementProfile) {
    public SurfaceRouteStartContext {
        Objects.requireNonNull(worldLayer, "worldLayer");
        Objects.requireNonNull(surfaceNodes, "surfaceNodes");
        Objects.requireNonNull(feetPosition, "feetPosition");
        Objects.requireNonNull(movementProfile, "movementProfile");
    }
}

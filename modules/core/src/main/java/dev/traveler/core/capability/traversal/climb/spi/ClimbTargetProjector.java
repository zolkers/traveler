package dev.traveler.core.capability.traversal.climb.spi;

import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Optional;

@FunctionalInterface
public interface ClimbTargetProjector {
    Optional<WorldPoint> climbFaceTarget(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities);
}

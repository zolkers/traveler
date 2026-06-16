package dev.traveler.core.capability.traversal.climb.impl;

import dev.traveler.core.capability.traversal.climb.spi.ClimbTargetProjector;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.navigation.SurfaceClimbTraversal;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Optional;

public final class DefaultClimbTargetProjector implements ClimbTargetProjector {
    @Override
    public Optional<WorldPoint> climbFaceTarget(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        return SurfaceClimbTraversal.climbFaceTarget(worldLayer, from, to, capabilities);
    }
}

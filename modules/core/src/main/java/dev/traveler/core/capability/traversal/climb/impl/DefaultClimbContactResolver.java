package dev.traveler.core.capability.traversal.climb.impl;

import dev.traveler.core.capability.traversal.climb.spi.ClimbContactResolver;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.navigation.SurfaceClimbTraversal;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Optional;

public final class DefaultClimbContactResolver implements ClimbContactResolver {
    @Override
    public Optional<List<SurfaceNode>> climbRouteNodes(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        return SurfaceClimbTraversal.climbRouteNodes(worldLayer, from, to, capabilities);
    }
}

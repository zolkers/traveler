package dev.traveler.core.route.start;

import dev.traveler.core.world.navigation.SurfaceClimbTraversal;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Objects;

public final class ClimbSurfaceRouteStartProvider implements SurfaceRouteStartProvider {
    @Override
    public List<SurfaceNode> startNodes(SurfaceRouteStartContext context) {
        SurfaceRouteStartContext safeContext = Objects.requireNonNull(context, "context");
        return SurfaceClimbTraversal.climbStartNodes(
                safeContext.worldLayer(),
                safeContext.feetPosition(),
                safeContext.movementProfile().capabilities());
    }
}

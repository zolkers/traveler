package dev.traveler.core.route.start;

import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;

@FunctionalInterface
public interface SurfaceRouteStartProvider {
    List<SurfaceNode> startNodes(SurfaceRouteStartContext context);
}

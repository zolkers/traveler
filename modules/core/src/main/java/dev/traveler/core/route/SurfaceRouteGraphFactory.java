package dev.traveler.core.route;

import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.surface.SurfaceNode;

@FunctionalInterface
public interface SurfaceRouteGraphFactory {
    Graph<SurfaceNode> create(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            SurfaceNode goal,
            RouteSearchSettings settings);
}

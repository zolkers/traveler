package dev.traveler.core.route;

import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.navigation.SurfaceTraversalGraph;
import dev.traveler.core.world.navigation.SurfaceTraversalGraphSettings;
import dev.traveler.core.world.surface.SurfaceNode;

final class DefaultSurfaceRouteGraphFactory implements SurfaceRouteGraphFactory {
    @Override
    public Graph<SurfaceNode> create(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            SurfaceNode goal,
            RouteSearchSettings settings) {
        return new SurfaceTraversalGraph(
                worldLayer,
                start,
                goal,
                settings.movementProfile(),
                SurfaceTraversalGraphSettings.standard(
                        settings.horizontalMargin(),
                        settings.verticalMargin()));
    }
}

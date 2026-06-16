package dev.traveler.core.route;

import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import dev.traveler.core.world.navigation.SurfaceTransitionResolver;
import dev.traveler.core.route.internal.SurfaceTraversalFeatures;
import dev.traveler.core.world.navigation.SurfaceTraversalGraph;
import dev.traveler.core.world.navigation.SurfaceTraversalGraphSettings;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Objects;

final class DefaultSurfaceRouteGraphFactory implements SurfaceRouteGraphFactory {
    private final List<SurfaceConnectionProvider> connectionProviders;
    private final SurfaceTransitionResolver transitionResolver;

    DefaultSurfaceRouteGraphFactory() {
        this(
                SurfaceConnectionProvider.standard(),
                SurfaceTraversalFeatures.transitionResolver(SurfaceTraversalFeatures.standard()));
    }

    DefaultSurfaceRouteGraphFactory(
            List<SurfaceConnectionProvider> connectionProviders,
            SurfaceTransitionResolver transitionResolver) {
        this.connectionProviders = List.copyOf(Objects.requireNonNull(connectionProviders, "connectionProviders"));
        this.transitionResolver = Objects.requireNonNull(transitionResolver, "transitionResolver");
    }

    List<SurfaceConnectionProvider> connectionProviders() {
        return connectionProviders;
    }

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
                        settings.verticalMargin())
                        .withConnectionProviders(connectionProviders)
                        .withTransitionResolver(transitionResolver));
    }
}

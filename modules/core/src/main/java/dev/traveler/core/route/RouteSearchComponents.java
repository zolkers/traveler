package dev.traveler.core.route;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.impl.StandardTraversalModules;
import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.Pathfinder;
import dev.traveler.core.route.internal.SurfaceTraversalFeature;
import dev.traveler.core.route.internal.SurfaceTraversalFeatures;
import dev.traveler.core.route.start.SurfaceRouteStartResolver;
import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.SurfaceRouteStepResolver;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.smooth.PathSmoothingSelector;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import dev.traveler.core.world.navigation.SurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SurfaceTransitionResolver;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RouteSearchComponents(
        BlockRouteGraphFactory blockGraphFactory,
        SurfaceRouteGraphFactory surfaceGraphFactory,
        SurfaceRouteStartResolver surfaceStartResolver,
        SurfaceTransitionResolver surfaceTransitionResolver,
        SurfaceRouteStepResolver surfaceRouteStepResolver,
        Pathfinder<BlockPosition> blockPathfinder,
        Pathfinder<SurfaceNode> surfacePathfinder,
        PathSmoothingSelector<BlockPosition> blockSmoothingSelector,
        PathSmoothingSelector<SurfaceNode> surfaceSmoothingSelector) {
    public RouteSearchComponents {
        Objects.requireNonNull(blockGraphFactory, "blockGraphFactory");
        Objects.requireNonNull(surfaceGraphFactory, "surfaceGraphFactory");
        Objects.requireNonNull(surfaceStartResolver, "surfaceStartResolver");
        Objects.requireNonNull(surfaceTransitionResolver, "surfaceTransitionResolver");
        Objects.requireNonNull(surfaceRouteStepResolver, "surfaceRouteStepResolver");
        Objects.requireNonNull(blockPathfinder, "blockPathfinder");
        Objects.requireNonNull(surfacePathfinder, "surfacePathfinder");
        Objects.requireNonNull(blockSmoothingSelector, "blockSmoothingSelector");
        Objects.requireNonNull(surfaceSmoothingSelector, "surfaceSmoothingSelector");
    }

    public static RouteSearchComponents standard() {
        return withTraversalModules(StandardTraversalModules.modules());
    }

    public static RouteSearchComponents withTraversalModules(List<? extends TraversalModule> modules) {
        TraversalProviders providers = traversalProviders(modules);
        SurfaceTransitionResolver transitionResolver =
                new SurfaceTransitionResolver(providers.transitionProviders());
        return new RouteSearchComponents(
                new DefaultBlockRouteGraphFactory(),
                new DefaultSurfaceRouteGraphFactory(
                        providers.connectionProviders(),
                        transitionResolver),
                new SurfaceRouteStartResolver(providers.routeStartProviders()),
                transitionResolver,
                new SurfaceRouteStepResolver(providers.routeStepProviders()),
                new AStarPathfinder<>(),
                new AStarPathfinder<>(),
                PathSmoothingSelector.farthestReachable(),
                PathSmoothingSelector.farthestReachable());
    }

    public RouteSearchComponents withSurfaceGraphFactory(SurfaceRouteGraphFactory factory) {
        return new RouteSearchComponents(
                blockGraphFactory,
                factory,
                surfaceStartResolver,
                surfaceTransitionResolver,
                surfaceRouteStepResolver,
                blockPathfinder,
                surfacePathfinder,
                blockSmoothingSelector,
                surfaceSmoothingSelector);
    }

    public RouteSearchComponents withSurfaceTraversalFeatures(List<SurfaceTraversalFeature> features) {
        List<SurfaceTraversalFeature> safeFeatures = List.copyOf(Objects.requireNonNull(features, "features"));
        SurfaceTransitionResolver transitionResolver = SurfaceTraversalFeatures.transitionResolver(safeFeatures);
        return new RouteSearchComponents(
                blockGraphFactory,
                new DefaultSurfaceRouteGraphFactory(
                        SurfaceTraversalFeatures.connectionProviders(safeFeatures),
                        transitionResolver),
                SurfaceTraversalFeatures.startResolver(safeFeatures),
                transitionResolver,
                SurfaceTraversalFeatures.routeStepResolver(safeFeatures),
                blockPathfinder,
                surfacePathfinder,
                blockSmoothingSelector,
                surfaceSmoothingSelector);
    }

    public RouteSearchComponents withSurfaceStartResolver(SurfaceRouteStartResolver resolver) {
        return new RouteSearchComponents(
                blockGraphFactory,
                surfaceGraphFactory,
                resolver,
                surfaceTransitionResolver,
                surfaceRouteStepResolver,
                blockPathfinder,
                surfacePathfinder,
                blockSmoothingSelector,
                surfaceSmoothingSelector);
    }

    public RouteSearchComponents withSurfaceSmoothingSelector(PathSmoothingSelector<SurfaceNode> selector) {
        return new RouteSearchComponents(
                blockGraphFactory,
                surfaceGraphFactory,
                surfaceStartResolver,
                surfaceTransitionResolver,
                surfaceRouteStepResolver,
                blockPathfinder,
                surfacePathfinder,
                blockSmoothingSelector,
                selector);
    }

    private static TraversalProviders traversalProviders(List<? extends TraversalModule> modules) {
        List<TraversalModule> safeModules = List.copyOf(Objects.requireNonNull(modules, "modules"));
        List<SurfaceConnectionProvider> connectionProviders = new ArrayList<>();
        List<SurfaceRouteStartProvider> routeStartProviders = new ArrayList<>();
        List<SurfaceTransitionProvider> transitionProviders = new ArrayList<>();
        List<SurfaceRouteStepProvider> routeStepProviders = new ArrayList<>();
        for (TraversalModule module : safeModules) {
            if (!module.descriptor().enabled()) {
                continue;
            }
            addConnectionProviders(module, connectionProviders);
            addRouteProviders(module, routeStartProviders, transitionProviders, routeStepProviders);
        }
        return new TraversalProviders(
                connectionProviders,
                routeStartProviders,
                transitionProviders,
                routeStepProviders);
    }

    private static void addConnectionProviders(
            TraversalModule module,
            List<SurfaceConnectionProvider> providers) {
        for (TraversalConnectionContributor contributor : module.connectionContributors()) {
            providers.addAll(contributor.surfaceConnectionProviders());
        }
    }

    private static void addRouteProviders(
            TraversalModule module,
            List<SurfaceRouteStartProvider> routeStartProviders,
            List<SurfaceTransitionProvider> transitionProviders,
            List<SurfaceRouteStepProvider> routeStepProviders) {
        for (TraversalRouteContributor contributor : module.routeContributors()) {
            routeStartProviders.addAll(contributor.routeStartProviders());
            transitionProviders.addAll(contributor.transitionProviders());
            routeStepProviders.addAll(contributor.routeStepProviders());
        }
    }

    private record TraversalProviders(
            List<SurfaceConnectionProvider> connectionProviders,
            List<SurfaceRouteStartProvider> routeStartProviders,
            List<SurfaceTransitionProvider> transitionProviders,
            List<SurfaceRouteStepProvider> routeStepProviders) {
        private TraversalProviders {
            connectionProviders =
                    List.copyOf(Objects.requireNonNull(connectionProviders, "connectionProviders"));
            routeStartProviders =
                    List.copyOf(Objects.requireNonNull(routeStartProviders, "routeStartProviders"));
            transitionProviders =
                    List.copyOf(Objects.requireNonNull(transitionProviders, "transitionProviders"));
            routeStepProviders =
                    List.copyOf(Objects.requireNonNull(routeStepProviders, "routeStepProviders"));
        }
    }
}

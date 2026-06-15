package dev.traveler.core.route;

import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.Pathfinder;
import dev.traveler.core.route.start.SurfaceRouteStartResolver;
import dev.traveler.core.route.step.SurfaceRouteStepResolver;
import dev.traveler.core.smooth.PathSmoothingSelector;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.navigation.SurfaceTransitionResolver;
import dev.traveler.core.world.navigation.SurfaceTraversalFeature;
import dev.traveler.core.world.navigation.SurfaceTraversalFeatures;
import dev.traveler.core.world.surface.SurfaceNode;
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
        List<SurfaceTraversalFeature> traversalFeatures = SurfaceTraversalFeatures.standard();
        SurfaceTransitionResolver transitionResolver = SurfaceTraversalFeatures.transitionResolver(traversalFeatures);
        return new RouteSearchComponents(
                new DefaultBlockRouteGraphFactory(),
                new DefaultSurfaceRouteGraphFactory(
                        SurfaceTraversalFeatures.connectionProviders(traversalFeatures),
                        transitionResolver),
                SurfaceTraversalFeatures.startResolver(traversalFeatures),
                transitionResolver,
                SurfaceTraversalFeatures.routeStepResolver(traversalFeatures),
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
}

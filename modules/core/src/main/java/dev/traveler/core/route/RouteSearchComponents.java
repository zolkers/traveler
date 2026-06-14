package dev.traveler.core.route;

import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.Pathfinder;
import dev.traveler.core.smooth.PathSmoothingSelector;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public record RouteSearchComponents(
        BlockRouteGraphFactory blockGraphFactory,
        SurfaceRouteGraphFactory surfaceGraphFactory,
        Pathfinder<BlockPosition> blockPathfinder,
        Pathfinder<SurfaceNode> surfacePathfinder,
        PathSmoothingSelector<BlockPosition> blockSmoothingSelector,
        PathSmoothingSelector<SurfaceNode> surfaceSmoothingSelector) {
    public RouteSearchComponents {
        Objects.requireNonNull(blockGraphFactory, "blockGraphFactory");
        Objects.requireNonNull(surfaceGraphFactory, "surfaceGraphFactory");
        Objects.requireNonNull(blockPathfinder, "blockPathfinder");
        Objects.requireNonNull(surfacePathfinder, "surfacePathfinder");
        Objects.requireNonNull(blockSmoothingSelector, "blockSmoothingSelector");
        Objects.requireNonNull(surfaceSmoothingSelector, "surfaceSmoothingSelector");
    }

    public static RouteSearchComponents standard() {
        return new RouteSearchComponents(
                new DefaultBlockRouteGraphFactory(),
                new DefaultSurfaceRouteGraphFactory(),
                new AStarPathfinder<>(),
                new AStarPathfinder<>(),
                PathSmoothingSelector.farthestReachable(),
                PathSmoothingSelector.farthestReachable());
    }

    public RouteSearchComponents withSurfaceGraphFactory(SurfaceRouteGraphFactory factory) {
        return new RouteSearchComponents(
                blockGraphFactory,
                factory,
                blockPathfinder,
                surfacePathfinder,
                blockSmoothingSelector,
                surfaceSmoothingSelector);
    }

    public RouteSearchComponents withSurfaceSmoothingSelector(PathSmoothingSelector<SurfaceNode> selector) {
        return new RouteSearchComponents(
                blockGraphFactory,
                surfaceGraphFactory,
                blockPathfinder,
                surfacePathfinder,
                blockSmoothingSelector,
                selector);
    }
}

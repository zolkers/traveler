package dev.traveler.core.route;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.route.api.RoutePlan;
import dev.traveler.core.route.internal.DefaultRoutePlanner;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

public final class RouteSearchService {
    private final DefaultRoutePlanner planner;

    public RouteSearchService(RouteSearchSettings settings) {
        this(new DefaultRoutePlanner(settings));
    }

    public RouteSearchService(RouteSearchSettings settings, RouteSearchComponents components) {
        this(new DefaultRoutePlanner(settings, components));
    }

    RouteSearchService(DefaultRoutePlanner planner) {
        this.planner = Objects.requireNonNull(planner, "planner");
    }

    public RouteSearchResult search(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target) {
        return planner.search(worldLayer, start, target);
    }

    public RouteSearchResult search(
            WorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal) {
        return planner.search(worldLayer, start, goal);
    }

    public RoutePlan plan(RouteSearchResult result) {
        return planner.plan(result);
    }

    static double distance(BlockPosition from, BlockPosition to) {
        int deltaX = Math.abs(from.x() - to.x());
        int deltaZ = Math.abs(from.z() - to.z());
        int straight = Math.max(deltaX, deltaZ) - Math.min(deltaX, deltaZ);
        return straight + Math.min(deltaX, deltaZ) * Math.sqrt(2.0) + Math.abs(from.y() - to.y()) * 0.5;
    }
}

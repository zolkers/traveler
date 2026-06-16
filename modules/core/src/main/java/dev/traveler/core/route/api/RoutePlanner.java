package dev.traveler.core.route.api;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;

public interface RoutePlanner {
    RoutePlan plan(WorldLayer world, BlockPosition start, RouteGoal goal);
}

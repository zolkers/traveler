package dev.traveler.core.route.api;

import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Optional;

public interface RoutePlanner {
    Optional<RoutePlan> plan(BlockPosition start, RouteGoal goal);
}

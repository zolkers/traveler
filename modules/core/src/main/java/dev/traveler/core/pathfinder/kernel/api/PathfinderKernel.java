package dev.traveler.core.pathfinder.kernel.api;

import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementProfile;

public interface PathfinderKernel {
    PathfinderKernelResult findRoute(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            MovementProfile movementProfile);

    PathfinderKernelMode mode();
}

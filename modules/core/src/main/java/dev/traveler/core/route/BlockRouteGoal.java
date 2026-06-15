package dev.traveler.core.route;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

record BlockRouteGoal(BlockPosition target) implements RouteGoal {
    BlockRouteGoal {
        Objects.requireNonNull(target, "target");
    }

    @Override
    public Optional<BlockPosition> requestedTarget() {
        return Optional.of(target);
    }

    @Override
    public List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver, BlockPosition start) {
        SurfaceNodeResolver safeResolver = Objects.requireNonNull(resolver, "resolver");
        List<SurfaceNode> targetSurfaces = safeResolver.surfaces(target);
        if (!targetSurfaces.isEmpty()) {
            return targetSurfaces;
        }
        List<SurfaceNode> climbLandings = safeResolver.climbLandingSurfaces(target);
        if (!climbLandings.isEmpty()) {
            return climbLandings;
        }
        List<SurfaceNode> climbSurfaces = safeResolver.climbSurfaces(target);
        if (!climbSurfaces.isEmpty()) {
            return climbSurfaces;
        }
        return safeResolver.standingSurface(target).map(List::of).orElseGet(List::of);
    }

    @Override
    public BlockPosition blockGoal(WorldLayer worldLayer, BlockPosition start) {
        return RouteGoalSupport.passableFeetGoal(worldLayer, target);
    }

    @Override
    public BlockPosition preferredPosition(BlockPosition fallback) {
        return target;
    }

    @Override
    public boolean isSatisfiedBy(BlockPosition position) {
        return target.equals(Objects.requireNonNull(position, "position"));
    }

    @Override
    public String displayName() {
        return "xyz " + target.x() + "," + target.y() + "," + target.z();
    }
}

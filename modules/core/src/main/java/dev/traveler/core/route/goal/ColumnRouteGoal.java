package dev.traveler.core.route.goal;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.List;
import java.util.Objects;

public record ColumnRouteGoal(int x, int z) implements RouteGoal {
    @Override
    public List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver, BlockPosition start) {
        BlockPosition candidateFeet = preferredPosition(start);
        List<SurfaceNode> support = RouteGoalSupport.standingSurfaceGoals(resolver, candidateFeet);
        if (!support.isEmpty()) {
            return support;
        }
        return resolver.surfaces(candidateFeet);
    }

    @Override
    public BlockPosition blockGoal(WorldLayer worldLayer, BlockPosition start) {
        return RouteGoalSupport.passableFeetGoal(worldLayer, preferredPosition(start));
    }

    @Override
    public BlockPosition preferredPosition(BlockPosition fallback) {
        BlockPosition safeFallback = Objects.requireNonNull(fallback, "fallback");
        return new BlockPosition(x, safeFallback.y(), z);
    }

    @Override
    public boolean isSatisfiedBy(BlockPosition position) {
        BlockPosition safePosition = Objects.requireNonNull(position, "position");
        return safePosition.x() == x && safePosition.z() == z;
    }

    @Override
    public String displayName() {
        return "xz " + x + "," + z;
    }
}

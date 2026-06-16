package dev.traveler.core.route.goal;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.List;
import java.util.Objects;

public record HeightRouteGoal(int y) implements RouteGoal {
    @Override
    public List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver, BlockPosition start) {
        return RouteGoalSupport.standingSurfaceGoals(resolver, preferredPosition(start));
    }

    @Override
    public BlockPosition blockGoal(WorldLayer worldLayer, BlockPosition start) {
        return RouteGoalSupport.passableFeetGoal(worldLayer, preferredPosition(start));
    }

    @Override
    public BlockPosition preferredPosition(BlockPosition fallback) {
        BlockPosition safeFallback = Objects.requireNonNull(fallback, "fallback");
        return new BlockPosition(safeFallback.x(), y, safeFallback.z());
    }

    @Override
    public boolean isSatisfiedBy(BlockPosition position) {
        return Objects.requireNonNull(position, "position").y() == y;
    }

    @Override
    public String displayName() {
        return "y " + y;
    }
}

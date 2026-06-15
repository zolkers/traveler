package dev.traveler.core.route;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.List;
import java.util.Objects;

enum UnspecifiedRouteGoal implements RouteGoal {
    INSTANCE;

    @Override
    public List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver, BlockPosition start) {
        Objects.requireNonNull(resolver, "resolver");
        return List.of();
    }

    @Override
    public BlockPosition blockGoal(WorldLayer worldLayer, BlockPosition start) {
        throw new IllegalStateException("Unspecified route goals cannot produce a block goal.");
    }

    @Override
    public BlockPosition preferredPosition(BlockPosition fallback) {
        return Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public boolean isSatisfiedBy(BlockPosition position) {
        return false;
    }

    @Override
    public String displayName() {
        return "unspecified";
    }
}

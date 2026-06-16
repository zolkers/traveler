package dev.traveler.core.route.goal;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.List;
import java.util.Objects;

final class RouteGoalSupport {
    private RouteGoalSupport() {
    }

    static BlockPosition passableFeetGoal(WorldLayer worldLayer, BlockPosition target) {
        if (worldLayer == null) {
            return target;
        }
        if (worldLayer.classify(target).passability() == BlockPassability.SOLID) {
            return target.above();
        }
        return target;
    }

    static List<SurfaceNode> standingSurfaceGoals(SurfaceNodeResolver resolver, BlockPosition feetPosition) {
        SurfaceNodeResolver safeResolver = Objects.requireNonNull(resolver, "resolver");
        List<SurfaceNode> standing = safeResolver.standingSurfaces(feetPosition);
        if (!standing.isEmpty()) {
            return standing;
        }
        return safeResolver.surfaces(feetPosition.below());
    }
}

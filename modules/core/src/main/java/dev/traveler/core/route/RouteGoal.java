package dev.traveler.core.route;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RouteGoal(Optional<BlockPosition> requestedTarget) {
    public RouteGoal {
        requestedTarget = Objects.requireNonNull(requestedTarget, "requestedTarget");
    }

    public static RouteGoal blockTarget(BlockPosition target) {
        return new RouteGoal(Optional.of(Objects.requireNonNull(target, "target")));
    }

    static RouteGoal unspecified() {
        return new RouteGoal(Optional.empty());
    }

    public List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver) {
        SurfaceNodeResolver safeResolver = Objects.requireNonNull(resolver, "resolver");
        if (requestedTarget.isEmpty()) {
            return List.of();
        }
        BlockPosition target = requestedTarget.orElseThrow();
        List<SurfaceNode> targetSurfaces = safeResolver.surfaces(target);
        if (!targetSurfaces.isEmpty()) {
            return targetSurfaces;
        }
        return safeResolver.standingSurface(target).map(List::of).orElseGet(List::of);
    }

    public BlockPosition blockGoal(WorldLayer worldLayer) {
        BlockPosition target = requestedTarget.orElseThrow();
        if (worldLayer == null) {
            return target;
        }
        if (worldLayer.classify(target).passability() == BlockPassability.SOLID) {
            return target.above();
        }
        return target;
    }

    BlockPosition preferredPosition(BlockPosition fallback) {
        return requestedTarget.orElse(fallback);
    }
}

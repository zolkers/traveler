package dev.traveler.core.debug.snapshots;

import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PathfinderDebugSnapshot(
        PathfinderResult<BlockPosition> result,
        String message,
        Instant updatedAt,
        List<SurfaceNode> surfaceNodes,
        List<NavigationPoint> routePoints) {
    public PathfinderDebugSnapshot(PathfinderResult<BlockPosition> result, String message, Instant updatedAt) {
        this(result, message, updatedAt, List.of());
    }

    public PathfinderDebugSnapshot(
            PathfinderResult<BlockPosition> result,
            String message,
            Instant updatedAt,
            List<SurfaceNode> surfaceNodes) {
        this(result, message, updatedAt, surfaceNodes, List.of());
    }

    public PathfinderDebugSnapshot {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(updatedAt, "updatedAt");
        surfaceNodes = List.copyOf(Objects.requireNonNull(surfaceNodes, "surfaceNodes"));
        routePoints = List.copyOf(Objects.requireNonNull(routePoints, "routePoints"));
    }

    public boolean hasSurfaceNodes() {
        return !surfaceNodes.isEmpty();
    }

    public boolean hasRoutePoints() {
        return !routePoints.isEmpty();
    }
}

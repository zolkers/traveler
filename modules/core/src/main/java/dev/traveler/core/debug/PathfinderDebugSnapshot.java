package dev.traveler.core.debug;

import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PathfinderDebugSnapshot(
        PathfinderResult<BlockPosition> result,
        String message,
        Instant updatedAt,
        List<SurfaceNode> surfaceNodes) {
    public PathfinderDebugSnapshot(PathfinderResult<BlockPosition> result, String message, Instant updatedAt) {
        this(result, message, updatedAt, List.of());
    }

    public PathfinderDebugSnapshot {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(updatedAt, "updatedAt");
        surfaceNodes = List.copyOf(Objects.requireNonNull(surfaceNodes, "surfaceNodes"));
    }

    public boolean hasSurfaceNodes() {
        return !surfaceNodes.isEmpty();
    }
}

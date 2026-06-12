package dev.traveler.mc.v1_21_11.common.debug;

import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.world.BlockPosition;
import java.time.Instant;
import java.util.Objects;

public record PathfinderDebugSnapshot(
        PathfinderResult<BlockPosition> result, String message, Instant updatedAt) {
    public PathfinderDebugSnapshot {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}

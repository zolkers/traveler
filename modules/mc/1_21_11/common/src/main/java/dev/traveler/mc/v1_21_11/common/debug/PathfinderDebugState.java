package dev.traveler.mc.v1_21_11.common.debug;

import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.world.BlockPosition;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class PathfinderDebugState {
    private PathfinderResult<BlockPosition> latestResult;
    private String latestMessage;
    private Instant updatedAt;

    public synchronized void update(PathfinderResult<BlockPosition> result) {
        update(result, null);
    }

    public synchronized void update(PathfinderResult<BlockPosition> result, String message) {
        latestResult = Objects.requireNonNull(result, "result");
        latestMessage = message;
        updatedAt = Instant.now();
    }

    public synchronized void clear() {
        latestResult = null;
        latestMessage = null;
        updatedAt = null;
    }

    public synchronized Optional<PathfinderResult<BlockPosition>> latestResult() {
        return Optional.ofNullable(latestResult);
    }

    public synchronized Optional<String> latestMessage() {
        return Optional.ofNullable(latestMessage);
    }

    public synchronized Optional<Instant> updatedAt() {
        return Optional.ofNullable(updatedAt);
    }
}

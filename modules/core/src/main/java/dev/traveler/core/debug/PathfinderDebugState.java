package dev.traveler.core.debug;

import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.world.BlockPosition;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class PathfinderDebugState {
    private PathfinderDebugSnapshot latestSnapshot;

    public synchronized void update(PathfinderResult<BlockPosition> result) {
        update(result, null);
    }

    public synchronized void update(PathfinderResult<BlockPosition> result, String message) {
        latestSnapshot = new PathfinderDebugSnapshot(
                copyResult(Objects.requireNonNull(result, "result")), message, Instant.now());
    }

    public synchronized void clear() {
        latestSnapshot = null;
    }

    public synchronized Optional<PathfinderDebugSnapshot> latestSnapshot() {
        return Optional.ofNullable(latestSnapshot);
    }

    public synchronized Optional<PathfinderDebugSnapshot> snapshot() {
        return latestSnapshot();
    }

    public synchronized Optional<PathfinderResult<BlockPosition>> latestResult() {
        return latestSnapshot().map(PathfinderDebugSnapshot::result);
    }

    public synchronized Optional<String> latestMessage() {
        return latestSnapshot().map(PathfinderDebugSnapshot::message);
    }

    public synchronized Optional<Instant> updatedAt() {
        return latestSnapshot().map(PathfinderDebugSnapshot::updatedAt);
    }

    private static PathfinderResult<BlockPosition> copyResult(PathfinderResult<BlockPosition> result) {
        return new PathfinderResult<>(result.status(), ImmutableGraphPath.copyOf(result.path()));
    }
}

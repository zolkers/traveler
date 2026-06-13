package dev.traveler.core.debug;

import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PathfinderDebugState {
    private PathfinderDebugSnapshot latestSnapshot;
    private NavigationDebugSnapshot latestNavigation;

    public synchronized void update(PathfinderResult<BlockPosition> result) {
        update(result, null);
    }

    public synchronized void update(PathfinderResult<BlockPosition> result, String message) {
        latestSnapshot = new PathfinderDebugSnapshot(
                copyResult(Objects.requireNonNull(result, "result")), message, Instant.now());
    }

    public synchronized void updateSurface(PathfinderResult<SurfaceNode> result) {
        updateSurface(result, null);
    }

    public synchronized void updateSurface(PathfinderResult<SurfaceNode> result, String message) {
        PathfinderResult<SurfaceNode> safeResult = Objects.requireNonNull(result, "result");
        latestSnapshot = new PathfinderDebugSnapshot(
                copySurfaceResult(safeResult), message, Instant.now(), copySurfaceNodes(safeResult));
    }

    public synchronized void clear() {
        latestSnapshot = null;
        latestNavigation = null;
    }

    public synchronized void updateNavigation(NavigationFrameInput input, NavigationControlFrame frame) {
        latestNavigation = NavigationDebugSnapshot.from(input, frame, Instant.now());
    }

    public synchronized void clearNavigation() {
        latestNavigation = null;
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

    public synchronized Optional<NavigationDebugSnapshot> latestNavigation() {
        return Optional.ofNullable(latestNavigation);
    }

    public synchronized Optional<String> navigationSummary() {
        return latestNavigation().map(DebugTextFormatter::navigationSummary);
    }

    private static PathfinderResult<BlockPosition> copyResult(PathfinderResult<BlockPosition> result) {
        return new PathfinderResult<>(result.status(), ImmutableGraphPath.copyOf(result.path()));
    }

    private static PathfinderResult<BlockPosition> copySurfaceResult(PathfinderResult<SurfaceNode> result) {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        for (SurfaceNode node : result.path()) {
            path.addNode(node.renderBlockPosition());
        }
        path.setCost(result.path().cost());
        return new PathfinderResult<>(result.status(), ImmutableGraphPath.copyOf(path));
    }

    private static List<SurfaceNode> copySurfaceNodes(PathfinderResult<SurfaceNode> result) {
        return List.copyOf(result.path().nodes());
    }

}

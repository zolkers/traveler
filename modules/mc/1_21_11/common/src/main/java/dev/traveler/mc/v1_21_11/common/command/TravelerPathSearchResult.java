package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

record TravelerPathSearchResult(
        PathfinderResult<BlockPosition> blockResult,
        Optional<PathfinderResult<SurfaceNode>> surfaceResult,
        String message) {
    TravelerPathSearchResult {
        Objects.requireNonNull(blockResult, "blockResult");
        surfaceResult = Objects.requireNonNull(surfaceResult, "surfaceResult");
    }

    void updateDebug(PathfinderDebugState debugState) {
        PathfinderDebugState state = Objects.requireNonNull(debugState, "debugState");
        if (surfaceResult.isPresent()) {
            state.updateSurface(surfaceResult.orElseThrow(), message);
            return;
        }
        state.update(blockResult, message);
    }

    Optional<NavigationPath> navigationPath() {
        if (status() != PathfinderStatus.FOUND) {
            return Optional.empty();
        }
        List<NavigationPoint> points = surfaceResult
                .map(result -> result.path().nodes().stream()
                        .map(TravelerPathSearchResult::surfacePoint)
                        .toList())
                .orElseGet(() -> blockResult.path().nodes().stream()
                        .map(NavigationPoint::blockCenter)
                        .toList());
        return navigationPath(points);
    }

    boolean alreadyAtTarget() {
        return status() == PathfinderStatus.FOUND && navigationPointCount() == 1;
    }

    PathfinderStatus status() {
        return blockResult.status();
    }

    private int navigationPointCount() {
        return surfaceResult
                .map(result -> result.path().nodeCount())
                .orElseGet(() -> blockResult.path().nodeCount());
    }

    private static Optional<NavigationPath> navigationPath(List<NavigationPoint> points) {
        if (points.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(NavigationPath.of(points));
    }

    private static NavigationPoint surfacePoint(SurfaceNode node) {
        return new NavigationPoint(node.centerX(), node.floorY(), node.centerZ());
    }
}

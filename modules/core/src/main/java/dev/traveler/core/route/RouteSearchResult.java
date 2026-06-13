package dev.traveler.core.route;

import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;
import java.util.Optional;

public record RouteSearchResult(
        PathfinderResult<BlockPosition> blockResult,
        Optional<PathfinderResult<SurfaceNode>> surfaceResult,
        Optional<RoutePath> route,
        RouteSearchDiagnostics diagnostics) {
    public RouteSearchResult {
        Objects.requireNonNull(blockResult, "blockResult");
        surfaceResult = Objects.requireNonNull(surfaceResult, "surfaceResult");
        route = Objects.requireNonNull(route, "route");
        Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public PathfinderStatus status() {
        return blockResult.status();
    }
}

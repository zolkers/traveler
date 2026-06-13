package dev.traveler.core.route;

import dev.traveler.core.graph.MutableGraphPath;
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

    public static RouteSearchResult notFound(RouteSearchDiagnostics diagnostics) {
        PathfinderResult<BlockPosition> result =
                new PathfinderResult<>(PathfinderStatus.NOT_FOUND, new MutableGraphPath<>());
        return new RouteSearchResult(
                result,
                Optional.empty(),
                Optional.empty(),
                Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}

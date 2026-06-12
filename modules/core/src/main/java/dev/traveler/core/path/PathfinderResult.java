package dev.traveler.core.path;

import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.graph.MutableGraphPath;
import java.util.Objects;

public record PathfinderResult<N>(PathfinderStatus status, GraphPath<N> path) {
    public PathfinderResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(path, "path");
    }

    static <N> PathfinderResult<N> found(GraphPath<N> path) {
        return new PathfinderResult<>(PathfinderStatus.FOUND, path);
    }

    static <N> PathfinderResult<N> notFound() {
        return new PathfinderResult<>(PathfinderStatus.NOT_FOUND, new MutableGraphPath<>());
    }

    static <N> PathfinderResult<N> running() {
        return new PathfinderResult<>(PathfinderStatus.RUNNING, new MutableGraphPath<>());
    }
}

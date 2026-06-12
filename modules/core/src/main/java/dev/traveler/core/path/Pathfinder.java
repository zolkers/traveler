package dev.traveler.core.path;

public interface Pathfinder<N> {
    PathfinderResult<N> search(PathfinderRequest<N> request);

    PathfinderResult<N> search(PathfinderRequest<N> request, long budgetNanos);
}

package dev.traveler.core.graph;

@FunctionalInterface
public interface Heuristic<N> {
    double estimate(N from, N to);
}

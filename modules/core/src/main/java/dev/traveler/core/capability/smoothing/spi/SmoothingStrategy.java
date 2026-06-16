package dev.traveler.core.capability.smoothing.spi;

import dev.traveler.core.graph.GraphPath;

public interface SmoothingStrategy<N> {
    GraphPath<N> smooth(GraphPath<N> path);
}

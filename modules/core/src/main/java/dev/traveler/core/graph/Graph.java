package dev.traveler.core.graph;

@FunctionalInterface
public interface Graph<N> {
    Iterable<Connection<N>> outgoingConnections(N node);
}

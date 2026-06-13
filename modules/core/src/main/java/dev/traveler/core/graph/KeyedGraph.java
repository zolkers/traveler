package dev.traveler.core.graph;

public interface KeyedGraph<N> extends Graph<N> {
    long keyOf(N node);
}

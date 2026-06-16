package dev.traveler.core.graph;

public record Connection<N>(N from, N to, double cost) {
    public Connection {
        if (cost < 0.0) {
            throw new IllegalArgumentException("Connection cost must be non-negative.");
        }
    }
}

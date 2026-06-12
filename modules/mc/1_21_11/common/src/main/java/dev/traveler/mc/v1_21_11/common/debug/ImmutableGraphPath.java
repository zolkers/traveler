package dev.traveler.mc.v1_21_11.common.debug;

import dev.traveler.core.graph.GraphPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

final class ImmutableGraphPath<N> implements GraphPath<N> {
    private final List<N> nodes;
    private final double cost;

    private ImmutableGraphPath(List<N> nodes, double cost) {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.cost = cost;
    }

    static <N> ImmutableGraphPath<N> copyOf(GraphPath<N> path) {
        Objects.requireNonNull(path, "path");
        return new ImmutableGraphPath<>(path.nodes(), path.cost());
    }

    @Override
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public int nodeCount() {
        return nodes.size();
    }

    @Override
    public N nodeAt(int index) {
        return nodes.get(index);
    }

    @Override
    public double cost() {
        return cost;
    }

    @Override
    public List<N> nodes() {
        return nodes;
    }

    @Override
    public Iterator<N> iterator() {
        return nodes.iterator();
    }
}

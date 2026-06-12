package dev.traveler.core.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class MutableGraphPath<N> implements GraphPath<N> {
    private final List<N> nodes = new ArrayList<>();
    private double cost;

    public void addNode(N node) {
        nodes.add(node);
    }

    public void clear() {
        nodes.clear();
        cost = 0.0;
    }

    public void setCost(double cost) {
        if (!(cost >= 0.0)) {
            throw new IllegalArgumentException("Path cost must be non-negative.");
        }
        this.cost = cost;
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
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public Iterator<N> iterator() {
        return nodes().iterator();
    }
}

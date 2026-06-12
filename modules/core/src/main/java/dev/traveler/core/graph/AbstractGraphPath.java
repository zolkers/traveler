package dev.traveler.core.graph;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractGraphPath<N> implements GraphPath<N> {
    private double cost;

    protected abstract List<N> nodeList();

    protected final void setPathCost(double cost) {
        if (!(cost >= 0.0)) {
            throw new IllegalArgumentException("Path cost must be non-negative.");
        }
        this.cost = cost;
    }

    @Override
    public final boolean isEmpty() {
        return nodeList().isEmpty();
    }

    @Override
    public final int nodeCount() {
        return nodeList().size();
    }

    @Override
    public final N nodeAt(int index) {
        return nodeList().get(index);
    }

    @Override
    public final double cost() {
        return cost;
    }

    @Override
    public List<N> nodes() {
        return Collections.unmodifiableList(nodeList());
    }

    @Override
    public final Iterator<N> iterator() {
        return nodes().iterator();
    }
}

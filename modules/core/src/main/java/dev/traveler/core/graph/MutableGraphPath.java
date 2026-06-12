package dev.traveler.core.graph;

import java.util.ArrayList;
import java.util.List;

public final class MutableGraphPath<N> extends AbstractGraphPath<N> {
    private final List<N> nodes = new ArrayList<>();

    public void addNode(N node) {
        nodes.add(node);
    }

    public void clear() {
        nodes.clear();
        setPathCost(0.0);
    }

    public void setCost(double cost) {
        setPathCost(cost);
    }

    @Override
    protected List<N> nodeList() {
        return nodes;
    }
}

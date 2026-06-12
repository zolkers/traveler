package dev.traveler.core.debug;

import dev.traveler.core.graph.AbstractGraphPath;
import dev.traveler.core.graph.GraphPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class ImmutableGraphPath<N> extends AbstractGraphPath<N> {
    private final List<N> nodes;

    private ImmutableGraphPath(List<N> nodes, double cost) {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        setPathCost(cost);
    }

    static <N> ImmutableGraphPath<N> copyOf(GraphPath<N> path) {
        Objects.requireNonNull(path, "path");
        return new ImmutableGraphPath<>(path.nodes(), path.cost());
    }

    @Override
    protected List<N> nodeList() {
        return nodes;
    }
}

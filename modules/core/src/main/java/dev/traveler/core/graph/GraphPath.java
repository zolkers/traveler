package dev.traveler.core.graph;

import java.util.List;

public interface GraphPath<N> extends Iterable<N> {
    boolean isEmpty();

    int nodeCount();

    N nodeAt(int index);

    double cost();

    List<N> nodes();
}

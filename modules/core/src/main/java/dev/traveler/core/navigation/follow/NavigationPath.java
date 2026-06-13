package dev.traveler.core.navigation.follow;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.List;
import java.util.Objects;

public record NavigationPath(List<NavigationPoint> nodes) {
    public NavigationPath {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        if (nodes.size() < 2) {
            throw new IllegalArgumentException("A navigation path needs at least two nodes.");
        }
    }

    public static NavigationPath of(List<NavigationPoint> nodes) {
        return new NavigationPath(nodes);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public NavigationPoint nodeAt(int index) {
        return nodes.get(index);
    }

    public NavigationPoint lastNode() {
        return nodes.getLast();
    }
}

package dev.traveler.core.navigation.follow;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record NavigationPath(List<NavigationPoint> nodes, List<NavigationSegmentAction> segmentActions) {
    public NavigationPath {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        segmentActions = List.copyOf(Objects.requireNonNull(segmentActions, "segmentActions"));
        if (nodes.size() < 2) {
            throw new IllegalArgumentException("A navigation path needs at least two nodes.");
        }
        if (segmentActions.size() != nodes.size() - 1) {
            throw new IllegalArgumentException("A navigation path needs one action per segment.");
        }
    }

    public static NavigationPath of(List<NavigationPoint> nodes) {
        List<NavigationPoint> safeNodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        return new NavigationPath(safeNodes, inferredActions(safeNodes.size() - 1));
    }

    public static NavigationPath of(
            List<NavigationPoint> nodes,
            List<NavigationSegmentAction> segmentActions) {
        return new NavigationPath(nodes, segmentActions);
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

    public NavigationSegmentAction actionBeforeNode(int nodeIndex) {
        if (nodeIndex <= 0 || nodeIndex >= nodes.size()) {
            throw new IndexOutOfBoundsException("Node index must target an existing segment end.");
        }
        return segmentActions.get(nodeIndex - 1);
    }

    private static List<NavigationSegmentAction> inferredActions(int count) {
        return Collections.nCopies(Math.max(count, 0), NavigationSegmentAction.INFER);
    }
}

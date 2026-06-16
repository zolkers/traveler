package dev.traveler.core.navigation.follow;

import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record NavigationPath(
        List<WorldPoint> nodes,
        List<NavigationSegmentIntent> segmentIntents) {
    public NavigationPath {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        segmentIntents = List.copyOf(Objects.requireNonNull(segmentIntents, "segmentIntents"));
        if (nodes.size() < 2) {
            throw new IllegalArgumentException("A navigation path needs at least two nodes.");
        }
        if (segmentIntents.size() != nodes.size() - 1) {
            throw new IllegalArgumentException("A navigation path needs one intent per segment.");
        }
    }

    public static NavigationPath of(List<WorldPoint> nodes) {
        List<WorldPoint> safeNodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        return new NavigationPath(safeNodes, inferredIntents(safeNodes));
    }

    public static NavigationPath of(
            List<WorldPoint> nodes,
            List<MovementAction> segmentActions) {
        List<WorldPoint> safeNodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        return new NavigationPath(safeNodes, intentsFromActions(safeNodes, segmentActions));
    }

    public static NavigationPath of(
            List<WorldPoint> nodes,
            List<MovementAction> segmentActions,
            List<WorldPoint> actionTargets) {
        return new NavigationPath(nodes, intentsFromActionsAndTargets(segmentActions, actionTargets));
    }

    public static NavigationPath withIntents(
            List<WorldPoint> nodes,
            List<NavigationSegmentIntent> segmentIntents) {
        return new NavigationPath(nodes, segmentIntents);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public WorldPoint nodeAt(int index) {
        return nodes.get(index);
    }

    public WorldPoint lastNode() {
        return nodes.getLast();
    }

    public MovementAction actionBeforeNode(int nodeIndex) {
        return segmentIntentBeforeNode(nodeIndex).action();
    }

    public WorldPoint actionTargetBeforeNode(int nodeIndex) {
        return segmentIntentBeforeNode(nodeIndex).actionTarget();
    }

    public NavigationSegmentIntent segmentIntentBeforeNode(int nodeIndex) {
        if (nodeIndex <= 0 || nodeIndex >= nodes.size()) {
            throw new IndexOutOfBoundsException("Node index must target an existing segment end.");
        }
        return segmentIntents.get(nodeIndex - 1);
    }

    public List<MovementAction> segmentActions() {
        return segmentIntents.stream().map(NavigationSegmentIntent::action).toList();
    }

    public List<WorldPoint> actionTargets() {
        return segmentIntents.stream().map(NavigationSegmentIntent::actionTarget).toList();
    }

    private static List<NavigationSegmentIntent> inferredIntents(List<WorldPoint> nodes) {
        List<MovementAction> actions = Collections.nCopies(Math.max(nodes.size() - 1, 0),
                MovementAction.WALK);
        return intentsFromActions(nodes, actions);
    }

    private static List<NavigationSegmentIntent> intentsFromActions(
            List<WorldPoint> nodes,
            List<MovementAction> segmentActions) {
        return intentsFromActionsAndTargets(segmentActions, segmentEnds(nodes));
    }

    private static List<NavigationSegmentIntent> intentsFromActionsAndTargets(
            List<MovementAction> segmentActions,
            List<WorldPoint> actionTargets) {
        List<MovementAction> actions = List.copyOf(Objects.requireNonNull(segmentActions, "segmentActions"));
        List<WorldPoint> targets = List.copyOf(Objects.requireNonNull(actionTargets, "actionTargets"));
        if (actions.size() != targets.size()) {
            throw new IllegalArgumentException("A navigation path needs one action target per segment action.");
        }
        java.util.ArrayList<NavigationSegmentIntent> intents = new java.util.ArrayList<>(actions.size());
        for (int index = 0; index < actions.size(); index++) {
            intents.add(NavigationSegmentIntent.of(actions.get(index), targets.get(index)));
        }
        return List.copyOf(intents);
    }

    private static List<WorldPoint> segmentEnds(List<WorldPoint> nodes) {
        if (nodes.size() <= 1) {
            return List.of();
        }
        return nodes.subList(1, nodes.size());
    }
}

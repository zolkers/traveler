package dev.traveler.core.route;

import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RoutePath(List<RouteStep> steps) {
    public RoutePath {
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("A route path needs at least one step.");
        }
        requireConnected(steps);
    }

    public static RoutePath of(List<RouteStep> steps) {
        return new RoutePath(steps);
    }

    public List<SurfaceNode> nodes() {
        List<SurfaceNode> nodes = new ArrayList<>(steps.size() + 1);
        nodes.add(steps.getFirst().from());
        for (RouteStep step : steps) {
            nodes.add(step.to());
        }
        return List.copyOf(nodes);
    }

    public List<WorldPoint> points() {
        List<WorldPoint> points = new ArrayList<>(steps.size() + 1);
        points.add(pointOf(steps.getFirst().from()));
        for (RouteStep step : steps) {
            points.add(pointOf(step.to()));
        }
        return List.copyOf(points);
    }

    public List<WorldPoint> actionTargets() {
        return steps.stream().map(RouteStep::targetPoint).toList();
    }

    public List<WorldPoint> executionPoints() {
        List<WorldPoint> points = new ArrayList<>(steps.size() + 1);
        points.add(pointOf(steps.getFirst().from()));
        for (RouteStep step : steps) {
            points.add(step.targetPoint());
        }
        return List.copyOf(points);
    }

    public List<MovementAction> actions() {
        return steps.stream().map(RouteStep::action).toList();
    }

    public double cost() {
        return steps.stream().mapToDouble(RouteStep::cost).sum();
    }

    public long actionCount(MovementAction action) {
        Objects.requireNonNull(action, "action");
        return steps.stream().filter(step -> step.action() == action).count();
    }

    private static void requireConnected(List<RouteStep> steps) {
        SurfaceNode previous = steps.getFirst().to();
        for (int index = 1; index < steps.size(); index++) {
            previous = requireConnectedStep(steps.get(index), previous);
        }
    }

    private static SurfaceNode requireConnectedStep(RouteStep step, SurfaceNode previous) {
        if (!step.from().equals(previous)) {
            throw new IllegalArgumentException("Route steps must be connected.");
        }
        return step.to();
    }

    private static WorldPoint pointOf(SurfaceNode node) {
        return new WorldPoint(node.centerX(), node.floorY(), node.centerZ());
    }
}

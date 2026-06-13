package dev.traveler.core.route;

import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public record RouteStep(SurfaceNode from, SurfaceNode to, MovementAction action, double cost) {
    public RouteStep {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(action, "action");
        requireTraversable(action);
        requireCost(cost);
    }

    private static void requireTraversable(MovementAction action) {
        if (action == MovementAction.BLOCKED) {
            throw new IllegalArgumentException("Route step action must be traversable.");
        }
    }

    private static void requireCost(double cost) {
        if (!Double.isFinite(cost) || cost < 0.0) {
            throw new IllegalArgumentException("Route step cost must be finite and non-negative.");
        }
    }
}

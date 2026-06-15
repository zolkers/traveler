package dev.traveler.core.route.step;

import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.navigation.SurfaceTransitionEvaluator;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public record SurfaceRouteStepContext(
        SurfaceWorldLayer worldLayer,
        SurfaceNode from,
        SurfaceNode to,
        MovementProfile movementProfile,
        SurfaceTransitionEvaluator transitionEvaluator) {
    public SurfaceRouteStepContext {
        Objects.requireNonNull(worldLayer, "worldLayer");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(movementProfile, "movementProfile");
        Objects.requireNonNull(transitionEvaluator, "transitionEvaluator");
    }

    public MovementCapabilities capabilities() {
        return movementProfile.capabilities();
    }

    public MovementDecision decision() {
        return transitionEvaluator.decision(worldLayer, from, to);
    }

    public double transitionCost() {
        return surfaceDistance(from, to);
    }

    public NavigationPoint pointOf(SurfaceNode node) {
        SurfaceNode safeNode = Objects.requireNonNull(node, "node");
        return new NavigationPoint(safeNode.centerX(), safeNode.floorY(), safeNode.centerZ());
    }

    public static double surfaceDistance(SurfaceNode from, SurfaceNode to) {
        SurfaceNode safeFrom = Objects.requireNonNull(from, "from");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        double deltaX = Math.abs(safeFrom.centerX() - safeTo.centerX());
        double deltaZ = Math.abs(safeFrom.centerZ() - safeTo.centerZ());
        return Math.hypot(deltaX, deltaZ) + Math.abs(safeFrom.floorY() - safeTo.floorY()) * 0.5;
    }
}

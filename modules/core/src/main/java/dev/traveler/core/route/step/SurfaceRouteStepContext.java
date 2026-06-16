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
    private static final double FLOOR_EPSILON = 0.001;

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

    public NavigationPoint stableLandingPointOf(SurfaceNode node) {
        SurfaceNode safeNode = Objects.requireNonNull(node, "node");
        if (!hasUniformLandingFloor(safeNode)) {
            return pointOf(safeNode);
        }
        return new NavigationPoint(
                safeNode.blockPosition().x() + 0.5,
                safeNode.floorY(),
                safeNode.blockPosition().z() + 0.5);
    }

    private boolean hasUniformLandingFloor(SurfaceNode node) {
        double localFloor = node.floorY() - node.blockPosition().y();
        for (int cellX = 0; cellX <= 1; cellX++) {
            for (int cellZ = 0; cellZ <= 1; cellZ++) {
                if (!sameFloor(localFloor, blockFloorAt(node, cellX, cellZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    private double blockFloorAt(SurfaceNode node, int cellX, int cellZ) {
        return worldLayer.surfaceBlock(node.blockPosition()).shape().floorHeightForCellOrNaN(cellX, cellZ);
    }

    private static boolean sameFloor(double expected, double actual) {
        return Double.isFinite(actual) && Math.abs(expected - actual) <= FLOOR_EPSILON;
    }

    public static double surfaceDistance(SurfaceNode from, SurfaceNode to) {
        SurfaceNode safeFrom = Objects.requireNonNull(from, "from");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        double deltaX = Math.abs(safeFrom.centerX() - safeTo.centerX());
        double deltaZ = Math.abs(safeFrom.centerZ() - safeTo.centerZ());
        return Math.hypot(deltaX, deltaZ) + Math.abs(safeFrom.floorY() - safeTo.floorY()) * 0.5;
    }
}

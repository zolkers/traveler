package dev.traveler.core.world.navigation;

import dev.traveler.core.world.movement.EntityDimensions;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

record SurfaceBodyFootprint(int minGlobalX, int maxGlobalX, int minGlobalZ, int maxGlobalZ) {
    private static final double CELL_SIZE = 0.5;
    private static final double BOUNDARY_EPSILON = 1.0E-9;

    static SurfaceBodyFootprint around(SurfaceNode node, EntityDimensions dimensions) {
        SurfaceNode surfaceNode = Objects.requireNonNull(node, "node");
        EntityDimensions entityDimensions = Objects.requireNonNull(dimensions, "dimensions");
        double radius = entityDimensions.width() * 0.5;
        return aroundCenter(surfaceNode.centerX(), surfaceNode.centerZ(), radius);
    }

    static SurfaceBodyFootprint around(NavigationPoint point, EntityDimensions dimensions) {
        NavigationPoint navigationPoint = Objects.requireNonNull(point, "point");
        EntityDimensions entityDimensions = Objects.requireNonNull(dimensions, "dimensions");
        double radius = entityDimensions.width() * 0.5;
        return aroundCenter(navigationPoint.x(), navigationPoint.z(), radius);
    }

    static SurfaceBodyFootprint adjustedAround(SurfaceNode node, EntityDimensions dimensions) {
        SurfaceNode surfaceNode = Objects.requireNonNull(node, "node");
        EntityDimensions entityDimensions = Objects.requireNonNull(dimensions, "dimensions");
        double radius = entityDimensions.width() * 0.5;
        double centerX = adjustedBodyCenter(surfaceNode.centerX(), surfaceNode.blockPosition().x(), radius);
        double centerZ = adjustedBodyCenter(surfaceNode.centerZ(), surfaceNode.blockPosition().z(), radius);
        return aroundCenter(centerX, centerZ, radius);
    }

    private static SurfaceBodyFootprint aroundCenter(double centerX, double centerZ, double radius) {
        return new SurfaceBodyFootprint(
                globalCell(centerX - radius + BOUNDARY_EPSILON),
                globalCell(centerX + radius - BOUNDARY_EPSILON),
                globalCell(centerZ - radius + BOUNDARY_EPSILON),
                globalCell(centerZ + radius - BOUNDARY_EPSILON));
    }

    private static double adjustedBodyCenter(double nodeCenter, int blockCoordinate, double radius) {
        if (radius >= 0.5) {
            return nodeCenter;
        }
        double min = blockCoordinate + radius;
        double max = blockCoordinate + 1.0 - radius;
        return Math.clamp(nodeCenter, min, max);
    }

    private static int globalCell(double coordinate) {
        return (int) Math.floor(coordinate / CELL_SIZE);
    }
}

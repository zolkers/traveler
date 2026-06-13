package dev.traveler.core.world.navigation;

import dev.traveler.core.world.movement.EntityDimensions;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

record SurfaceBodyFootprint(int minGlobalX, int maxGlobalX, int minGlobalZ, int maxGlobalZ) {
    private static final double CELL_SIZE = 0.5;
    private static final double BOUNDARY_EPSILON = 1.0E-9;

    static SurfaceBodyFootprint around(SurfaceNode node, EntityDimensions dimensions) {
        SurfaceNode surfaceNode = Objects.requireNonNull(node, "node");
        EntityDimensions entityDimensions = Objects.requireNonNull(dimensions, "dimensions");
        double radius = entityDimensions.width() * 0.5;
        return new SurfaceBodyFootprint(
                globalCell(surfaceNode.centerX() - radius + BOUNDARY_EPSILON),
                globalCell(surfaceNode.centerX() + radius - BOUNDARY_EPSILON),
                globalCell(surfaceNode.centerZ() - radius + BOUNDARY_EPSILON),
                globalCell(surfaceNode.centerZ() + radius - BOUNDARY_EPSILON));
    }

    private static int globalCell(double coordinate) {
        return (int) Math.floor(coordinate / CELL_SIZE);
    }
}

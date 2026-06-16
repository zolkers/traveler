package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.EntityDimensions;
import java.util.Objects;

public final class SurfaceBodyClearance {
    private static final double BODY_EPSILON = 0.0001;

    private SurfaceBodyClearance() {}

    public static boolean hasClearance(
            SurfaceWorldLayer worldLayer,
            WorldPoint position,
            EntityDimensions dimensions) {
        SurfaceWorldLayer world = Objects.requireNonNull(worldLayer, "worldLayer");
        WorldPoint point = Objects.requireNonNull(position, "position");
        EntityDimensions entityDimensions = Objects.requireNonNull(dimensions, "dimensions");
        double minY = point.y() + BODY_EPSILON;
        double maxY = point.y() + entityDimensions.height();
        SurfaceBodyFootprint footprint = SurfaceBodyFootprint.around(point, entityDimensions);
        for (int y = (int) Math.floor(minY) - 1; y <= (int) Math.floor(maxY); y++) {
            if (collidesWithFootprint(world, footprint, y, minY, maxY)) {
                return false;
            }
        }
        return true;
    }

    private static boolean collidesWithFootprint(
            SurfaceWorldLayer world,
            SurfaceBodyFootprint footprint,
            int blockY,
            double minY,
            double maxY) {
        for (int globalX = footprint.minGlobalX(); globalX <= footprint.maxGlobalX(); globalX++) {
            for (int globalZ = footprint.minGlobalZ(); globalZ <= footprint.maxGlobalZ(); globalZ++) {
                if (collidesWithBodyCell(world, globalX, globalZ, blockY, minY, maxY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean collidesWithBodyCell(
            SurfaceWorldLayer world,
            int globalX,
            int globalZ,
            int blockY,
            double minY,
            double maxY) {
        int blockX = blockCoordinate(globalX);
        int blockZ = blockCoordinate(globalZ);
        double localMinY = minY - blockY;
        double localMaxY = maxY - blockY;
        if (localMaxY <= 0.0) {
            return false;
        }
        return world.surfaceBlock(new BlockPosition(blockX, blockY, blockZ))
                .shape()
                .collidesWithCellBody(cellCoordinate(globalX), cellCoordinate(globalZ), localMinY, localMaxY);
    }

    private static int blockCoordinate(int globalCoordinate) {
        return Math.floorDiv(globalCoordinate, 2);
    }

    private static int cellCoordinate(int globalCoordinate) {
        return Math.floorMod(globalCoordinate, 2);
    }
}

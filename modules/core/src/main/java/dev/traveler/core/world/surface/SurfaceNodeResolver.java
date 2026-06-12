package dev.traveler.core.world.surface;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.layer.SurfaceWorldLayer;
import java.util.Objects;
import java.util.Optional;

public final class SurfaceNodeResolver {
    private static final int CENTER_CELL = 1;
    private static final double STANDING_RANGE = 1.0;
    private static final double FLOOR_EPSILON = 0.001;

    private final SurfaceWorldLayer worldLayer;

    public SurfaceNodeResolver(SurfaceWorldLayer worldLayer) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
    }

    public Optional<SurfaceNode> centeredSurface(BlockPosition supportPosition) {
        return surfaceAt(supportPosition, CENTER_CELL, CENTER_CELL);
    }

    public Optional<SurfaceNode> standingSurface(BlockPosition feetPosition) {
        Optional<SurfaceNode> feetSurface = standingCandidate(feetPosition, feetPosition);
        Optional<SurfaceNode> belowSurface = standingCandidate(feetPosition, feetPosition.below());
        return highestSurface(feetSurface, belowSurface);
    }

    public Optional<SurfaceNode> surfaceAt(BlockPosition supportPosition, int cellX, int cellZ) {
        BlockPosition safePosition = Objects.requireNonNull(supportPosition, "supportPosition");
        double floor = worldLayer.surfaceBlock(safePosition).shape().floorHeightForCellOrNaN(cellX, cellZ);
        if (Double.isNaN(floor)) {
            return Optional.empty();
        }
        return Optional.of(new SurfaceNode(safePosition, cellX, cellZ, safePosition.y() + floor));
    }

    private Optional<SurfaceNode> standingCandidate(BlockPosition feetPosition, BlockPosition supportPosition) {
        return centeredSurface(supportPosition).filter(node -> isStandingSurface(feetPosition, node));
    }

    private static Optional<SurfaceNode> highestSurface(
            Optional<SurfaceNode> first,
            Optional<SurfaceNode> second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty() || first.orElseThrow().floorY() >= second.orElseThrow().floorY()) {
            return first;
        }
        return second;
    }

    private static boolean isStandingSurface(BlockPosition feetPosition, SurfaceNode node) {
        double minFloor = feetPosition.y() - FLOOR_EPSILON;
        double maxFloor = feetPosition.y() + STANDING_RANGE + FLOOR_EPSILON;
        return node.floorY() >= minFloor && node.floorY() <= maxFloor;
    }
}

package dev.traveler.core.world.surface;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.SurfaceCell;
import java.util.Objects;

public record SurfaceNode(BlockPosition blockPosition, int cellX, int cellZ, double floorY) {
    public SurfaceNode {
        Objects.requireNonNull(blockPosition, "blockPosition");
        SurfaceCell.requireIndex(cellX, "cellX");
        SurfaceCell.requireIndex(cellZ, "cellZ");
        if (!Double.isFinite(floorY)) {
            throw new IllegalArgumentException("Floor Y must be finite.");
        }
    }

    public boolean sameSubcell(SurfaceNode other) {
        Objects.requireNonNull(other, "other");
        return blockPosition.equals(other.blockPosition) && cellX == other.cellX && cellZ == other.cellZ;
    }

    public BlockPosition renderBlockPosition() {
        return new BlockPosition(blockPosition.x(), (int) Math.floor(floorY), blockPosition.z());
    }

    public double centerX() {
        return blockPosition.x() + SurfaceCell.centerOffset(cellX);
    }

    public double centerZ() {
        return blockPosition.z() + SurfaceCell.centerOffset(cellZ);
    }
}

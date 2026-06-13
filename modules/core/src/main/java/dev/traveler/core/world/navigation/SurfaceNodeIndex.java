package dev.traveler.core.world.navigation;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

final class SurfaceNodeIndex {
    private static final int CELLS_PER_BLOCK = 4;

    private final SearchBounds bounds;
    private final int width;
    private final int depth;
    private final int size;

    SurfaceNodeIndex(SearchBounds bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.width = bounds.width();
        this.depth = bounds.depth();
        this.size = Math.multiplyExact(blockCount(bounds), CELLS_PER_BLOCK);
    }

    int size() {
        return size;
    }

    int indexOf(SurfaceNode node) {
        SurfaceNode safeNode = Objects.requireNonNull(node, "node");
        BlockPosition position = safeNode.blockPosition();
        if (!bounds.contains(position)) {
            throw new IllegalArgumentException("Surface node is outside search bounds.");
        }
        return blockIndex(position) * CELLS_PER_BLOCK + cellIndex(safeNode);
    }

    private int blockIndex(BlockPosition position) {
        int xIndex = position.x() - bounds.minX();
        int yIndex = position.y() - bounds.minY();
        int zIndex = position.z() - bounds.minZ();
        return (yIndex * depth + zIndex) * width + xIndex;
    }

    private static int cellIndex(SurfaceNode node) {
        return node.cellX() * 2 + node.cellZ();
    }

    private static int blockCount(SearchBounds bounds) {
        int area = Math.multiplyExact(bounds.width(), bounds.depth());
        return Math.multiplyExact(area, bounds.height());
    }
}

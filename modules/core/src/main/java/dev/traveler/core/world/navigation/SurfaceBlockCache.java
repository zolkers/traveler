package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

final class SurfaceBlockCache {
    private final SurfaceWorldLayer worldLayer;
    private final SearchBounds bounds;
    private final SurfaceBlock[] blocks;
    private final boolean[] loaded;
    private final int width;
    private final int depth;

    SurfaceBlockCache(SurfaceWorldLayer worldLayer, SearchBounds bounds) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.width = bounds.width();
        this.depth = bounds.depth();
        this.blocks = new SurfaceBlock[cacheSize(bounds)];
        this.loaded = new boolean[blocks.length];
    }

    SurfaceBlock get(BlockPosition position) {
        Objects.requireNonNull(position, "position");
        return get(position.x(), position.y(), position.z());
    }

    SurfaceBlock get(int x, int y, int z) {
        if (!bounds.contains(x, y, z)) {
            return worldLayer.surfaceBlock(x, y, z);
        }
        return cached(x, y, z);
    }

    private SurfaceBlock cached(int x, int y, int z) {
        int index = indexOf(x, y, z);
        if (!loaded[index]) {
            blocks[index] = worldLayer.surfaceBlock(x, y, z);
            loaded[index] = true;
        }
        return blocks[index];
    }

    private int indexOf(int x, int y, int z) {
        int xIndex = x - bounds.minX();
        int yIndex = y - bounds.minY();
        int zIndex = z - bounds.minZ();
        return (yIndex * depth + zIndex) * width + xIndex;
    }

    private static int cacheSize(SearchBounds bounds) {
        int area = Math.multiplyExact(bounds.width(), bounds.depth());
        return Math.multiplyExact(area, bounds.height());
    }
}

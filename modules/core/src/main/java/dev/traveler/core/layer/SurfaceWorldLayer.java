package dev.traveler.core.layer;

import dev.traveler.core.world.block.BlockPosition;

public interface SurfaceWorldLayer extends WorldLayer {
    SurfaceBlock surfaceBlock(BlockPosition position);

    default SurfaceBlock surfaceBlock(int x, int y, int z) {
        return surfaceBlock(new BlockPosition(x, y, z));
    }

    @Override
    default BlockClassification classify(BlockPosition position) {
        return surfaceBlock(position).classification();
    }
}

package dev.traveler.core.layer;

import dev.traveler.core.world.block.BlockPosition;

public interface SurfaceWorldLayer extends WorldLayer {
    SurfaceBlock surfaceBlock(BlockPosition position);

    @Override
    default BlockClassification classify(BlockPosition position) {
        return surfaceBlock(position).classification();
    }
}

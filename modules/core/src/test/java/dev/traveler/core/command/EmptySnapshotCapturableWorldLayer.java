package dev.traveler.core.command;

import dev.traveler.core.layer.SnapshotCapturableWorldLayer;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.block.BlockPosition;

abstract class EmptySnapshotCapturableWorldLayer implements SnapshotCapturableWorldLayer {
    @Override
    public SurfaceBlock surfaceBlock(BlockPosition position) {
        return SurfaceBlock.empty();
    }
}

package dev.traveler.core.layer;

import dev.traveler.core.world.block.BlockPosition;

public interface SnapshotCapturableWorldLayer extends SurfaceWorldLayer {
    SnapshotCaptureSession captureSession(
            BlockPosition start,
            BlockPosition target,
            int horizontalMargin,
            int verticalMargin);
}

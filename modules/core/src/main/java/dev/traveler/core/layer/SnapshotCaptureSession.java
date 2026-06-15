package dev.traveler.core.layer;

public interface SnapshotCaptureSession {
    boolean captureNext(int blockBudget, long deadlineNanos);

    long blockCount();

    long capturedBlocks();

    SurfaceWorldLayer snapshot();
}

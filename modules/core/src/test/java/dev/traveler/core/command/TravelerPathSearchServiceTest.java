package dev.traveler.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.SnapshotCaptureSession;
import dev.traveler.core.layer.SnapshotCapturableWorldLayer;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import org.junit.jupiter.api.Test;

class TravelerPathSearchServiceTest {
    @Test
    void searchesSnapshotCapturableWorldsThroughCoreContract() {
        CapturableLayer layer = new CapturableLayer(1_000L);
        TravelerPathSearchService service = new TravelerPathSearchService(() -> layer);
        TravelerCommandSource source = new TestSource(new BlockPosition(4, 70, -2));

        TravelerPathSearchSubmission submission =
                service.blockPathSubmission(source, new BlockPosition(8, 71, -2), "path:block");

        assertTrue(submission.snapshotSearch().isPresent());
        assertEquals(new BlockPosition(4, 70, -2), layer.start);
        assertEquals(new BlockPosition(8, 71, -2), layer.target);
        assertEquals(TravelerPathSearchService.searchSettings().horizontalMargin(), layer.horizontalMargin);
        assertEquals(TravelerPathSearchService.searchSettings().verticalMargin(), layer.verticalMargin);
    }

    @Test
    void rejectsOversizedSnapshotSearchesBeforeCapturing() {
        CapturableLayer layer = new CapturableLayer(1_000_000L);
        TravelerPathSearchService service = new TravelerPathSearchService(() -> layer);

        TravelerCommandSource source = new TestSource(new BlockPosition(0, 64, 0));

        TravelerPathSearchSubmission submission =
                service.blockPathSubmission(source, new BlockPosition(0, 10_000, 0), "path:block");

        assertTrue(submission.immediateResult().isPresent());
        assertTrue(submission.immediateResult().orElseThrow().message().contains("search-too-large"));
        assertFalse(layer.captureStarted);
    }

    @Test
    void clipsLongDistanceSnapshotSearchesInsteadOfRejectingTheWholeGoal() {
        CapturableLayer layer = new CapturableLayer(1_000L);
        TravelerPathSearchService service = new TravelerPathSearchService(() -> layer);
        TravelerCommandSource source = new TestSource(new BlockPosition(0, 64, 0));

        TravelerPathSearchSubmission submission =
                service.blockPathSubmission(source, new BlockPosition(10_000, 64, 10_000), "path:block");

        assertTrue(submission.snapshotSearch().isPresent());
        assertTrue(layer.captureStarted);
        assertEquals(new BlockPosition(72, 64, 72), layer.target);
    }

    private static final class CapturableLayer implements SnapshotCapturableWorldLayer {
        private final long blockCount;
        private boolean captureStarted;
        private BlockPosition start;
        private BlockPosition target;
        private int horizontalMargin;
        private int verticalMargin;

        private CapturableLayer(long blockCount) {
            this.blockCount = blockCount;
        }

        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return SurfaceBlock.empty();
        }

        @Override
        public SnapshotCaptureSession captureSession(
                BlockPosition start,
                BlockPosition target,
                int horizontalMargin,
                int verticalMargin) {
            captureStarted = true;
            this.start = start;
            this.target = target;
            this.horizontalMargin = horizontalMargin;
            this.verticalMargin = verticalMargin;
            return new SnapshotCaptureSession() {
                @Override
                public boolean captureNext(int blockBudget, long deadlineNanos) {
                    return true;
                }

                @Override
                public long blockCount() {
                    return blockCount;
                }

                @Override
                public long capturedBlocks() {
                    return blockCount;
                }

                @Override
                public SurfaceWorldLayer snapshot() {
                    return CapturableLayer.this;
                }
            };
        }
    }

    private record TestSource(BlockPosition blockPosition) implements TravelerCommandSource {
        @Override
        public void reply(String message) {}
    }
}

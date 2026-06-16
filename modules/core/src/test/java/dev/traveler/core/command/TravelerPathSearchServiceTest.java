package dev.traveler.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.NavigationBudgetProvider;
import dev.traveler.core.layer.SnapshotCaptureSession;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.WorldNavigationBudget;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Optional;
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
        assertEquals(new BlockPosition(52, 64, 52), layer.target);
        assertEquals(8, layer.horizontalMargin);
        assertEquals(16, layer.verticalMargin);
    }

    @Test
    void longDistanceSnapshotSearchUsesTheSameMarginsAsItsCapture() {
        CapturableLayer layer = new CapturableLayer(1_000L);
        TravelerPathSearchService service = new TravelerPathSearchService(() -> layer);
        TravelerCommandSource source = new TestSource(new BlockPosition(0, 64, 0));

        TravelerPathSearchSubmission submission =
                service.blockPathSubmission(source, new BlockPosition(10_000, 64, 10_000), "path:block");

        TravelerPathSearchService.SnapshotBlockSearch snapshotSearch =
                submission.snapshotSearch().orElseThrow();
        assertEquals(8, snapshotSearch.routeSearchSettings().horizontalMargin());
        assertEquals(16, snapshotSearch.routeSearchSettings().verticalMargin());
    }

    @Test
    void adaptsLongDistanceSnapshotSearchesToWorldNavigationBudget() {
        BudgetedCapturableLayer layer = new BudgetedCapturableLayer(1_000L, new WorldNavigationBudget(64));
        TravelerPathSearchService service = new TravelerPathSearchService(() -> layer);
        TravelerCommandSource source = new TestSource(new BlockPosition(0, 64, 0));

        TravelerPathSearchSubmission submission =
                service.blockPathSubmission(source, new BlockPosition(10_000, 64, 10_000), "path:block");

        CapturableLayer captured = layer;
        assertTrue(submission.snapshotSearch().isPresent());
        assertTrue(captured.captureStarted);
        assertEquals(new BlockPosition(32, 64, 32), captured.target);
        assertEquals(8, captured.horizontalMargin);
        assertEquals(16, captured.verticalMargin);
    }

    @Test
    void replanSnapshotSearchUsesExplicitSegmentStartInsteadOfCurrentSourcePosition() {
        CapturableLayer layer = new CapturableLayer(1_000L);
        TravelerPathSearchService service = new TravelerPathSearchService(() -> layer);
        TravelerCommandSource source = new TestSource(new BlockPosition(14, 64, 0));
        BlockPosition segmentEnd = new BlockPosition(20, 64, 0);

        TravelerPathSearchSubmission submission = service.goalPathSubmission(
                source,
                RouteGoal.xz(10_000, 0),
                "navigate:block",
                Optional.of(new NavigationPoint(segmentEnd.x() + 0.25, segmentEnd.y(), segmentEnd.z() + 0.75)));

        assertTrue(submission.snapshotSearch().isPresent());
        assertEquals(segmentEnd, layer.start);
    }

    private static class CapturableLayer extends EmptySnapshotCapturableWorldLayer {
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

    private static final class BudgetedCapturableLayer extends CapturableLayer implements NavigationBudgetProvider {
        private final WorldNavigationBudget budget;

        private BudgetedCapturableLayer(long blockCount, WorldNavigationBudget budget) {
            super(blockCount);
            this.budget = budget;
        }

        @Override
        public WorldNavigationBudget navigationBudget() {
            return budget;
        }
    }

    private record TestSource(BlockPosition blockPosition) implements TravelerCommandSource {
        @Override
        public void reply(String message) {}
    }
}

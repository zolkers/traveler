package dev.traveler.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.layer.SnapshotCaptureSession;
import dev.traveler.core.layer.SnapshotCapturableWorldLayer;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.world.block.BlockPosition;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class TravelerCommandRuntimeTest {
    @Test
    void pathTestRepliesAndStoresDebugResultWithoutMinecraftBindings() {
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerCommandRuntime runtime =
                new TravelerCommandRuntime(debugState, new TravelerNavigationState(), () -> null);
        RecordingSource source = new RecordingSource(null);

        TravelerCommandResponse response = runtime.pathCommands().pathTest(source);

        assertEquals("path test status=FOUND nodes=2", response.message());
        assertEquals(List.of("path test status=FOUND nodes=2"), source.replies);
        assertTrue(debugState.latestResult().isPresent());
    }

    @Test
    void navigateStopClearsNavigationStateAndDebugNavigation() {
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TravelerCommandRuntime runtime = new TravelerCommandRuntime(debugState, navigationState, () -> null);
        RecordingSource source = new RecordingSource(new BlockPosition(0, 64, 0));

        TravelerCommandResponse response = runtime.navigateCommands().stop(source);

        assertEquals("navigation stopped | debug nav cleared", response.message());
        assertEquals(List.of("navigation stopped | debug nav cleared"), source.replies);
        assertTrue(navigationState.activeSession().isEmpty());
        assertEquals("navigation stopped", navigationState.latestMessage().orElseThrow());
    }

    @Test
    void failedPathJobsReportTheFailureCause() {
        TravelerCommandRuntime runtime = new TravelerCommandRuntime(
                new PathfinderDebugState(),
                new TravelerNavigationState(),
                ExplodingWorld::new);
        RecordingSource source = new RecordingSource(new BlockPosition(0, 64, 0));

        runtime.navigateCommands().block(source, new BlockPosition(2, 64, 0));
        waitForJobs(runtime, () -> source.replies.stream().anyMatch(reply -> reply.contains("failed id=")));

        assertTrue(source.replies.getLast().contains("navigate:block failed id="));
        assertTrue(source.replies.getLast().contains("IllegalStateException: synthetic path failure"));
    }

    private static void waitForJobs(TravelerCommandRuntime runtime, BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(2L).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            runtime.drainPathJobs();
            Thread.onSpinWait();
        }
        assertTrue(condition.getAsBoolean());
    }

    private static final class RecordingSource implements TravelerCommandSource {
        private final BlockPosition blockPosition;
        private final List<String> replies = new ArrayList<>();

        private RecordingSource(BlockPosition blockPosition) {
            this.blockPosition = blockPosition;
        }

        @Override
        public BlockPosition blockPosition() {
            return blockPosition;
        }

        @Override
        public void reply(String message) {
            replies.add(message);
        }
    }

    private static final class ExplodingWorld implements SnapshotCapturableWorldLayer {
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
            return new SnapshotCaptureSession() {
                @Override
                public boolean captureNext(int blockBudget, long deadlineNanos) {
                    return true;
                }

                @Override
                public long blockCount() {
                    return 1L;
                }

                @Override
                public long capturedBlocks() {
                    return 1L;
                }

                @Override
                public SurfaceWorldLayer snapshot() {
                    return new FailingSnapshot();
                }
            };
        }
    }

    private static final class FailingSnapshot implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            throw new IllegalStateException("synthetic path failure");
        }
    }
}

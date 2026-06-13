package dev.traveler.core.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class PathJobExecutorTest {
    @Test
    void completesJobOnTravelerWorkerThread() {
        try (PathJobExecutor executor = new PathJobExecutor()) {
            PathJobHandle<String> handle = executor.submit(new PathJob<>(
                    "test", () -> Thread.currentThread().getName()));

            waitUntil(handle::isDone);

            assertEquals(PathJobState.FOUND, handle.state());
            assertTrue(handle.result().isPresent());
            assertTrue(handle.result().orElseThrow().value().startsWith("Traveler-Pathfinder"));
        }
    }

    @Test
    void cancelsQueuedJobBeforeItStarts() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (PathJobExecutor executor = new PathJobExecutor()) {
            PathJobHandle<String> first = executor.submit(new PathJob<>(
                    "blocking", () -> waitForRelease(started, release)));
            PathJobHandle<String> second = executor.submit(new PathJob<>("queued", () -> "should-not-run"));

            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertEquals(PathJobState.QUEUED, second.state());
            assertTrue(second.cancel());
            release.countDown();
            waitUntil(first::isDone);
            waitUntil(second::isDone);

            assertEquals(PathJobState.FOUND, first.state());
            assertEquals(PathJobState.CANCELLED, second.state());
            assertFalse(second.result().isPresent());
        }
    }

    @Test
    void cancelsRunningJobAndInterruptsWorker() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);

        try (PathJobExecutor executor = new PathJobExecutor()) {
            PathJobHandle<String> handle = executor.submit(new PathJob<>(
                    "running", () -> waitForInterrupt(started, interrupted)));

            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertEquals(PathJobState.RUNNING, handle.state());
            assertTrue(handle.cancel());

            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
            assertEquals(PathJobState.CANCELLED, handle.state());
            assertFalse(handle.result().isPresent());
        }
    }

    private static String waitForRelease(CountDownLatch started, CountDownLatch release) {
        started.countDown();
        try {
            release.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "interrupted";
        }
        return "released";
    }

    private static String waitForInterrupt(CountDownLatch started, CountDownLatch interrupted) {
        started.countDown();
        while (!Thread.currentThread().isInterrupted()) {
            Thread.onSpinWait();
        }
        interrupted.countDown();
        return "interrupted";
    }

    private static void waitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(2L).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertTrue(condition.getAsBoolean());
    }
}

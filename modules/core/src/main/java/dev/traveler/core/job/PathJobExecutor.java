package dev.traveler.core.job;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class PathJobExecutor implements AutoCloseable {
    private static final String THREAD_NAME = "Traveler-Pathfinder";

    private final AtomicLong nextId = new AtomicLong(1L);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory());

    public <T> PathJobHandle<T> submit(PathJob<T> job) {
        PathJobHandle<T> handle = new PathJobHandle<>(nextId.getAndIncrement(), Objects.requireNonNull(job, "job"));
        Future<?> future = executor.submit(() -> run(handle, job));
        handle.attach(future);
        return handle;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private static <T> void run(PathJobHandle<T> handle, PathJob<T> job) {
        if (!handle.markRunning()) {
            return;
        }
        try {
            handle.complete(job.action().call());
        } catch (Exception exception) {
            handle.fail(exception);
        }
    }

    private static ThreadFactory threadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        };
    }
}

package dev.traveler.core.job;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public final class PathJobHandle<T> {
    private final long id;
    private final PathJob<T> job;
    private final AtomicReference<PathJobState> state = new AtomicReference<>(PathJobState.QUEUED);
    private final AtomicReference<PathJobResult<T>> result = new AtomicReference<>();
    private final CompletableFuture<PathJobResult<T>> completion = new CompletableFuture<>();

    private volatile Future<?> future;

    PathJobHandle(long id, PathJob<T> job) {
        this.id = id;
        this.job = Objects.requireNonNull(job, "job");
    }

    public long id() {
        return id;
    }

    public String purpose() {
        return job.purpose();
    }

    public PathJobState state() {
        return state.get();
    }

    public boolean isDone() {
        return isTerminal(state());
    }

    public Optional<PathJobResult<T>> result() {
        return Optional.ofNullable(result.get());
    }

    public CompletableFuture<PathJobResult<T>> completion() {
        return completion;
    }

    public boolean cancel() {
        PathJobState current = state.get();
        while (!isTerminal(current)) {
            if (state.compareAndSet(current, PathJobState.CANCELLED)) {
                cancelFuture();
                completion.complete(PathJobResult.cancelled(id, purpose()));
                return true;
            }
            current = state.get();
        }
        return false;
    }

    void attach(Future<?> future) {
        this.future = Objects.requireNonNull(future, "future");
    }

    boolean markRunning() {
        return state.compareAndSet(PathJobState.QUEUED, PathJobState.RUNNING);
    }

    void complete(T value) {
        PathJobState completedState = job.completedState(value);
        PathJobResult<T> completed = resultFor(completedState, value);
        if (!state.compareAndSet(PathJobState.RUNNING, completedState)) {
            return;
        }
        result.set(completed);
        completion.complete(completed);
    }

    void fail(Throwable failure) {
        PathJobResult<T> failed = PathJobResult.failed(id, purpose(), failure);
        if (!state.compareAndSet(PathJobState.RUNNING, PathJobState.FAILED)) {
            return;
        }
        result.set(failed);
        completion.complete(failed);
    }

    private PathJobResult<T> resultFor(PathJobState completedState, T value) {
        if (completedState == PathJobState.NOT_FOUND) {
            return PathJobResult.notFound(id, purpose(), value);
        }
        return PathJobResult.found(id, purpose(), value);
    }

    private void cancelFuture() {
        Future<?> attached = future;
        if (attached != null) {
            attached.cancel(true);
        }
    }

    private static boolean isTerminal(PathJobState state) {
        return state == PathJobState.FOUND
                || state == PathJobState.NOT_FOUND
                || state == PathJobState.FAILED
                || state == PathJobState.CANCELLED;
    }
}

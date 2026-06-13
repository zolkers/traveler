package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.command.TravelerCommandFeedback;
import dev.traveler.core.command.TravelerCommandResult;
import dev.traveler.core.debug.DebugTextFormatter;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.job.PathJob;
import dev.traveler.core.job.PathJobExecutor;
import dev.traveler.core.job.PathJobHandle;
import dev.traveler.core.job.PathJobState;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.world.block.BlockPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class TravelerPathJobService implements AutoCloseable {
    private static final String PATH_PURPOSE = "path:block";
    private static final String NAVIGATE_PURPOSE = "navigate:block";

    private final PathfinderDebugState debugState;
    private final TravelerNavigationState navigationState;
    private final TravelerPathSearchService searchService;
    private final PathJobExecutor executor;
    private final List<PendingPathJob> pendingJobs = new ArrayList<>();

    TravelerPathJobService(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            TravelerPathSearchService searchService) {
        this(debugState, navigationState, searchService, new PathJobExecutor());
    }

    TravelerPathJobService(
            PathfinderDebugState debugState,
            TravelerNavigationState navigationState,
            TravelerPathSearchService searchService,
            PathJobExecutor executor) {
        this.debugState = Objects.requireNonNull(debugState, "debugState");
        this.navigationState = Objects.requireNonNull(navigationState, "navigationState");
        this.searchService = Objects.requireNonNull(searchService, "searchService");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    TravelerCommandResult queuePathBlock(TravelerCommandContext context, BlockPosition target) {
        PendingPathJob job = submit(context, target, PATH_PURPOSE, this::completePath);
        return TravelerCommandResult.success("path queued id=" + job.id() + " target=" + format(target));
    }

    TravelerCommandResult queueNavigateBlock(TravelerCommandContext context, BlockPosition target) {
        PendingPathJob job = submit(context, target, NAVIGATE_PURPOSE, this::completeNavigation);
        return TravelerCommandResult.success("navigate queued id=" + job.id() + " target=" + format(target));
    }

    void drainCompleted() {
        completedJobs().forEach(PendingPathJob::complete);
    }

    @Override
    public void close() {
        executor.close();
    }

    private synchronized PendingPathJob submit(
            TravelerCommandContext context,
            BlockPosition target,
            String purpose,
            PathCompletion completion) {
        cancelActiveJob(purpose);
        PathJob<TravelerPathSearchResult> job = searchService.blockPathJob(context, target, purpose);
        PathJobHandle<TravelerPathSearchResult> handle = executor.submit(job);
        PendingPathJob pending = new PendingPathJob(handle, context.feedback(), completion);
        pendingJobs.add(pending);
        return pending;
    }

    private synchronized List<PendingPathJob> completedJobs() {
        List<PendingPathJob> completed = pendingJobs.stream()
                .filter(PendingPathJob::isDone)
                .toList();
        pendingJobs.removeAll(completed);
        return completed;
    }

    private void completePath(TravelerPathSearchResult result, TravelerCommandFeedback feedback) {
        result.updateDebug(debugState);
        feedback.reply(result.message());
    }

    private void completeNavigation(TravelerPathSearchResult result, TravelerCommandFeedback feedback) {
        result.updateDebug(debugState);
        Optional<NavigationPath> path = result.navigationPath();
        if (path.isEmpty()) {
            navigationFailure(result, feedback);
            return;
        }
        String message = result.message().replaceFirst("^path", "navigate") + " | " + pathSummary();
        navigationState.start(path.orElseThrow(), message);
        feedback.reply(message);
    }

    private void navigationFailure(TravelerPathSearchResult result, TravelerCommandFeedback feedback) {
        String message = "navigation not started status=" + result.status();
        navigationState.stop(message);
        feedback.reply(message);
    }

    private String pathSummary() {
        return debugState.latestSnapshot()
                .map(DebugTextFormatter::pathSummary)
                .orElse("path=none");
    }

    private void cancelActiveJob(String purpose) {
        activeHandle(purpose).ifPresent(PathJobHandle::cancel);
    }

    private Optional<PathJobHandle<TravelerPathSearchResult>> activeHandle(String purpose) {
        return pendingJobs.stream()
                .map(PendingPathJob::handle)
                .filter(handle -> handle.purpose().equals(purpose))
                .filter(handle -> !handle.isDone())
                .findFirst();
    }

    private static String format(BlockPosition position) {
        return position.x() + "," + position.y() + "," + position.z();
    }

    @FunctionalInterface
    private interface PathCompletion {
        void apply(TravelerPathSearchResult result, TravelerCommandFeedback feedback);
    }

    private record PendingPathJob(
            PathJobHandle<TravelerPathSearchResult> handle,
            TravelerCommandFeedback feedback,
            PathCompletion completion) {
        private PendingPathJob {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(feedback, "feedback");
            Objects.requireNonNull(completion, "completion");
        }

        long id() {
            return handle.id();
        }

        boolean isDone() {
            return handle.isDone();
        }

        void complete() {
            if (handle.state() == PathJobState.CANCELLED) {
                feedback.reply(handle.purpose() + " cancelled id=" + handle.id());
                return;
            }
            handle.result().ifPresentOrElse(this::completeResult, this::completeWithoutResult);
        }

        private void completeResult(dev.traveler.core.job.PathJobResult<TravelerPathSearchResult> result) {
            if (result.state() == PathJobState.FAILED) {
                feedback.reply(handle.purpose() + " failed id=" + handle.id());
                return;
            }
            completion.apply(result.value(), feedback);
        }

        private void completeWithoutResult() {
            feedback.reply(handle.purpose() + " completed without result id=" + handle.id());
        }
    }
}

package dev.traveler.core.command;

import dev.traveler.core.debug.DebugTextFormatter;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.job.PathJobExecutor;
import dev.traveler.core.job.PathJobHandle;
import dev.traveler.core.job.PathJobState;
import dev.traveler.core.navigation.NavigationGoalPlan;
import dev.traveler.core.navigation.NavigationReplanRequest;
import dev.traveler.core.navigation.TravelerNavigationState;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class TravelerPathJobService implements AutoCloseable {
    private static final String PATH_PURPOSE = "path:block";
    private static final String NAVIGATE_PURPOSE = "navigate:block";
    private static final int SNAPSHOT_CAPTURE_BLOCK_BUDGET = 2_048;
    private static final long SNAPSHOT_CAPTURE_NANOS = 1_500_000L;

    private final PathfinderDebugState debugState;
    private final TravelerNavigationState navigationState;
    private final TravelerPathSearchService searchService;
    private final PathJobExecutor executor;
    private final List<PendingPathJob> pendingJobs = new ArrayList<>();
    private final List<PendingSnapshotJob> pendingSnapshots = new ArrayList<>();
    private TravelerCommandSource lastNavigationSource;
    private long nextSnapshotId = 1L;

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

    TravelerCommandResponse queuePathBlock(TravelerCommandSource source, BlockPosition target) {
        return queuePathGoal(source, RouteGoal.blockTarget(target));
    }

    TravelerCommandResponse queuePathGoal(TravelerCommandSource source, RouteGoal goal) {
        QueueOutcome outcome = submit(source, goal, PATH_PURPOSE, this::completePath);
        if (outcome.immediateResult().isPresent()) {
            TravelerPathSearchResult result = outcome.immediateResult().orElseThrow();
            result.updateDebug(debugState);
            return TravelerCommandResponse.reply(source, result.message());
        }
        return TravelerCommandResponse.reply(
                source, "path queued id=" + outcome.queuedId().orElseThrow() + " goal=" + goal.displayName());
    }

    TravelerCommandResponse queueNavigateBlock(TravelerCommandSource source, BlockPosition target) {
        return queueNavigateGoal(source, RouteGoal.blockTarget(target));
    }

    TravelerCommandResponse queueNavigateGoal(TravelerCommandSource source, RouteGoal goal) {
        lastNavigationSource = Objects.requireNonNull(source, "source");
        QueueOutcome outcome = submit(source, goal, NAVIGATE_PURPOSE, this::completeNavigation);
        if (outcome.immediateResult().isPresent()) {
            TravelerPathSearchResult result = outcome.immediateResult().orElseThrow();
            result.updateDebug(debugState);
            String message = navigationFailureMessage(result);
            navigationState.stop(message);
            return TravelerCommandResponse.reply(source, message);
        }
        return TravelerCommandResponse.reply(
                source, "navigate queued id=" + outcome.queuedId().orElseThrow() + " goal=" + goal.displayName());
    }

    void drainCompleted() {
        queueNavigationReplanIfRequested();
        advanceSnapshotCaptures();
        completedJobs().forEach(PendingPathJob::complete);
    }

    @Override
    public void close() {
        executor.close();
    }

    private synchronized QueueOutcome submit(
            TravelerCommandSource source,
            RouteGoal goal,
            String purpose,
            PathCompletion completion) {
        return submit(source, goal, purpose, completion, Optional.empty());
    }

    private synchronized QueueOutcome submit(
            TravelerCommandSource source,
            RouteGoal goal,
            String purpose,
            PathCompletion completion,
            Optional<NavigationPoint> startOverride) {
        cancelActiveJob(purpose);
        TravelerPathSearchSubmission submission = searchService.goalPathSubmission(
                source,
                goal,
                purpose,
                startOverride);
        if (submission.immediateResult().isPresent()) {
            return QueueOutcome.immediate(submission.immediateResult().orElseThrow());
        }
        if (submission.snapshotSearch().isPresent()) {
            return queueSnapshotSearch(source, purpose, completion, submission.snapshotSearch().orElseThrow());
        }
        PathJobHandle<TravelerPathSearchResult> handle =
                executor.submit(submission.job().orElseThrow());
        PendingPathJob pending = new PendingPathJob(handle, source::reply, completion);
        pendingJobs.add(pending);
        return QueueOutcome.queued(pending.id());
    }

    private void queueNavigationReplanIfRequested() {
        Optional<NavigationReplanRequest> request = navigationState.pendingReplanRequest();
        if (request.isEmpty() || lastNavigationSource == null || hasActiveNavigationSearch()) {
            return;
        }
        NavigationReplanRequest replan = navigationState.consumeReplanRequest().orElseThrow();
        PathCompletion completion = completionFor(replan);
        QueueOutcome outcome = submit(
                lastNavigationSource,
                replan.goal(),
                NAVIGATE_PURPOSE,
                completion,
                replan.startOverride());
        if (outcome.immediateResult().isPresent()) {
            completion.apply(outcome.immediateResult().orElseThrow(), lastNavigationSource::reply);
            return;
        }
        lastNavigationSource.reply("navigate replan queued id="
                + outcome.queuedId().orElseThrow()
                + " goal="
                + replan.goal().displayName()
                + " reason="
                + replan.reason());
    }

    private PathCompletion completionFor(NavigationReplanRequest replan) {
        return switch (replan.activation()) {
            case START_NEW_SESSION -> this::completeNavigation;
            case PREPARE_LOOKAHEAD -> (result, feedback) ->
                    completeNavigationLookahead(result, feedback, replan.goalPlanOverride());
            case REPLACE_ACTIVE_SESSION -> (result, feedback) ->
                    completeNavigationRepair(result, feedback, replan.goalPlanOverride());
        };
    }

    private synchronized boolean hasActiveNavigationSearch() {
        return activeHandle(NAVIGATE_PURPOSE).isPresent()
                || pendingSnapshots.stream().anyMatch(snapshot -> snapshot.purpose().equals(NAVIGATE_PURPOSE));
    }

    private synchronized List<PendingPathJob> completedJobs() {
        List<PendingPathJob> completed = pendingJobs.stream()
                .filter(PendingPathJob::isDone)
                .toList();
        pendingJobs.removeAll(completed);
        return completed;
    }

    private synchronized QueueOutcome queueSnapshotSearch(
            TravelerCommandSource source,
            String purpose,
            PathCompletion completion,
            TravelerPathSearchService.SnapshotBlockSearch snapshotSearch) {
        PendingSnapshotJob pending =
                new PendingSnapshotJob(nextSnapshotId++, purpose, snapshotSearch, source::reply, completion);
        pendingSnapshots.add(pending);
        return QueueOutcome.queued(pending.id());
    }

    private synchronized void advanceSnapshotCaptures() {
        for (PendingSnapshotJob pending : completedSnapshotCaptures()) {
            pendingJobs.add(submitSnapshotSearch(pending));
        }
    }

    private List<PendingSnapshotJob> completedSnapshotCaptures() {
        List<PendingSnapshotJob> completed = new ArrayList<>();
        Iterator<PendingSnapshotJob> iterator = pendingSnapshots.iterator();
        while (iterator.hasNext()) {
            PendingSnapshotJob pending = iterator.next();
            if (!captureSnapshotBudget(pending)) {
                continue;
            }
            iterator.remove();
            completed.add(pending);
        }
        return completed;
    }

    private boolean captureSnapshotBudget(PendingSnapshotJob pending) {
        long deadlineNanos = System.nanoTime() + SNAPSHOT_CAPTURE_NANOS;
        return pending.captureNext(SNAPSHOT_CAPTURE_BLOCK_BUDGET, deadlineNanos);
    }

    private PendingPathJob submitSnapshotSearch(PendingSnapshotJob pending) {
        pending.feedback().reply(pending.readyMessage());
        PathJobHandle<TravelerPathSearchResult> handle = executor.submit(pending.pathJob());
        return new PendingPathJob(handle, pending.feedback(), pending.completion());
    }

    private void completePath(TravelerPathSearchResult result, CommandFeedback feedback) {
        result.updateDebug(debugState);
        feedback.reply(result.message());
    }

    private void completeNavigation(TravelerPathSearchResult result, CommandFeedback feedback) {
        result.updateDebug(debugState);
        Optional<NavigationPath> path = result.navigationPath();
        if (path.isEmpty()) {
            completeEmptyNavigation(result, feedback);
            return;
        }
        String message = result.message().replaceFirst("^path", "navigate") + " | " + pathSummary();
        Optional<NavigationGoalPlan> goalPlan = result.navigationGoalPlan();
        if (goalPlan.isPresent()) {
            navigationState.start(path.orElseThrow(), message, goalPlan.orElseThrow());
        } else {
            navigationState.start(path.orElseThrow(), message);
        }
        feedback.reply(message);
    }

    private void completeNavigationLookahead(TravelerPathSearchResult result, CommandFeedback feedback) {
        completeNavigationLookahead(result, feedback, Optional.empty());
    }

    private void completeNavigationLookahead(
            TravelerPathSearchResult result,
            CommandFeedback feedback,
            Optional<NavigationGoalPlan> goalPlanOverride) {
        if (navigationState.activeSession().isEmpty()) {
            result.updateDebug(debugState);
            feedback.reply(navigationFailureMessage(result) + " | stale lookahead ignored");
            return;
        }
        Optional<NavigationPath> path = result.navigationPath();
        if (path.isPresent()) {
            prepareNavigationLookahead(result, feedback, path.orElseThrow(), goalPlanOverride);
            return;
        }
        result.updateDebug(debugState);
        feedback.reply(navigationFailureMessage(result) + " | keeping current segment");
    }

    private void prepareNavigationLookahead(
            TravelerPathSearchResult result,
            CommandFeedback feedback,
            NavigationPath path,
            Optional<NavigationGoalPlan> goalPlanOverride) {
        result.updateDebug(debugState);
        Optional<NavigationGoalPlan> goalPlan = Objects.requireNonNull(goalPlanOverride, "goalPlanOverride")
                .or(result::navigationGoalPlan);
        if (goalPlan.isEmpty()) {
            feedback.reply(navigationFailureMessage(result) + " | missing goal plan; keeping current segment");
            return;
        }
        String message = result.message().replaceFirst("^path", "navigate") + " | lookahead ready | " + pathSummary();
        navigationState.prepareLookahead(path, message, goalPlan.orElseThrow());
        feedback.reply(message);
    }

    private void completeNavigationRepair(
            TravelerPathSearchResult result,
            CommandFeedback feedback,
            Optional<NavigationGoalPlan> goalPlanOverride) {
        if (navigationState.activeSession().isEmpty()) {
            result.updateDebug(debugState);
            feedback.reply(navigationFailureMessage(result) + " | stale repair ignored");
            return;
        }
        Optional<NavigationPath> path = result.navigationPath();
        if (path.isEmpty()) {
            result.updateDebug(debugState);
            feedback.reply(navigationFailureMessage(result) + " | keeping current segment");
            return;
        }
        replaceNavigationSegment(result, feedback, path.orElseThrow(), goalPlanOverride);
    }

    private void replaceNavigationSegment(
            TravelerPathSearchResult result,
            CommandFeedback feedback,
            NavigationPath path,
            Optional<NavigationGoalPlan> goalPlanOverride) {
        result.updateDebug(debugState);
        Optional<NavigationGoalPlan> goalPlan = Objects.requireNonNull(goalPlanOverride, "goalPlanOverride")
                .or(result::navigationGoalPlan);
        if (goalPlan.isEmpty()) {
            feedback.reply(navigationFailureMessage(result) + " | missing goal plan; keeping current segment");
            return;
        }
        String message = result.message().replaceFirst("^path", "navigate") + " | repair ready | " + pathSummary();
        navigationState.replaceActiveSession(path, message, goalPlan.orElseThrow());
        feedback.reply(message);
    }

    private void completeEmptyNavigation(TravelerPathSearchResult result, CommandFeedback feedback) {
        if (result.alreadyAtTarget()) {
            navigationAlreadyAtTarget(result, feedback);
            return;
        }
        navigationFailure(result, feedback);
    }

    private void navigationAlreadyAtTarget(TravelerPathSearchResult result, CommandFeedback feedback) {
        String message = "navigation already at target | " + result.message();
        navigationState.stop(message);
        feedback.reply(message);
    }

    private void navigationFailure(TravelerPathSearchResult result, CommandFeedback feedback) {
        String message = navigationFailureMessage(result);
        navigationState.stop(message);
        feedback.reply(message);
    }

    private static String navigationFailureMessage(TravelerPathSearchResult result) {
        return "navigation not started status=" + result.status() + " | " + result.message();
    }

    private String pathSummary() {
        return debugState.latestSnapshot()
                .map(DebugTextFormatter::pathSummary)
                .orElse("path=none");
    }

    private void cancelActiveJob(String purpose) {
        activeHandle(purpose).ifPresent(PathJobHandle::cancel);
        cancelActiveSnapshots(purpose);
    }

    private Optional<PathJobHandle<TravelerPathSearchResult>> activeHandle(String purpose) {
        return pendingJobs.stream()
                .map(PendingPathJob::handle)
                .filter(handle -> handle.purpose().equals(purpose))
                .filter(handle -> !handle.isDone())
                .findFirst();
    }

    private void cancelActiveSnapshots(String purpose) {
        List<PendingSnapshotJob> cancelled = pendingSnapshots.stream()
                .filter(snapshot -> snapshot.purpose().equals(purpose))
                .toList();
        cancelled.forEach(PendingSnapshotJob::cancel);
        pendingSnapshots.removeAll(cancelled);
    }

    @FunctionalInterface
    private interface PathCompletion {
        void apply(TravelerPathSearchResult result, CommandFeedback feedback);
    }

    @FunctionalInterface
    private interface CommandFeedback {
        void reply(String message);
    }

    private record PendingPathJob(
            PathJobHandle<TravelerPathSearchResult> handle,
            CommandFeedback feedback,
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
                feedback.reply(handle.purpose()
                        + " failed id="
                        + handle.id()
                        + " cause="
                        + failureSummary(result));
                return;
            }
            completion.apply(result.value(), feedback);
        }

        private static String failureSummary(
                dev.traveler.core.job.PathJobResult<TravelerPathSearchResult> result) {
            return result.failureCause()
                    .map(PendingPathJob::failureSummary)
                    .orElse("unknown");
        }

        private static String failureSummary(Throwable failure) {
            String message = failure.getMessage();
            if (message == null || message.isBlank()) {
                return failure.getClass().getSimpleName();
            }
            return failure.getClass().getSimpleName() + ": " + message;
        }

        private void completeWithoutResult() {
            feedback.reply(handle.purpose() + " completed without result id=" + handle.id());
        }
    }

    private record QueueOutcome(
            Optional<Long> queuedId,
            Optional<TravelerPathSearchResult> immediateResult) {
        private QueueOutcome {
            queuedId = Objects.requireNonNull(queuedId, "queuedId");
            immediateResult = Objects.requireNonNull(immediateResult, "immediateResult");
            if (queuedId.isPresent() == immediateResult.isPresent()) {
                throw new IllegalArgumentException("Queue outcome must contain exactly one value.");
            }
        }

        private static QueueOutcome queued(long queuedId) {
            return new QueueOutcome(Optional.of(queuedId), Optional.empty());
        }

        private static QueueOutcome immediate(TravelerPathSearchResult result) {
            return new QueueOutcome(Optional.empty(), Optional.of(Objects.requireNonNull(result, "result")));
        }
    }

    private record PendingSnapshotJob(
            long id,
            String purpose,
            TravelerPathSearchService.SnapshotBlockSearch snapshotSearch,
            CommandFeedback feedback,
            PathCompletion completion) {
        private PendingSnapshotJob {
            Objects.requireNonNull(purpose, "purpose");
            Objects.requireNonNull(snapshotSearch, "snapshotSearch");
            Objects.requireNonNull(feedback, "feedback");
            Objects.requireNonNull(completion, "completion");
        }

        private boolean captureNext(int blockBudget, long deadlineNanos) {
            return snapshotSearch.captureNext(blockBudget, deadlineNanos);
        }

        private dev.traveler.core.job.PathJob<TravelerPathSearchResult> pathJob() {
            return snapshotSearch.pathJob();
        }

        private String readyMessage() {
            return purpose
                    + " snapshot ready id="
                    + id
                    + " blocks="
                    + snapshotSearch.capturedBlocks()
                    + "/"
                    + snapshotSearch.blockCount()
                    + " | worker queued";
        }

        private void cancel() {
            feedback.reply(purpose + " cancelled id=" + id);
        }
    }
}

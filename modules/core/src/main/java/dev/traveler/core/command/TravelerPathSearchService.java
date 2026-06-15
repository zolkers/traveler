package dev.traveler.core.command;

import dev.traveler.core.job.PathJob;
import dev.traveler.core.job.PathJobState;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SnapshotCaptureSession;
import dev.traveler.core.layer.SnapshotCapturableWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.route.RouteSearchDiagnostics;
import dev.traveler.core.route.RouteSearchResult;
import dev.traveler.core.route.RouteSearchService;
import dev.traveler.core.route.RouteSearchSettings;
import dev.traveler.core.route.longdistance.LongDistanceRoutePlan;
import dev.traveler.core.route.longdistance.LongDistanceRoutePlanner;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.FluidHandling;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

final class TravelerPathSearchService {
    private static final BlockPosition TEST_START = new BlockPosition(0, 64, 0);
    private static final BlockPosition TEST_GOAL = new BlockPosition(3, 64, 0);
    private static final long MAX_SNAPSHOT_BLOCKS = 262_144L;
    private static final RouteSearchSettings SEARCH_SETTINGS = RouteSearchSettings.standardClient();
    private static final RouteSearchService ROUTE_SEARCH_SERVICE = new RouteSearchService(SEARCH_SETTINGS);
    private static final LongDistanceRoutePlanner LONG_DISTANCE_PLANNER = LongDistanceRoutePlanner.standard();

    private final Supplier<? extends WorldLayer> worldLayerSupplier;

    TravelerPathSearchService(Supplier<? extends WorldLayer> worldLayerSupplier) {
        this.worldLayerSupplier = Objects.requireNonNull(worldLayerSupplier, "worldLayerSupplier");
    }

    TravelerPathSearchResult testPath() {
        RouteSearchResult result = ROUTE_SEARCH_SERVICE.search(null, TEST_START, TEST_GOAL);
        String message = "path test status=" + result.status() + " nodes=" + result.blockResult().path().nodeCount();
        return new TravelerPathSearchResult(result, message);
    }

    static RouteSearchSettings searchSettings() {
        return SEARCH_SETTINGS;
    }

    TravelerPathSearchSubmission blockPathSubmission(
            TravelerCommandSource source,
            BlockPosition target,
            String purpose) {
        return goalPathSubmission(source, RouteGoal.blockTarget(target), purpose);
    }

    TravelerPathSearchSubmission goalPathSubmission(
            TravelerCommandSource source,
            RouteGoal goal,
            String purpose) {
        RouteGoal safeGoal = Objects.requireNonNull(goal, "goal");
        BlockPosition start = startPosition(source, safeGoal);
        WorldLayer worldLayer = worldLayerSupplier.get();
        LongDistanceRoutePlan plan = LONG_DISTANCE_PLANNER.plan(start, safeGoal);
        RouteGoal activeGoal = plan.activeGoal();
        BlockPosition activeBlockGoal = planningBlockGoal(worldLayer, activeGoal, start);
        Optional<TravelerPathSearchResult> rejection = oversizedSnapshot(worldLayer, start, activeBlockGoal);
        if (rejection.isPresent()) {
            return TravelerPathSearchSubmission.immediate(rejection.orElseThrow());
        }
        if (worldLayer instanceof SnapshotCapturableWorldLayer snapshotWorldLayer) {
            return TravelerPathSearchSubmission.snapshot(
                    new SnapshotBlockSearch(snapshotWorldLayer, start, activeGoal, activeBlockGoal, plan, purpose));
        }
        return TravelerPathSearchSubmission.queued(
                pathJob(worldLayer, start, activeGoal, activeBlockGoal, plan, purpose));
    }

    private static BlockPosition planningBlockGoal(WorldLayer worldLayer, RouteGoal goal, BlockPosition start) {
        if (worldLayer instanceof SnapshotCapturableWorldLayer) {
            return goal.blockGoal(null, start);
        }
        return goal.blockGoal(worldLayer, start);
    }

    private static Optional<TravelerPathSearchResult> oversizedSnapshot(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target) {
        if (!(worldLayer instanceof SnapshotCapturableWorldLayer)) {
            return Optional.empty();
        }
        SearchVolume volume = SearchVolume.around(start, target);
        if (volume.blockCount() <= MAX_SNAPSHOT_BLOCKS) {
            return Optional.empty();
        }
        return Optional.of(rejectedSearch(target, volume));
    }

    private static TravelerPathSearchResult rejectedSearch(BlockPosition target, SearchVolume volume) {
        RouteSearchResult searchResult = RouteSearchResult.notFound(RouteSearchDiagnostics.blockNotFound());
        String message = "path block "
                + format(target)
                + " status=NOT_FOUND reason=search-too-large estimatedBlocks="
                + volume.blockCount()
                + " limit="
                + MAX_SNAPSHOT_BLOCKS;
        return new TravelerPathSearchResult(searchResult, message);
    }

    private static PathJob<TravelerPathSearchResult> pathJob(
            WorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            BlockPosition activeBlockGoal,
            LongDistanceRoutePlan plan,
            String purpose) {
        return new PathJob<>(
                purpose,
                () -> searchGoalPath(worldLayer, start, goal, activeBlockGoal, plan),
                TravelerPathSearchService::jobState);
    }

    private static TravelerPathSearchResult searchGoalPath(
            WorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            BlockPosition activeBlockGoal,
            LongDistanceRoutePlan plan) {
        RouteSearchResult result = ROUTE_SEARCH_SERVICE.search(worldLayer, start, goal);
        return new TravelerPathSearchResult(
                result,
                blockMessage(worldLayer, goal, activeBlockGoal, plan, result),
                plan);
    }

    private static BlockPosition startPosition(TravelerCommandSource source, RouteGoal goal) {
        BlockPosition sourcePosition = Objects.requireNonNull(source, "source").blockPosition();
        if (sourcePosition == null) {
            return goal.requestedTarget()
                    .map(BlockPosition::above)
                    .orElseGet(() -> goal.preferredPosition(new BlockPosition(0, 64, 0)));
        }
        return sourcePosition;
    }

    private static String blockMessage(
            WorldLayer worldLayer,
            RouteGoal goal,
            BlockPosition activeBlockGoal,
            LongDistanceRoutePlan plan,
            RouteSearchResult result) {
        String longDistance = plan.finalSegment()
                ? ""
                : " longDistance=segment finalGoal=" + plan.requestedGoal().displayName();
        if (worldLayer == null) {
            return "path "
                    + goalLabel(goal, activeBlockGoal)
                    + " status="
                    + result.status()
                    + " reason="
                    + result.diagnostics().reason()
                    + longDistance;
        }
        BlockPosition reportedGoal = reportedGoal(activeBlockGoal, result);
        BlockClassification classification = worldLayer.classify(reportedGoal);
        return "path "
                + goalLabel(goal, reportedGoal)
                + " status="
                + result.status()
                + " passability="
                + classification.passability()
                + " fluid="
                + (classification.fluidHandling() == FluidHandling.ALLOW)
                + " reason="
                + result.diagnostics().reason()
                + " routeSteps="
                + result.route().map(route -> route.steps().size()).orElse(0)
                + longDistance;
    }

    private static BlockPosition reportedGoal(BlockPosition fallback, RouteSearchResult result) {
        List<BlockPosition> nodes = result.blockResult().path().nodes();
        if (nodes.isEmpty()) {
            return fallback;
        }
        return nodes.getLast();
    }

    private static String goalLabel(RouteGoal goal, BlockPosition activeBlockGoal) {
        return goal.requestedTarget()
                .map(target -> "block " + format(target))
                .orElse(goal.displayName() + " active=" + format(activeBlockGoal));
    }

    private static String format(BlockPosition position) {
        return position.x() + "," + position.y() + "," + position.z();
    }

    private static PathJobState jobState(TravelerPathSearchResult result) {
        if (result.status() == PathfinderStatus.FOUND) {
            return PathJobState.FOUND;
        }
        return PathJobState.NOT_FOUND;
    }

    private record SearchVolume(long width, long height, long depth) {
        private static SearchVolume around(BlockPosition start, BlockPosition target) {
            long width = span(start.x(), target.x(), SEARCH_SETTINGS.horizontalMargin());
            long height = span(start.y(), target.y(), SEARCH_SETTINGS.verticalMargin());
            long depth = span(start.z(), target.z(), SEARCH_SETTINGS.horizontalMargin());
            return new SearchVolume(width, height, depth);
        }

        private long blockCount() {
            return Math.multiplyExact(Math.multiplyExact(width, height), depth);
        }

        private static long span(int first, int second, int margin) {
            return Math.abs((long) first - second) + margin * 2L + 1L;
        }
    }

    static final class SnapshotBlockSearch {
        private final SnapshotCaptureSession captureSession;
        private final BlockPosition start;
        private final RouteGoal goal;
        private final BlockPosition target;
        private final LongDistanceRoutePlan plan;
        private final String purpose;

        private SnapshotBlockSearch(
                SnapshotCapturableWorldLayer worldLayer,
                BlockPosition start,
                RouteGoal goal,
                BlockPosition target,
                LongDistanceRoutePlan plan,
                String purpose) {
            this.captureSession = worldLayer.captureSession(
                    start, target, SEARCH_SETTINGS.horizontalMargin(), SEARCH_SETTINGS.verticalMargin());
            this.start = Objects.requireNonNull(start, "start");
            this.goal = Objects.requireNonNull(goal, "goal");
            this.target = Objects.requireNonNull(target, "target");
            this.plan = Objects.requireNonNull(plan, "plan");
            this.purpose = Objects.requireNonNull(purpose, "purpose");
        }

        boolean captureNext(int blockBudget, long deadlineNanos) {
            return captureSession.captureNext(blockBudget, deadlineNanos);
        }

        long blockCount() {
            return captureSession.blockCount();
        }

        long capturedBlocks() {
            return captureSession.capturedBlocks();
        }

        PathJob<TravelerPathSearchResult> pathJob() {
            return new PathJob<>(
                    purpose,
                    () -> searchGoalPath(captureSession.snapshot(), start, goal, target, plan),
                    TravelerPathSearchService::jobState);
        }
    }
}

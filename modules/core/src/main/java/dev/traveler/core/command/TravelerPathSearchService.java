package dev.traveler.core.command;

import dev.traveler.core.job.PathJob;
import dev.traveler.core.job.PathJobState;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.NavigationBudgetProvider;
import dev.traveler.core.layer.SnapshotCaptureSession;
import dev.traveler.core.layer.SnapshotCapturableWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.common.geometry.WorldPoint;
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
        return goalPathSubmission(source, goal, purpose, Optional.empty());
    }

    TravelerPathSearchSubmission goalPathSubmission(
            TravelerCommandSource source,
            RouteGoal goal,
            String purpose,
            Optional<WorldPoint> startOverride) {
        RouteGoal safeGoal = Objects.requireNonNull(goal, "goal");
        BlockPosition start = startPosition(source, safeGoal, startOverride);
        WorldLayer worldLayer = worldLayerSupplier.get();
        LongDistanceRoutePlan plan = longDistancePlan(start, safeGoal, worldLayer);
        RouteGoal activeGoal = plan.activeGoal();
        BlockPosition activeBlockGoal = planningBlockGoal(worldLayer, activeGoal, start);
        SnapshotMargins margins = snapshotMargins(plan);
        Optional<TravelerPathSearchResult> rejection =
                oversizedSnapshot(worldLayer, start, activeBlockGoal, margins);
        if (rejection.isPresent()) {
            return TravelerPathSearchSubmission.immediate(rejection.orElseThrow());
        }
        if (worldLayer instanceof SnapshotCapturableWorldLayer snapshotWorldLayer) {
            return TravelerPathSearchSubmission.snapshot(
                    new SnapshotBlockSearch(
                            snapshotWorldLayer,
                            start,
                            activeGoal,
                            activeBlockGoal,
                            margins,
                            plan,
                            purpose,
                            startOverride));
        }
        return TravelerPathSearchSubmission.queued(
                pathJob(worldLayer, start, activeGoal, activeBlockGoal, plan, purpose, startOverride));
    }

    private static LongDistanceRoutePlan longDistancePlan(
            BlockPosition start,
            RouteGoal goal,
            WorldLayer worldLayer) {
        if (worldLayer instanceof NavigationBudgetProvider provider) {
            return LONG_DISTANCE_PLANNER.plan(start, goal, provider.navigationBudget());
        }
        return LONG_DISTANCE_PLANNER.plan(start, goal);
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
            BlockPosition target,
            SnapshotMargins margins) {
        if (!(worldLayer instanceof SnapshotCapturableWorldLayer)) {
            return Optional.empty();
        }
        SearchVolume volume = SearchVolume.around(start, target, margins);
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
            String purpose,
            Optional<WorldPoint> navigationStartOverride) {
        return new PathJob<>(
                purpose,
                () -> searchGoalPath(
                        ROUTE_SEARCH_SERVICE,
                        worldLayer,
                        start,
                        goal,
                        activeBlockGoal,
                        plan,
                        navigationStartOverride),
                TravelerPathSearchService::jobState);
    }

    private static TravelerPathSearchResult searchGoalPath(
            RouteSearchService routeSearchService,
            WorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            BlockPosition activeBlockGoal,
            LongDistanceRoutePlan plan,
            Optional<WorldPoint> navigationStartOverride) {
        RouteSearchResult result = routeSearchService.search(worldLayer, start, goal);
        return new TravelerPathSearchResult(
                result,
                blockMessage(worldLayer, goal, activeBlockGoal, plan, result),
                Optional.of(plan),
                navigationStartOverride);
    }

    private static BlockPosition startPosition(
            TravelerCommandSource source,
            RouteGoal goal,
            Optional<WorldPoint> startOverride) {
        Optional<WorldPoint> override = Objects.requireNonNull(startOverride, "startOverride");
        if (override.isPresent()) {
            return blockPosition(override.orElseThrow());
        }
        BlockPosition sourcePosition = Objects.requireNonNull(source, "source").blockPosition();
        if (sourcePosition == null) {
            return goal.requestedTarget()
                    .map(BlockPosition::above)
                    .orElseGet(() -> goal.preferredPosition(new BlockPosition(0, 64, 0)));
        }
        return sourcePosition;
    }

    private static BlockPosition blockPosition(WorldPoint point) {
        WorldPoint safePoint = Objects.requireNonNull(point, "point");
        return new BlockPosition(
                (int) Math.floor(safePoint.x()),
                (int) Math.floor(safePoint.y()),
                (int) Math.floor(safePoint.z()));
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

    private static SnapshotMargins snapshotMargins(LongDistanceRoutePlan plan) {
        if (plan.finalSegment()) {
            return new SnapshotMargins(SEARCH_SETTINGS.horizontalMargin(), SEARCH_SETTINGS.verticalMargin());
        }
        return new SnapshotMargins(
                plan.settings().frontierCaptureHorizontalMargin(),
                plan.settings().frontierCaptureVerticalMargin());
    }

    private record SnapshotMargins(int horizontal, int vertical) {
        private SnapshotMargins {
            if (horizontal < 0 || vertical < 0) {
                throw new IllegalArgumentException("Snapshot margins must be non-negative.");
            }
        }
    }

    private record SearchVolume(long width, long height, long depth) {
        private static SearchVolume around(BlockPosition start, BlockPosition target, SnapshotMargins margins) {
            long width = span(start.x(), target.x(), margins.horizontal());
            long height = span(start.y(), target.y(), margins.vertical());
            long depth = span(start.z(), target.z(), margins.horizontal());
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
        private final SnapshotMargins margins;
        private final LongDistanceRoutePlan plan;
        private final String purpose;
        private final Optional<WorldPoint> navigationStartOverride;

        private SnapshotBlockSearch(
                SnapshotCapturableWorldLayer worldLayer,
                BlockPosition start,
                RouteGoal goal,
                BlockPosition target,
                SnapshotMargins margins,
                LongDistanceRoutePlan plan,
                String purpose,
                Optional<WorldPoint> navigationStartOverride) {
            this.captureSession = worldLayer.captureSession(
                    start, target, margins.horizontal(), margins.vertical());
            this.start = Objects.requireNonNull(start, "start");
            this.goal = Objects.requireNonNull(goal, "goal");
            this.target = Objects.requireNonNull(target, "target");
            this.margins = Objects.requireNonNull(margins, "margins");
            this.plan = Objects.requireNonNull(plan, "plan");
            this.purpose = Objects.requireNonNull(purpose, "purpose");
            this.navigationStartOverride =
                    Objects.requireNonNull(navigationStartOverride, "navigationStartOverride");
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

        RouteSearchSettings routeSearchSettings() {
            return SEARCH_SETTINGS.withMargins(margins.horizontal(), margins.vertical());
        }

        PathJob<TravelerPathSearchResult> pathJob() {
            RouteSearchService routeSearchService = new RouteSearchService(routeSearchSettings());
            return new PathJob<>(
                    purpose,
                    () -> searchGoalPath(
                            routeSearchService,
                            captureSession.snapshot(),
                            start,
                            goal,
                            target,
                            plan,
                            navigationStartOverride),
                    TravelerPathSearchService::jobState);
        }
    }
}

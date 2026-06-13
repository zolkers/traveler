package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.command.TravelerCommandContext;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.job.PathJob;
import dev.traveler.core.job.PathJobState;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.route.RouteSearchDiagnostics;
import dev.traveler.core.route.RouteSearchResult;
import dev.traveler.core.route.RouteSearchService;
import dev.traveler.core.route.RouteSearchSettings;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.mc.v1_21_11.common.adapter.world.ImmutableMinecraftWorldSnapshot;
import dev.traveler.mc.v1_21_11.common.adapter.world.MinecraftWorldSnapshot;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

final class TravelerPathSearchService {
    private static final BlockPosition TEST_START = new BlockPosition(0, 64, 0);
    private static final BlockPosition TEST_GOAL = new BlockPosition(3, 64, 0);
    private static final long MAX_MINECRAFT_SNAPSHOT_BLOCKS = 262_144L;
    private static final RouteSearchSettings SEARCH_SETTINGS = RouteSearchSettings.standardClient();
    private static final RouteSearchService ROUTE_SEARCH_SERVICE = new RouteSearchService(SEARCH_SETTINGS);

    private final Supplier<? extends WorldLayer> worldLayerSupplier;

    TravelerPathSearchService(Supplier<? extends WorldLayer> worldLayerSupplier) {
        this.worldLayerSupplier = Objects.requireNonNull(worldLayerSupplier, "worldLayerSupplier");
    }

    TravelerPathSearchResult testPath() {
        RouteSearchResult result = ROUTE_SEARCH_SERVICE.search(null, TEST_START, TEST_GOAL);
        String message = "path test status=" + result.status() + " nodes=" + result.blockResult().path().nodeCount();
        return new TravelerPathSearchResult(result, message);
    }

    TravelerPathSearchSubmission blockPathSubmission(
            TravelerCommandContext context,
            BlockPosition target,
            String purpose) {
        BlockPosition start = startPosition(context, target);
        WorldLayer worldLayer = worldLayerSupplier.get();
        Optional<TravelerPathSearchResult> rejection = oversizedMinecraftSnapshot(worldLayer, start, target);
        if (rejection.isPresent()) {
            return TravelerPathSearchSubmission.immediate(rejection.orElseThrow());
        }
        if (worldLayer instanceof MinecraftWorldSnapshot minecraftWorldLayer) {
            return TravelerPathSearchSubmission.snapshot(
                    new SnapshotBlockSearch(minecraftWorldLayer, start, target, purpose));
        }
        return TravelerPathSearchSubmission.queued(pathJob(worldLayer, start, target, purpose));
    }

    private static Optional<TravelerPathSearchResult> oversizedMinecraftSnapshot(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target) {
        if (!(worldLayer instanceof MinecraftWorldSnapshot)) {
            return Optional.empty();
        }
        SearchVolume volume = SearchVolume.around(start, target);
        if (volume.blockCount() <= MAX_MINECRAFT_SNAPSHOT_BLOCKS) {
            return Optional.empty();
        }
        return Optional.of(rejectedSearch(target, volume));
    }

    private static TravelerPathSearchResult rejectedSearch(BlockPosition target, SearchVolume volume) {
        PathfinderResult<BlockPosition> result =
                new PathfinderResult<>(PathfinderStatus.NOT_FOUND, new MutableGraphPath<>());
        RouteSearchResult searchResult = new RouteSearchResult(
                result,
                Optional.empty(),
                Optional.empty(),
                RouteSearchDiagnostics.blockNotFound());
        String message = "path block "
                + format(target)
                + " status=NOT_FOUND reason=search-too-large estimatedBlocks="
                + volume.blockCount()
                + " limit="
                + MAX_MINECRAFT_SNAPSHOT_BLOCKS;
        return new TravelerPathSearchResult(searchResult, message);
    }

    private static PathJob<TravelerPathSearchResult> pathJob(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target,
            String purpose) {
        return new PathJob<>(
                purpose,
                () -> searchBlockPath(worldLayer, start, target),
                TravelerPathSearchService::jobState);
    }

    private static TravelerPathSearchResult searchBlockPath(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition target) {
        RouteSearchResult result = ROUTE_SEARCH_SERVICE.search(worldLayer, start, target);
        return new TravelerPathSearchResult(result, blockMessage(worldLayer, target, result));
    }

    private static BlockPosition startPosition(TravelerCommandContext context, BlockPosition target) {
        return context.source()
                .unwrap(TravelerCommandPosition.class)
                .flatMap(TravelerCommandPosition::blockPosition)
                .map(TravelerCommandBlockPosition::toCorePosition)
                .orElse(target.above());
    }

    private static String blockMessage(WorldLayer worldLayer, BlockPosition target, RouteSearchResult result) {
        if (worldLayer == null) {
            return "path block "
                    + format(target)
                    + " status="
                    + result.status()
                    + " reason="
                    + result.diagnostics().reason();
        }
        BlockClassification classification = worldLayer.classify(target);
        return "path block "
                + format(target)
                + " status="
                + result.status()
                + " passability="
                + classification.passability()
                + " fluid="
                + (classification.fluidHandling() == FluidHandling.ALLOW)
                + " reason="
                + result.diagnostics().reason()
                + " routeSteps="
                + result.route().map(route -> route.steps().size()).orElse(0);
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
        private final ImmutableMinecraftWorldSnapshot.CaptureSession captureSession;
        private final BlockPosition start;
        private final BlockPosition target;
        private final String purpose;

        private SnapshotBlockSearch(
                MinecraftWorldSnapshot worldLayer,
                BlockPosition start,
                BlockPosition target,
                String purpose) {
            this.captureSession = ImmutableMinecraftWorldSnapshot.captureSession(
                    worldLayer,
                    start,
                    target,
                    SEARCH_SETTINGS.horizontalMargin(),
                    SEARCH_SETTINGS.verticalMargin());
            this.start = Objects.requireNonNull(start, "start");
            this.target = Objects.requireNonNull(target, "target");
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
                    () -> searchBlockPath(captureSession.snapshot(), start, target),
                    TravelerPathSearchService::jobState);
        }
    }
}

package dev.traveler.core.navigation.diagnostics;

import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.block.BlockPosition;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class MovementFailureReportService implements MovementFailureReporter {
    private final Supplier<? extends BlockScanSource> blockScanSourceSupplier;
    private final MovementFailureReportSink sink;
    private final MovementFailureReportSettings settings;

    public MovementFailureReportService(
            Supplier<? extends BlockScanSource> blockScanSourceSupplier,
            MovementFailureReportSink sink) {
        this(blockScanSourceSupplier, sink, MovementFailureReportSettings.standard());
    }

    public MovementFailureReportService(
            Supplier<? extends BlockScanSource> blockScanSourceSupplier,
            MovementFailureReportSink sink,
            MovementFailureReportSettings settings) {
        this.blockScanSourceSupplier = Objects.requireNonNull(blockScanSourceSupplier, "blockScanSourceSupplier");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public void report(MovementFailureReportContext context) {
        MovementFailureReportContext safeContext = Objects.requireNonNull(context, "context");
        sink.write(new MovementFailureReport(
                Instant.now(),
                safeContext,
                scanBlocks(safeContext.input().position())));
    }

    private List<BlockScanSample> scanBlocks(WorldPoint center) {
        BlockScanSource source = blockScanSourceSupplier.get();
        if (source == null) {
            return List.of();
        }
        BlockPosition centerBlock = blockPosition(center);
        int half = settings.scanSize() / 2;
        List<BlockScanSample> samples = new ArrayList<>(settings.scanSize()
                * settings.scanSize()
                * settings.scanSize());
        for (int x = centerBlock.x() - half; x < centerBlock.x() - half + settings.scanSize(); x++) {
            for (int y = centerBlock.y() - half; y < centerBlock.y() - half + settings.scanSize(); y++) {
                for (int z = centerBlock.z() - half; z < centerBlock.z() - half + settings.scanSize(); z++) {
                    samples.add(sample(source, new BlockPosition(x, y, z)));
                }
            }
        }
        return List.copyOf(samples);
    }

    private static BlockScanSample sample(BlockScanSource source, BlockPosition position) {
        try {
            return source.sample(position);
        } catch (RuntimeException failure) {
            return BlockScanSample.unavailable(position, failure);
        }
    }

    private static BlockPosition blockPosition(WorldPoint point) {
        return new BlockPosition(
                (int) Math.floor(point.x()),
                (int) Math.floor(point.y()),
                (int) Math.floor(point.z()));
    }
}

package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ImmutableMinecraftWorldSnapshot implements SurfaceWorldLayer {
    private final Map<BlockPosition, SurfaceBlock> blocks;

    private ImmutableMinecraftWorldSnapshot(Map<BlockPosition, SurfaceBlock> blocks) {
        this.blocks = Map.copyOf(blocks);
    }

    public static ImmutableMinecraftWorldSnapshot capture(
            MinecraftWorldSnapshot source,
            BlockPosition start,
            BlockPosition target,
            int horizontalMargin,
            int verticalMargin) {
        CaptureSession session = captureSession(source, start, target, horizontalMargin, verticalMargin);
        session.captureAll();
        return session.snapshot();
    }

    public static CaptureSession captureSession(
            MinecraftWorldSnapshot source,
            BlockPosition start,
            BlockPosition target,
            int horizontalMargin,
            int verticalMargin) {
        Objects.requireNonNull(source, "source");
        Bounds bounds = Bounds.around(start, target, horizontalMargin, verticalMargin);
        return new CaptureSession(source, bounds);
    }

    @Override
    public SurfaceBlock surfaceBlock(BlockPosition position) {
        Objects.requireNonNull(position, "position");
        return blocks.getOrDefault(position, SurfaceBlock.empty());
    }

    public static final class CaptureSession {
        private final MinecraftWorldSnapshot source;
        private final Bounds bounds;
        private final Map<BlockPosition, SurfaceBlock> blocks;
        private int currentX;
        private int currentY;
        private int currentZ;
        private long capturedBlocks;
        private boolean complete;

        private CaptureSession(MinecraftWorldSnapshot source, Bounds bounds) {
            this.source = Objects.requireNonNull(source, "source");
            this.bounds = Objects.requireNonNull(bounds, "bounds");
            blocks = new HashMap<>(mapCapacity(bounds.blockCount()));
            currentX = bounds.minX();
            currentY = bounds.minY();
            currentZ = bounds.minZ();
        }

        public boolean captureNext(int blockBudget) {
            return captureNext(blockBudget, Long.MAX_VALUE);
        }

        public boolean captureNext(int blockBudget, long deadlineNanos) {
            requirePositive(blockBudget, "blockBudget");
            int captured = 0;
            do {
                if (complete) {
                    return true;
                }
                captureCurrentBlock();
                advance();
                captured++;
            } while (captured < blockBudget && System.nanoTime() < deadlineNanos);
            return complete;
        }

        public ImmutableMinecraftWorldSnapshot snapshot() {
            if (!complete) {
                throw new IllegalStateException("Snapshot capture is not complete.");
            }
            return new ImmutableMinecraftWorldSnapshot(blocks);
        }

        public long blockCount() {
            return bounds.blockCount();
        }

        public long capturedBlocks() {
            return capturedBlocks;
        }

        private void captureAll() {
            while (!captureNext(Integer.MAX_VALUE)) {
                // Continue until the full immutable snapshot is available.
            }
        }

        private void captureCurrentBlock() {
            BlockPosition position = new BlockPosition(currentX, currentY, currentZ);
            blocks.put(position, source.surfaceBlock(position));
            capturedBlocks++;
        }

        private void advance() {
            if (currentZ < bounds.maxZ()) {
                currentZ++;
                return;
            }
            advanceRow();
        }

        private void advanceRow() {
            currentZ = bounds.minZ();
            if (currentY < bounds.maxY()) {
                currentY++;
                return;
            }
            advanceColumn();
        }

        private void advanceColumn() {
            currentY = bounds.minY();
            if (currentX < bounds.maxX()) {
                currentX++;
                return;
            }
            complete = true;
        }
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static int mapCapacity(long expectedSize) {
        long capacity = expectedSize + expectedSize / 3L + 1L;
        long safeCapacity = Math.min(capacity, Integer.MAX_VALUE - 8L);
        return Math.toIntExact(safeCapacity);
    }

    private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        static Bounds around(BlockPosition start, BlockPosition target, int horizontalMargin, int verticalMargin) {
            BlockPosition safeStart = Objects.requireNonNull(start, "start");
            BlockPosition safeTarget = Objects.requireNonNull(target, "target");
            int safeHorizontalMargin = requirePositive(horizontalMargin, "horizontalMargin");
            int safeVerticalMargin = requirePositive(verticalMargin, "verticalMargin");
            return new Bounds(
                    Math.min(safeStart.x(), safeTarget.x()) - safeHorizontalMargin,
                    Math.max(safeStart.x(), safeTarget.x()) + safeHorizontalMargin,
                    Math.min(safeStart.y(), safeTarget.y()) - safeVerticalMargin,
                    Math.max(safeStart.y(), safeTarget.y()) + safeVerticalMargin,
                    Math.min(safeStart.z(), safeTarget.z()) - safeHorizontalMargin,
                    Math.max(safeStart.z(), safeTarget.z()) + safeHorizontalMargin);
        }

        private long blockCount() {
            long width = maxX - minX + 1L;
            long height = maxY - minY + 1L;
            long depth = maxZ - minZ + 1L;
            return Math.multiplyExact(Math.multiplyExact(width, height), depth);
        }
    }
}

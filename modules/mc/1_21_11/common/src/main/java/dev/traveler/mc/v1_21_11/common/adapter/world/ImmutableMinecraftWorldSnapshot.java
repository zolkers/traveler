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
        Objects.requireNonNull(source, "source");
        Bounds bounds = Bounds.around(start, target, horizontalMargin, verticalMargin);
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            captureColumn(source, blocks, bounds, x);
        }
        return new ImmutableMinecraftWorldSnapshot(blocks);
    }

    @Override
    public SurfaceBlock surfaceBlock(BlockPosition position) {
        Objects.requireNonNull(position, "position");
        return blocks.getOrDefault(position, SurfaceBlock.empty());
    }

    private static void captureColumn(
            MinecraftWorldSnapshot source, Map<BlockPosition, SurfaceBlock> blocks, Bounds bounds, int x) {
        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            captureRow(source, blocks, bounds, x, y);
        }
    }

    private static void captureRow(
            MinecraftWorldSnapshot source, Map<BlockPosition, SurfaceBlock> blocks, Bounds bounds, int x, int y) {
        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
            captureBlock(source, blocks, new BlockPosition(x, y, z));
        }
    }

    private static void captureBlock(
            MinecraftWorldSnapshot source, Map<BlockPosition, SurfaceBlock> blocks, BlockPosition position) {
        blocks.put(position, source.surfaceBlock(position));
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

        private static int requirePositive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }
    }
}

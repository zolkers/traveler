package dev.traveler.core.world.navigation;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.smooth.LineOfWalk;
import java.util.Objects;

public final class BlockLineOfWalk implements LineOfWalk<BlockPosition> {
    private static final int MAX_VERTICAL_DELTA = 1;

    private final WorldLayer worldLayer;

    public BlockLineOfWalk(WorldLayer worldLayer) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
    }

    @Override
    public boolean hasLineOfWalk(BlockPosition from, BlockPosition to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (Math.abs(to.y() - from.y()) > MAX_VERTICAL_DELTA) {
            return false;
        }
        return hasSampledLineOfWalk(from, to);
    }

    private boolean hasSampledLineOfWalk(BlockPosition from, BlockPosition to) {
        int steps = horizontalSteps(from, to);
        BlockPosition previous = from;
        for (int step = 1; step <= steps; step++) {
            BlockPosition sample = sample(from, to, step, steps);
            if (!canWalkStep(previous, sample)) {
                return false;
            }
            previous = sample;
        }
        return BlockStandability.isStandable(worldLayer, to);
    }

    private boolean canWalkStep(BlockPosition from, BlockPosition to) {
        if (from.equals(to)) {
            return true;
        }
        return BlockStandability.isStandable(worldLayer, to) && canUseDiagonal(from, to);
    }

    private boolean canUseDiagonal(BlockPosition from, BlockPosition to) {
        if (!isDiagonal(from, to)) {
            return true;
        }
        return isSideStandable(from, to, Integer.compare(to.x(), from.x()), 0)
                && isSideStandable(from, to, 0, Integer.compare(to.z(), from.z()));
    }

    private boolean isSideStandable(BlockPosition from, BlockPosition to, int xOffset, int zOffset) {
        int yOffset = to.y() - from.y();
        BlockPosition side = from.offset(xOffset, yOffset, zOffset);
        return BlockStandability.isStandable(worldLayer, side);
    }

    private static BlockPosition sample(BlockPosition from, BlockPosition to, int step, int steps) {
        return new BlockPosition(
                interpolate(from.x(), to.x(), step, steps),
                interpolate(from.y(), to.y(), step, steps),
                interpolate(from.z(), to.z(), step, steps));
    }

    private static int interpolate(int from, int to, int step, int steps) {
        double progress = (double) step / steps;
        return (int) Math.round(from + (to - from) * progress);
    }

    private static int horizontalSteps(BlockPosition from, BlockPosition to) {
        return Math.max(Math.abs(to.x() - from.x()), Math.abs(to.z() - from.z()));
    }

    private static boolean isDiagonal(BlockPosition from, BlockPosition to) {
        return from.x() != to.x() && from.z() != to.z();
    }
}

package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.smooth.PathNodePreservation;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public final class SurfaceSmoothingPolicy implements PathNodePreservation<SurfaceNode> {
    private static final double FLOOR_EPSILON = 0.001;

    private final SurfaceWorldLayer worldLayer;

    public SurfaceSmoothingPolicy(SurfaceWorldLayer worldLayer) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
    }

    @Override
    public boolean mustPreserve(SurfaceNode previous, SurfaceNode current, SurfaceNode next) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(next, "next");
        SurfaceBlock previousBlock = block(previous);
        SurfaceBlock currentBlock = block(current);
        SurfaceBlock nextBlock = block(next);
        return hasActionTransition(previous, current, previousBlock, currentBlock)
                || hasActionTransition(current, next, currentBlock, nextBlock)
                || hasSpecialBehavior(currentBlock);
    }

    private static boolean hasActionTransition(
            SurfaceNode from,
            SurfaceNode to,
            SurfaceBlock fromBlock,
            SurfaceBlock toBlock) {
        return changesFloor(from, to)
                || changesSupportHeight(from, to)
                || changesBehavior(fromBlock, toBlock)
                || changesFluidHandling(fromBlock, toBlock);
    }

    private static boolean changesFloor(SurfaceNode from, SurfaceNode to) {
        return Math.abs(from.floorY() - to.floorY()) > FLOOR_EPSILON;
    }

    private static boolean changesSupportHeight(SurfaceNode from, SurfaceNode to) {
        return from.blockPosition().y() != to.blockPosition().y();
    }

    private static boolean changesBehavior(SurfaceBlock from, SurfaceBlock to) {
        return from.behavior().key() != to.behavior().key();
    }

    private static boolean changesFluidHandling(SurfaceBlock from, SurfaceBlock to) {
        return from.classification().fluidHandling() != to.classification().fluidHandling();
    }

    private static boolean hasSpecialBehavior(SurfaceBlock block) {
        return block.behavior().key() != BlockBehaviorKey.FULL_BLOCK;
    }

    private SurfaceBlock block(SurfaceNode node) {
        return worldLayer.surfaceBlock(node.blockPosition());
    }
}

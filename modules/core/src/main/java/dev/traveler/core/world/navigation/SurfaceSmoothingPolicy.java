package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.smooth.PathNodePreservation;
import dev.traveler.core.world.block.BlockPosition;
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
                || hasUnsafeHorizontalTurn(previous, current, next);
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

    private boolean hasUnsafeHorizontalTurn(SurfaceNode previous, SurfaceNode current, SurfaceNode next) {
        return changesHorizontalDirection(previous, current, next) && hasNearbyBodyBlock(current);
    }

    private static boolean changesHorizontalDirection(SurfaceNode previous, SurfaceNode current, SurfaceNode next) {
        int firstX = Integer.compare(globalX(current) - globalX(previous), 0);
        int firstZ = Integer.compare(globalZ(current) - globalZ(previous), 0);
        int secondX = Integer.compare(globalX(next) - globalX(current), 0);
        int secondZ = Integer.compare(globalZ(next) - globalZ(current), 0);
        return firstX != secondX || firstZ != secondZ;
    }

    private boolean hasNearbyBodyBlock(SurfaceNode node) {
        int bodyY = (int) Math.floor(node.floorY() + FLOOR_EPSILON);
        for (HorizontalOffset offset : HorizontalDirections.EIGHT_WAY) {
            if (hasBodyBlockAt(node, offset, bodyY)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBodyBlockAt(SurfaceNode node, HorizontalOffset offset, int bodyY) {
        BlockPosition position =
                new BlockPosition(node.blockPosition().x() + offset.x(), bodyY, node.blockPosition().z() + offset.z());
        return !worldLayer.surfaceBlock(position).shape().isEmpty();
    }

    private SurfaceBlock block(SurfaceNode node) {
        return worldLayer.surfaceBlock(node.blockPosition());
    }

    private static int globalX(SurfaceNode node) {
        return SurfaceTraversalGraph.globalX(node);
    }

    private static int globalZ(SurfaceNode node) {
        return SurfaceTraversalGraph.globalZ(node);
    }
}

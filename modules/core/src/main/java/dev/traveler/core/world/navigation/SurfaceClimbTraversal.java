package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.special.ClimbableBlockBehavior;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SurfaceClimbTraversal {
    private static final double FLOOR_EPSILON = 0.001;

    private SurfaceClimbTraversal() {}

    public static boolean canClimb(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        return canClimbWithLookup(layer::surfaceBlock, from, to, capabilities, SurfaceClimbTraversal::isClimbable);
    }

    public static boolean preservesRouteGeometry(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        return canClimbWithLookup(
                layer::surfaceBlock,
                from,
                to,
                capabilities,
                SurfaceClimbTraversal::preservesClimbRouteGeometry);
    }

    static boolean canClimbWithLookup(
            BlockLookup blocks,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        return canClimbWithLookup(blocks, from, to, capabilities, SurfaceClimbTraversal::isClimbable);
    }

    private static boolean canClimbWithLookup(
            BlockLookup blocks,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities,
            ClimbBlockRule climbBlockRule) {
        BlockLookup safeBlocks = Objects.requireNonNull(blocks, "blocks");
        SurfaceNode safeFrom = Objects.requireNonNull(from, "from");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        MovementCapabilities safeCapabilities = Objects.requireNonNull(capabilities, "capabilities");
        if (!requiresClimb(safeFrom, safeTo, safeCapabilities)) {
            return false;
        }
        for (BlockColumn column : sharedClimbColumns(safeFrom, safeTo)) {
            if (hasContinuousClimbColumn(
                    safeBlocks,
                    column,
                    safeFrom.floorY(),
                    safeTo.floorY(),
                    safeCapabilities,
                    climbBlockRule)) {
                return true;
            }
        }
        return false;
    }

    private static boolean requiresClimb(
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        double floorDelta = Math.abs(to.floorY() - from.floorY());
        return capabilities.canWalk() && floorDelta > capabilities.maxStepUp() + FLOOR_EPSILON;
    }

    private static Set<BlockColumn> sharedClimbColumns(SurfaceNode from, SurfaceNode to) {
        Set<BlockColumn> fromColumns = climbColumnsAdjacentTo(from);
        Set<BlockColumn> shared = new HashSet<>();
        for (BlockColumn column : climbColumnsAdjacentTo(to)) {
            if (fromColumns.contains(column)) {
                shared.add(column);
            }
        }
        return Set.copyOf(shared);
    }

    private static Set<BlockColumn> climbColumnsAdjacentTo(SurfaceNode node) {
        Set<BlockColumn> columns = new HashSet<>();
        for (HorizontalOffset offset : HorizontalDirections.CARDINAL) {
            columns.add(new BlockColumn(
                    blockCoordinate(SurfaceTraversalGraph.globalX(node) + offset.x()),
                    blockCoordinate(SurfaceTraversalGraph.globalZ(node) + offset.z())));
        }
        return Set.copyOf(columns);
    }

    private static boolean hasContinuousClimbColumn(
            BlockLookup blocks,
            BlockColumn column,
            double fromFloorY,
            double toFloorY,
            MovementCapabilities capabilities,
            ClimbBlockRule climbBlockRule) {
        int minY = (int) Math.floor(Math.min(fromFloorY, toFloorY) + FLOOR_EPSILON);
        int maxYExclusive = (int) Math.ceil(Math.max(fromFloorY, toFloorY) - FLOOR_EPSILON);
        for (int y = minY; y < maxYExclusive; y++) {
            if (!climbBlockRule.matches(blocks.get(new BlockPosition(column.x(), y, column.z())), capabilities)) {
                return false;
            }
        }
        return maxYExclusive > minY;
    }

    static boolean isClimbable(SurfaceBlock block, MovementCapabilities capabilities) {
        return climbableBehavior(block.behavior())
                .map(behavior -> behavior.supportsClimbing(capabilities))
                .orElse(false);
    }

    private static boolean preservesClimbRouteGeometry(SurfaceBlock block, MovementCapabilities capabilities) {
        return climbableBehavior(block.behavior())
                .map(behavior -> behavior.preservesRouteGeometry(capabilities))
                .orElse(false);
    }

    private static Optional<ClimbableBlockBehavior> climbableBehavior(BlockBehavior behavior) {
        if (behavior instanceof ClimbableBlockBehavior climbable) {
            return Optional.of(climbable);
        }
        if (behavior instanceof WaterloggedBlockBehavior waterlogged) {
            return climbableBehavior(waterlogged.delegate());
        }
        return Optional.empty();
    }

    private static int blockCoordinate(int globalCoordinate) {
        return Math.floorDiv(globalCoordinate, 2);
    }

    @FunctionalInterface
    interface BlockLookup {
        SurfaceBlock get(BlockPosition position);
    }

    @FunctionalInterface
    private interface ClimbBlockRule {
        boolean matches(SurfaceBlock block, MovementCapabilities capabilities);
    }

    private record BlockColumn(int x, int z) {}
}

package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
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
        for (ClimbContact fromContact : climbContactsAdjacentTo(safeFrom)) {
            if (canClimbFromContact(
                    safeBlocks,
                    fromContact,
                    safeTo,
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

    private static boolean canClimbFromContact(
            BlockLookup blocks,
            ClimbContact fromContact,
            SurfaceNode to,
            double fromFloorY,
            double toFloorY,
            MovementCapabilities capabilities,
            ClimbBlockRule climbBlockRule) {
        for (ClimbContact toContact : climbContactsAdjacentTo(to)) {
            if (canClimbBetweenContacts(
                    blocks,
                    fromContact,
                    toContact,
                    fromFloorY,
                    toFloorY,
                    capabilities,
                    climbBlockRule)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canClimbBetweenContacts(
            BlockLookup blocks,
            ClimbContact fromContact,
            ClimbContact toContact,
            double fromFloorY,
            double toFloorY,
            MovementCapabilities capabilities,
            ClimbBlockRule climbBlockRule) {
        if (!fromContact.column().equals(toContact.column())) {
            return false;
        }
        return hasContinuousClimbColumn(
                        blocks,
                        fromContact.column(),
                        fromFloorY,
                        toFloorY,
                        capabilities,
                        fromContact.face(),
                        climbBlockRule)
                || hasContinuousClimbColumn(
                        blocks,
                        toContact.column(),
                        fromFloorY,
                        toFloorY,
                        capabilities,
                        toContact.face(),
                        climbBlockRule);
    }

    private static Set<ClimbContact> climbContactsAdjacentTo(SurfaceNode node) {
        Set<ClimbContact> contacts = new HashSet<>();
        for (HorizontalOffset offset : HorizontalDirections.CARDINAL) {
            contacts.add(climbContact(node, offset));
        }
        return Set.copyOf(contacts);
    }

    private static ClimbContact climbContact(SurfaceNode node, HorizontalOffset offset) {
        return new ClimbContact(
                new BlockColumn(
                        blockCoordinate(SurfaceTraversalGraph.globalX(node) + offset.x()),
                        blockCoordinate(SurfaceTraversalGraph.globalZ(node) + offset.z())),
                faceFromClimbBlockToNode(offset));
    }

    private static HorizontalFacing faceFromClimbBlockToNode(HorizontalOffset offsetFromNodeToClimb) {
        int x = -offsetFromNodeToClimb.x();
        int z = -offsetFromNodeToClimb.z();
        if (x < 0) {
            return HorizontalFacing.WEST;
        }
        if (x > 0) {
            return HorizontalFacing.EAST;
        }
        if (z < 0) {
            return HorizontalFacing.NORTH;
        }
        return HorizontalFacing.SOUTH;
    }

    private static boolean hasContinuousClimbColumn(
            BlockLookup blocks,
            BlockColumn column,
            double fromFloorY,
            double toFloorY,
            MovementCapabilities capabilities,
            HorizontalFacing face,
            ClimbBlockRule climbBlockRule) {
        int minY = (int) Math.floor(Math.min(fromFloorY, toFloorY) + FLOOR_EPSILON);
        int maxYExclusive = (int) Math.ceil(Math.max(fromFloorY, toFloorY) - FLOOR_EPSILON);
        for (int y = minY; y < maxYExclusive; y++) {
            if (!climbBlockRule.matches(blocks.get(new BlockPosition(column.x(), y, column.z())), capabilities, face)) {
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

    private static boolean isClimbable(
            SurfaceBlock block,
            MovementCapabilities capabilities,
            HorizontalFacing face) {
        return climbableBehavior(block.behavior())
                .map(behavior -> behavior.supportsClimbingFrom(face, capabilities))
                .orElse(false);
    }

    private static boolean preservesClimbRouteGeometry(
            SurfaceBlock block,
            MovementCapabilities capabilities,
            HorizontalFacing face) {
        return climbableBehavior(block.behavior())
                .map(behavior -> behavior.preservesRouteGeometry(capabilities)
                        && behavior.supportsClimbingFrom(face, capabilities))
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
        boolean matches(SurfaceBlock block, MovementCapabilities capabilities, HorizontalFacing face);
    }

    private record BlockColumn(int x, int z) {}

    private record ClimbContact(BlockColumn column, HorizontalFacing face) {}
}

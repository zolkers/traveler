package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.ClimbableBlockBehavior;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SurfaceClimbTraversal {
    private static final double FLOOR_EPSILON = 0.001;
    private static final double CLIMB_FACE_DISTANCE = 0.3;

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

    public static Optional<NavigationPoint> climbTarget(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        return climbTargetWithLookup(layer::surfaceBlock, from, to, capabilities);
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
            if (climbContactFrom(
                            safeBlocks,
                            fromContact,
                            safeTo,
                            safeFrom.floorY(),
                            safeTo.floorY(),
                            safeCapabilities,
                            climbBlockRule)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static Optional<NavigationPoint> climbTargetWithLookup(
            BlockLookup blocks,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        BlockLookup safeBlocks = Objects.requireNonNull(blocks, "blocks");
        SurfaceNode safeFrom = Objects.requireNonNull(from, "from");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        MovementCapabilities safeCapabilities = Objects.requireNonNull(capabilities, "capabilities");
        if (!requiresClimb(safeFrom, safeTo, safeCapabilities)) {
            return Optional.empty();
        }
        for (ClimbContact fromContact : climbContactsAdjacentTo(safeFrom)) {
            Optional<ClimbContact> contact = climbContactFrom(
                    safeBlocks,
                    fromContact,
                    safeTo,
                    safeFrom.floorY(),
                    safeTo.floorY(),
                    safeCapabilities,
                    SurfaceClimbTraversal::isClimbable);
            if (contact.isPresent()) {
                return contact.map(value -> climbTargetPoint(value, safeTo.floorY()));
            }
        }
        return Optional.empty();
    }

    private static boolean requiresClimb(
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        double floorDelta = Math.abs(to.floorY() - from.floorY());
        return capabilities.canWalk() && floorDelta > capabilities.maxStepUp() + FLOOR_EPSILON;
    }

    private static Optional<ClimbContact> climbContactFrom(
            BlockLookup blocks,
            ClimbContact fromContact,
            SurfaceNode to,
            double fromFloorY,
            double toFloorY,
            MovementCapabilities capabilities,
            ClimbBlockRule climbBlockRule) {
        for (ClimbContact toContact : climbContactsAdjacentTo(to)) {
            Optional<ClimbContact> contact = climbContactBetween(
                    blocks,
                    fromContact,
                    toContact,
                    fromFloorY,
                    toFloorY,
                    capabilities,
                    climbBlockRule);
            if (contact.isPresent()) {
                return contact;
            }
        }
        return Optional.empty();
    }

    private static Optional<ClimbContact> climbContactBetween(
            BlockLookup blocks,
            ClimbContact fromContact,
            ClimbContact toContact,
            double fromFloorY,
            double toFloorY,
            MovementCapabilities capabilities,
            ClimbBlockRule climbBlockRule) {
        if (!fromContact.column().equals(toContact.column())) {
            return Optional.empty();
        }
        if (hasContinuousClimbColumn(
                        blocks,
                        fromContact.column(),
                        fromFloorY,
                        toFloorY,
                        capabilities,
                        fromContact.face(),
                        climbBlockRule)) {
            return Optional.of(fromContact);
        }
        if (hasContinuousClimbColumn(
                        blocks,
                        toContact.column(),
                        fromFloorY,
                        toFloorY,
                        capabilities,
                        toContact.face(),
                        climbBlockRule)) {
            return Optional.of(toContact);
        }
        return Optional.empty();
    }

    private static List<ClimbContact> climbContactsAdjacentTo(SurfaceNode node) {
        Set<ClimbContact> seen = new HashSet<>();
        List<ClimbContact> contacts = new ArrayList<>(HorizontalDirections.CARDINAL.length);
        for (HorizontalOffset offset : HorizontalDirections.CARDINAL) {
            ClimbContact contact = climbContact(node, offset);
            if (seen.add(contact)) {
                contacts.add(contact);
            }
        }
        return List.copyOf(contacts);
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

    private static NavigationPoint climbTargetPoint(ClimbContact contact, double floorY) {
        BlockColumn column = contact.column();
        double x = switch (contact.face()) {
            case WEST -> column.x() - CLIMB_FACE_DISTANCE;
            case EAST -> column.x() + 1.0 + CLIMB_FACE_DISTANCE;
            case NORTH, SOUTH -> column.x() + 0.5;
        };
        double z = switch (contact.face()) {
            case NORTH -> column.z() - CLIMB_FACE_DISTANCE;
            case SOUTH -> column.z() + 1.0 + CLIMB_FACE_DISTANCE;
            case WEST, EAST -> column.z() + 0.5;
        };
        return new NavigationPoint(x, floorY, z);
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
                .map(behavior -> !behavior.allowsRouteSmoothing(capabilities)
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

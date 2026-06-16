package dev.traveler.core.world.navigation;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.BlockBehaviors;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.ClimbableBlockBehavior;
import dev.traveler.core.world.behavior.special.ClimbSurfaceGeometry;
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

    private SurfaceClimbTraversal() {}

    public static boolean canClimb(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        return canClimbWithLookup(layer::surfaceBlock, from, to, capabilities, SurfaceClimbTraversal::climbSurface);
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
                SurfaceClimbTraversal::preservedClimbSurface);
    }

    public static Optional<WorldPoint> climbTarget(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        return climbTargetWithLookup(layer::surfaceBlock, from, to, capabilities);
    }

    public static Optional<WorldPoint> climbFaceTarget(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        return climbContactWithLookup(layer::surfaceBlock, from, to, capabilities, false)
                .map(contact -> climbTargetPoint(contact, Objects.requireNonNull(to, "to").floorY()));
    }

    public static Optional<List<SurfaceNode>> climbRouteNodes(
            SurfaceWorldLayer worldLayer,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        return climbRouteNodesWithLookup(layer::surfaceBlock, from, to, capabilities);
    }

    public static List<SurfaceNode> climbStartNodes(
            SurfaceWorldLayer worldLayer,
            BlockPosition feetPosition,
            MovementCapabilities capabilities) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        BlockPosition position = Objects.requireNonNull(feetPosition, "feetPosition");
        MovementCapabilities safeCapabilities = Objects.requireNonNull(capabilities, "capabilities");
        Optional<ClimbableBlockBehavior> climbable =
                BlockBehaviors.climbable(layer.surfaceBlock(position).behavior());
        if (climbable.isEmpty()) {
            return List.of();
        }
        ClimbableBlockBehavior behavior = climbable.orElseThrow();
        if (!behavior.supportsClimbing(safeCapabilities)) {
            return List.of();
        }
        List<SurfaceNode> starts = new ArrayList<>();
        for (HorizontalFacing face : HorizontalFacing.values()) {
            behavior.climbSurface(face, safeCapabilities)
                    .map(surface -> surface.node(position))
                    .ifPresent(starts::add);
        }
        return List.copyOf(starts);
    }

    public static Optional<WorldPoint> climbStartTarget(
            SurfaceWorldLayer worldLayer,
            SurfaceNode node,
            MovementCapabilities capabilities) {
        SurfaceWorldLayer layer = Objects.requireNonNull(worldLayer, "worldLayer");
        SurfaceNode safeNode = Objects.requireNonNull(node, "node");
        return climbStartContact(layer, safeNode, capabilities)
                .map(contact -> climbTargetPoint(contact, safeNode.floorY()));
    }

    static boolean canClimbWithLookup(
            BlockLookup blocks,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        return canClimbWithLookup(blocks, from, to, capabilities, SurfaceClimbTraversal::climbSurface);
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
        for (ClimbFaceContact fromContact : climbContactsAdjacentTo(safeFrom)) {
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

    private static Optional<WorldPoint> climbTargetWithLookup(
            BlockLookup blocks,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        return climbContactWithLookup(blocks, from, to, capabilities, true)
                .map(value -> climbTargetPoint(value, Objects.requireNonNull(to, "to").floorY()));
    }

    private static Optional<List<SurfaceNode>> climbRouteNodesWithLookup(
            BlockLookup blocks,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities) {
        SurfaceNode safeFrom = Objects.requireNonNull(from, "from");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        return climbContactWithLookup(blocks, safeFrom, safeTo, capabilities, true)
                .map(contact -> climbColumnNodes(contact, safeFrom.floorY(), safeTo.floorY()));
    }

    private static Optional<ClimbContact> climbContactWithLookup(
            BlockLookup blocks,
            SurfaceNode from,
            SurfaceNode to,
            MovementCapabilities capabilities,
            boolean requiresVerticalClimb) {
        BlockLookup safeBlocks = Objects.requireNonNull(blocks, "blocks");
        SurfaceNode safeFrom = Objects.requireNonNull(from, "from");
        SurfaceNode safeTo = Objects.requireNonNull(to, "to");
        MovementCapabilities safeCapabilities = Objects.requireNonNull(capabilities, "capabilities");
        if (requiresVerticalClimb && !requiresClimb(safeFrom, safeTo, safeCapabilities)) {
            return Optional.empty();
        }
        for (ClimbFaceContact fromContact : climbContactsAdjacentTo(safeFrom)) {
            Optional<ClimbContact> contact = climbContactFrom(
                    safeBlocks,
                    fromContact,
                    safeTo,
                    safeFrom.floorY(),
                    safeTo.floorY(),
                    safeCapabilities,
                    SurfaceClimbTraversal::climbSurface);
            if (contact.isPresent()) {
                return contact;
            }
        }
        return Optional.empty();
    }

    private static List<SurfaceNode> climbColumnNodes(ClimbContact contact, double fromFloorY, double toFloorY) {
        int minY = (int) Math.floor(Math.min(fromFloorY, toFloorY) + FLOOR_EPSILON);
        int maxYExclusive = (int) Math.ceil(Math.max(fromFloorY, toFloorY) - FLOOR_EPSILON);
        if (maxYExclusive <= minY) {
            return List.of();
        }
        List<SurfaceNode> nodes = new ArrayList<>(maxYExclusive - minY);
        if (toFloorY >= fromFloorY) {
            for (int y = minY; y < maxYExclusive; y++) {
                nodes.add(climbNode(contact, y));
            }
            return List.copyOf(nodes);
        }
        for (int y = maxYExclusive - 1; y >= minY; y--) {
            nodes.add(climbNode(contact, y));
        }
        return List.copyOf(nodes);
    }

    private static SurfaceNode climbNode(ClimbContact contact, int y) {
        BlockColumn column = contact.column();
        return contact.geometry().node(new BlockPosition(column.x(), y, column.z()));
    }

    private static Optional<ClimbContact> climbStartContact(
            SurfaceWorldLayer worldLayer,
            SurfaceNode node,
            MovementCapabilities capabilities) {
        SurfaceNode safeNode = Objects.requireNonNull(node, "node");
        MovementCapabilities safeCapabilities = Objects.requireNonNull(capabilities, "capabilities");
        if (!sameFloor(safeNode.floorY(), safeNode.blockPosition().y())) {
            return Optional.empty();
        }
        Optional<ClimbableBlockBehavior> climbable =
                BlockBehaviors.climbable(worldLayer.surfaceBlock(safeNode.blockPosition()).behavior());
        if (climbable.isEmpty()) {
            return Optional.empty();
        }
        ClimbableBlockBehavior behavior = climbable.orElseThrow();
        if (!behavior.supportsClimbing(safeCapabilities)) {
            return Optional.empty();
        }
        BlockPosition position = safeNode.blockPosition();
        for (HorizontalFacing face : HorizontalFacing.values()) {
            Optional<ClimbSurfaceGeometry> geometry = behavior.climbSurface(face, safeCapabilities);
            if (geometry.isPresent() && geometry.orElseThrow().node(position).sameSubcell(safeNode)) {
                return Optional.of(new ClimbContact(
                        new BlockColumn(position.x(), position.z()),
                        face,
                        geometry.orElseThrow()));
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
            ClimbFaceContact fromContact,
            SurfaceNode to,
            double fromFloorY,
            double toFloorY,
            MovementCapabilities capabilities,
            ClimbBlockRule climbBlockRule) {
        for (ClimbFaceContact toContact : climbContactsAdjacentTo(to)) {
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
            ClimbFaceContact fromContact,
            ClimbFaceContact toContact,
            double fromFloorY,
            double toFloorY,
            MovementCapabilities capabilities,
            ClimbBlockRule climbBlockRule) {
        if (!fromContact.column().equals(toContact.column())) {
            return Optional.empty();
        }
        Optional<ClimbSurfaceGeometry> fromGeometry = continuousClimbSurface(
                        blocks,
                        fromContact.column(),
                        fromFloorY,
                        toFloorY,
                        capabilities,
                        fromContact.face(),
                        climbBlockRule);
        if (fromGeometry.isPresent()) {
            return Optional.of(new ClimbContact(
                    fromContact.column(),
                    fromContact.face(),
                    fromGeometry.orElseThrow()));
        }
        Optional<ClimbSurfaceGeometry> toGeometry = continuousClimbSurface(
                        blocks,
                        toContact.column(),
                        fromFloorY,
                        toFloorY,
                        capabilities,
                        toContact.face(),
                        climbBlockRule);
        if (toGeometry.isPresent()) {
            return Optional.of(new ClimbContact(
                    toContact.column(),
                    toContact.face(),
                    toGeometry.orElseThrow()));
        }
        return Optional.empty();
    }

    private static List<ClimbFaceContact> climbContactsAdjacentTo(SurfaceNode node) {
        Set<ClimbFaceContact> seen = new HashSet<>();
        List<ClimbFaceContact> contacts = new ArrayList<>(HorizontalDirections.CARDINAL.length);
        for (HorizontalOffset offset : HorizontalDirections.CARDINAL) {
            ClimbFaceContact contact = climbContact(node, offset);
            if (seen.add(contact)) {
                contacts.add(contact);
            }
        }
        return List.copyOf(contacts);
    }

    private static ClimbFaceContact climbContact(SurfaceNode node, HorizontalOffset offset) {
        return new ClimbFaceContact(
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

    private static Optional<ClimbSurfaceGeometry> continuousClimbSurface(
            BlockLookup blocks,
            BlockColumn column,
            double fromFloorY,
            double toFloorY,
            MovementCapabilities capabilities,
            HorizontalFacing face,
            ClimbBlockRule climbBlockRule) {
        int minY = (int) Math.floor(Math.min(fromFloorY, toFloorY) + FLOOR_EPSILON);
        int maxYExclusive = (int) Math.ceil(Math.max(fromFloorY, toFloorY) - FLOOR_EPSILON);
        if (maxYExclusive <= minY) {
            SurfaceBlock block = blocks.get(new BlockPosition(column.x(), minY, column.z()));
            return climbBlockRule.surface(block, capabilities, face);
        }
        ClimbSurfaceGeometry geometry = null;
        for (int y = minY; y < maxYExclusive; y++) {
            Optional<ClimbSurfaceGeometry> current = climbBlockRule.surface(
                    blocks.get(new BlockPosition(column.x(), y, column.z())),
                    capabilities,
                    face);
            if (current.isEmpty()) {
                return Optional.empty();
            }
            ClimbSurfaceGeometry currentGeometry = current.orElseThrow();
            if (geometry != null && !geometry.equals(currentGeometry)) {
                return Optional.empty();
            }
            geometry = currentGeometry;
        }
        return Optional.ofNullable(geometry);
    }

    private static WorldPoint climbTargetPoint(ClimbContact contact, double floorY) {
        BlockColumn column = contact.column();
        return contact.geometry().target(
                new BlockPosition(column.x(), (int) Math.floor(floorY + FLOOR_EPSILON), column.z()),
                floorY);
    }

    public static boolean isClimbable(SurfaceBlock block, MovementCapabilities capabilities) {
        return BlockBehaviors.climbable(block.behavior())
                .map(behavior -> behavior.supportsClimbing(capabilities))
                .orElse(false);
    }

    private static Optional<ClimbSurfaceGeometry> climbSurface(
            SurfaceBlock block,
            MovementCapabilities capabilities,
            HorizontalFacing face) {
        return BlockBehaviors.climbable(block.behavior())
                .flatMap(behavior -> behavior.climbSurface(face, capabilities));
    }

    private static Optional<ClimbSurfaceGeometry> preservedClimbSurface(
            SurfaceBlock block,
            MovementCapabilities capabilities,
            HorizontalFacing face) {
        return BlockBehaviors.climbable(block.behavior())
                .filter(behavior -> !behavior.allowsRouteSmoothing(capabilities))
                .flatMap(behavior -> behavior.climbSurface(face, capabilities));
    }

    private static int blockCoordinate(int globalCoordinate) {
        return Math.floorDiv(globalCoordinate, 2);
    }

    private static boolean sameFloor(double first, double second) {
        return Math.abs(first - second) <= FLOOR_EPSILON;
    }

    @FunctionalInterface
    interface BlockLookup {
        SurfaceBlock get(BlockPosition position);
    }

    @FunctionalInterface
    private interface ClimbBlockRule {
        Optional<ClimbSurfaceGeometry> surface(
                SurfaceBlock block,
                MovementCapabilities capabilities,
                HorizontalFacing face);
    }

    private record BlockColumn(int x, int z) {}

    private record ClimbFaceContact(BlockColumn column, HorizontalFacing face) {}

    private record ClimbContact(BlockColumn column, HorizontalFacing face, ClimbSurfaceGeometry geometry) {}
}

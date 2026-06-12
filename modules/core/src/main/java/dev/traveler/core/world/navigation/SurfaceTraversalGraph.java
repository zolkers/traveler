package dev.traveler.core.world.navigation;

import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SurfaceTraversalGraph implements Graph<SurfaceNode> {
    private static final double PLAYER_HEIGHT = 1.8;
    private static final double BODY_EPSILON = 0.0001;
    private static final double FLOOR_EPSILON = 0.001;
    private static final int[] SUPPORT_Y_OFFSETS = {0, 1, -1};
    private static final int MAX_CONNECTIONS_PER_NODE =
            HorizontalDirections.EIGHT_WAY.length * SUPPORT_Y_OFFSETS.length;

    private final SurfaceWorldLayer worldLayer;
    private final SearchBounds bounds;
    private final MovementCapabilities capabilities;
    private final SurfaceBlockCache surfaceBlocks;

    public SurfaceTraversalGraph(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            SurfaceNode goal,
            MovementCapabilities capabilities,
            int horizontalMargin,
            int verticalMargin) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
        SurfaceNode safeStart = Objects.requireNonNull(start, "start");
        SurfaceNode safeGoal = Objects.requireNonNull(goal, "goal");
        SearchBounds searchBounds = SearchBounds.around(
                safeStart.blockPosition(),
                safeGoal.blockPosition(),
                horizontalMargin,
                verticalMargin);
        this.bounds = searchBounds;
        this.surfaceBlocks = new SurfaceBlockCache(this.worldLayer, searchBounds);
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    @Override
    public Iterable<Connection<SurfaceNode>> outgoingConnections(SurfaceNode node) {
        Objects.requireNonNull(node, "node");
        if (!insideBounds(node) || !canStandOn(node)) {
            return List.of();
        }
        return connectionsFrom(node);
    }

    private List<Connection<SurfaceNode>> connectionsFrom(SurfaceNode node) {
        List<Connection<SurfaceNode>> connections = new ArrayList<>(MAX_CONNECTIONS_PER_NODE);
        for (HorizontalOffset direction : HorizontalDirections.EIGHT_WAY) {
            addDirectionConnections(node, direction, connections);
        }
        return connections;
    }

    private void addDirectionConnections(
            SurfaceNode node,
            HorizontalOffset direction,
            List<Connection<SurfaceNode>> connections) {
        int destinationX = globalX(node) + direction.x();
        int destinationZ = globalZ(node) + direction.z();
        for (int yOffset : SUPPORT_Y_OFFSETS) {
            addConnectionForCandidateY(node, direction, connections, destinationX, destinationZ, yOffset);
        }
    }

    private void addConnectionForCandidateY(
            SurfaceNode node,
            HorizontalOffset direction,
            List<Connection<SurfaceNode>> connections,
            int destinationX,
            int destinationZ,
            int yOffset) {
        SurfaceNode candidate = surfaceNode(destinationX, node.blockPosition().y() + yOffset, destinationZ);
        if (candidate == null) {
            return;
        }
        SurfaceBlock block = surfaceBlock(candidate.blockPosition());
        if (!canReach(node, candidate, block, direction)) {
            return;
        }
        connections.add(new Connection<>(node, candidate, movementCost(node, candidate)));
    }

    private boolean canReach(SurfaceNode from, SurfaceNode to, SurfaceBlock block, HorizontalOffset direction) {
        return insideBounds(to)
                && hasBodyClearance(to)
                && destinationAllowsMovement(from, to, block, direction)
                && canUseDirection(from, to, direction);
    }

    private boolean canUseDirection(SurfaceNode from, SurfaceNode to, HorizontalOffset direction) {
        if (!direction.isDiagonal()) {
            return true;
        }
        return canMoveDiagonally(from, to, direction);
    }

    private boolean canMoveDiagonally(SurfaceNode from, SurfaceNode to, HorizontalOffset direction) {
        int originX = globalX(from);
        int originZ = globalZ(from);
        return hasReachableSide(from, originX + direction.x(), originZ, to.floorY())
                && hasReachableSide(from, originX, originZ + direction.z(), to.floorY());
    }

    private boolean hasReachableSide(SurfaceNode from, int globalX, int globalZ, double targetFloorY) {
        for (int yOffset : SUPPORT_Y_OFFSETS) {
            if (canUseAsDiagonalSideAtY(from, globalX, globalZ, targetFloorY, yOffset)) {
                return true;
            }
        }
        return false;
    }

    private boolean canUseAsDiagonalSideAtY(
            SurfaceNode from,
            int globalX,
            int globalZ,
            double targetFloorY,
            int yOffset) {
        SurfaceNode candidate = surfaceNode(globalX, from.blockPosition().y() + yOffset, globalZ);
        if (candidate == null) {
            return false;
        }
        SurfaceBlock block = surfaceBlock(candidate.blockPosition());
        return hasBodyClearance(candidate)
                && destinationAllowsMovement(from, candidate, block, directionBetween(from, candidate))
                && Math.abs(candidate.floorY() - targetFloorY) <= upwardClearance();
    }

    private SurfaceNode surfaceNode(int globalX, int blockY, int globalZ) {
        int blockX = blockCoordinate(globalX);
        int blockZ = blockCoordinate(globalZ);
        int cellX = cellCoordinate(globalX);
        int cellZ = cellCoordinate(globalZ);
        SurfaceBlock block = surfaceBlock(blockX, blockY, blockZ);
        if (!block.behavior().supportsStanding(capabilities)) {
            return null;
        }
        double floorHeight = block.shape().floorHeightForCellOrNaN(cellX, cellZ);
        if (Double.isNaN(floorHeight)) {
            return null;
        }
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        return new SurfaceNode(position, cellX, cellZ, blockY + floorHeight);
    }

    SurfaceNode surfaceNodeAt(int globalX, int blockY, int globalZ) {
        return surfaceNode(globalX, blockY, globalZ);
    }

    private boolean canStandOn(SurfaceNode node) {
        SurfaceNode surface = surfaceNode(globalX(node), node.blockPosition().y(), globalZ(node));
        return surface != null && sameFloor(surface, node) && hasBodyClearance(node);
    }

    private boolean hasBodyClearance(SurfaceNode node) {
        double minY = node.floorY() + BODY_EPSILON;
        double maxY = node.floorY() + PLAYER_HEIGHT;
        for (int y = (int) Math.floor(minY); y <= (int) Math.floor(maxY); y++) {
            if (collidesWithBody(node, y, minY, maxY)) {
                return false;
            }
        }
        return true;
    }

    private boolean collidesWithBody(SurfaceNode node, int blockY, double minY, double maxY) {
        SurfaceBlock block = surfaceBlock(node.blockPosition().x(), blockY, node.blockPosition().z());
        double localMinY = clamp(minY - blockY);
        double localMaxY = clamp(maxY - blockY);
        if (localMaxY <= 0.0 || localMinY >= 1.0) {
            return false;
        }
        return block.shape().collidesWithCellBody(node.cellX(), node.cellZ(), localMinY, localMaxY);
    }

    private boolean insideBounds(SurfaceNode node) {
        return bounds.contains(node.blockPosition());
    }

    private double movementCost(SurfaceNode from, SurfaceNode to) {
        double horizontalCost = horizontalCost(from, to);
        double climbCost = climbCost(to.floorY() - from.floorY());
        return horizontalCost + climbCost;
    }

    private static double horizontalCost(SurfaceNode from, SurfaceNode to) {
        double deltaX = from.centerX() - to.centerX();
        double deltaZ = from.centerZ() - to.centerZ();
        return Math.hypot(deltaX, deltaZ);
    }

    private double climbCost(double deltaY) {
        if (deltaY <= 0.0) {
            return Math.abs(deltaY) * 0.1;
        }
        if (deltaY <= capabilities.maxStepUp()) {
            return deltaY * 0.5;
        }
        return 1.0 + deltaY;
    }

    private boolean destinationAllowsMovement(
            SurfaceNode from,
            SurfaceNode to,
            SurfaceBlock block,
            HorizontalOffset direction) {
        return destinationAllowsMovement(from, to, block, MovementDirection.fromOffset(direction.x(), direction.z()));
    }

    private boolean destinationAllowsMovement(
            SurfaceNode from,
            SurfaceNode to,
            SurfaceBlock block,
            MovementDirection direction) {
        SurfaceMovementContext context = new SurfaceMovementContext(from, to, capabilities, direction);
        return block.behavior().evaluateMovement(context).allowed();
    }

    private double upwardClearance() {
        return Math.max(capabilities.maxStepUp(), capabilities.maxJumpHeight());
    }

    private static MovementDirection directionBetween(SurfaceNode from, SurfaceNode to) {
        int xOffset = Double.compare(to.centerX(), from.centerX());
        int zOffset = Double.compare(to.centerZ(), from.centerZ());
        return MovementDirection.fromOffset(xOffset, zOffset);
    }

    private static boolean sameFloor(SurfaceNode first, SurfaceNode second) {
        return Math.abs(first.floorY() - second.floorY()) <= FLOOR_EPSILON;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private SurfaceBlock surfaceBlock(BlockPosition position) {
        return surfaceBlocks.get(position);
    }

    private SurfaceBlock surfaceBlock(int x, int y, int z) {
        return surfaceBlocks.get(x, y, z);
    }

    static int globalX(SurfaceNode node) {
        return node.blockPosition().x() * 2 + node.cellX();
    }

    static int globalZ(SurfaceNode node) {
        return node.blockPosition().z() * 2 + node.cellZ();
    }

    private static int blockCoordinate(int globalCoordinate) {
        return Math.floorDiv(globalCoordinate, 2);
    }

    private static int cellCoordinate(int globalCoordinate) {
        return Math.floorMod(globalCoordinate, 2);
    }

}

package dev.traveler.core.world.navigation;

import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.EntityDimensions;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.movement.MovementProfiles;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.KeyedGraph;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SurfaceTraversalGraph implements KeyedGraph<SurfaceNode> {
    private static final double BODY_EPSILON = 0.0001;
    private static final double FLOOR_EPSILON = 0.001;
    private static final int[] SUPPORT_Y_OFFSETS = {0, 1, -1};
    private static final int SPECIAL_CONNECTIONS_PER_NODE = 8;
    private static final int MAX_CONNECTIONS_PER_NODE =
            HorizontalDirections.EIGHT_WAY.length * SUPPORT_Y_OFFSETS.length + SPECIAL_CONNECTIONS_PER_NODE;

    private final SurfaceWorldLayer worldLayer;
    private final SearchBounds bounds;
    private final EntityDimensions dimensions;
    private final MovementCapabilities capabilities;
    private final SurfaceClearanceScorer clearanceScorer;
    private final SurfaceBodyClearanceMode bodyClearanceMode;
    private final SurfaceMovementEvaluator movementEvaluator;
    private final SurfaceNodeIndex nodeIndex;
    private final double[] clearanceScores;
    private final boolean[] clearanceScoreLoaded;
    private final boolean[] bodyClearance;
    private final boolean[] bodyClearanceLoaded;
    private final boolean[] exactBodyClearance;
    private final boolean[] exactBodyClearanceLoaded;
    private final SurfaceBlockCache surfaceBlocks;

    public SurfaceTraversalGraph(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            SurfaceNode goal,
            MovementCapabilities capabilities,
            int horizontalMargin,
            int verticalMargin) {
        this(
                worldLayer,
                start,
                goal,
                MovementProfiles.defaultPlayerWith(capabilities),
                SurfaceTraversalGraphSettings.basic(horizontalMargin, verticalMargin));
    }

    public SurfaceTraversalGraph(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            SurfaceNode goal,
            MovementProfile movementProfile,
            int horizontalMargin,
            int verticalMargin) {
        this(
                worldLayer,
                start,
                goal,
                movementProfile,
                SurfaceTraversalGraphSettings.basic(horizontalMargin, verticalMargin));
    }

    public SurfaceTraversalGraph(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            SurfaceNode goal,
            MovementCapabilities capabilities,
            SurfaceTraversalGraphSettings settings) {
        this(worldLayer, start, goal, MovementProfiles.defaultPlayerWith(capabilities), settings);
    }

    public SurfaceTraversalGraph(
            SurfaceWorldLayer worldLayer,
            SurfaceNode start,
            SurfaceNode goal,
            MovementProfile movementProfile,
            SurfaceTraversalGraphSettings settings) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
        SurfaceNode safeStart = Objects.requireNonNull(start, "start");
        SurfaceNode safeGoal = Objects.requireNonNull(goal, "goal");
        SurfaceTraversalGraphSettings safeSettings = Objects.requireNonNull(settings, "settings");
        MovementProfile profile = Objects.requireNonNull(movementProfile, "movementProfile");
        SearchBounds searchBounds = SearchBounds.around(
                safeStart.blockPosition(),
                safeGoal.blockPosition(),
                safeSettings.horizontalMargin(),
                safeSettings.verticalMargin());
        this.bounds = searchBounds;
        this.surfaceBlocks = new SurfaceBlockCache(this.worldLayer, searchBounds);
        this.dimensions = profile.dimensions();
        this.capabilities = profile.capabilities();
        this.clearanceScorer = safeSettings.clearanceScorer();
        this.bodyClearanceMode = safeSettings.bodyClearanceMode();
        this.movementEvaluator = new SurfaceMovementEvaluator(capabilities);
        this.nodeIndex = new SurfaceNodeIndex(searchBounds);
        this.clearanceScores = new double[nodeIndex.size()];
        this.clearanceScoreLoaded = new boolean[nodeIndex.size()];
        this.bodyClearance = new boolean[nodeIndex.size()];
        this.bodyClearanceLoaded = new boolean[nodeIndex.size()];
        this.exactBodyClearance = new boolean[nodeIndex.size()];
        this.exactBodyClearanceLoaded = new boolean[nodeIndex.size()];
    }

    @Override
    public Iterable<Connection<SurfaceNode>> outgoingConnections(SurfaceNode node) {
        Objects.requireNonNull(node, "node");
        if (!insideBounds(node) || !canStandOn(node)) {
            return List.of();
        }
        return connectionsFrom(node);
    }

    @Override
    public long keyOf(SurfaceNode node) {
        return nodeIndex.indexOf(node);
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
        addDropConnection(node, direction, connections);
        addJumpConnection(node, direction, connections);
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

    private void addDropConnection(
            SurfaceNode node, HorizontalOffset direction, List<Connection<SurfaceNode>> connections) {
        if (!canAttemptDrop(direction)) {
            return;
        }
        SurfaceNode candidate = dropSurfaceNode(node, direction);
        if (candidate == null) {
            return;
        }
        SurfaceBlock block = surfaceBlock(candidate.blockPosition());
        if (!canReachDrop(node, candidate, block, direction)) {
            return;
        }
        connections.add(new Connection<>(node, candidate, movementCost(node, candidate)));
    }

    private boolean canAttemptDrop(HorizontalOffset direction) {
        return !direction.isDiagonal() && capabilities.maxSafeFallDistance() > capabilities.maxStepUp();
    }

    private SurfaceNode dropSurfaceNode(SurfaceNode node, HorizontalOffset direction) {
        int destinationX = globalX(node) + direction.x() * 2;
        int destinationZ = globalZ(node) + direction.z() * 2;
        return surfaceNode(destinationX, node.blockPosition().y() - 1, destinationZ);
    }

    private boolean canReachDrop(
            SurfaceNode from, SurfaceNode to, SurfaceBlock block, HorizontalOffset direction) {
        return insideBounds(to)
                && isDropDown(from, to)
                && hasBodyClearance(to)
                && destinationAllowsMovement(from, to, block, direction);
    }

    private boolean isDropDown(SurfaceNode from, SurfaceNode to) {
        double delta = from.floorY() - to.floorY();
        return delta > capabilities.maxStepUp() + FLOOR_EPSILON
                && delta <= capabilities.maxSafeFallDistance() + FLOOR_EPSILON;
    }

    private void addJumpConnection(
            SurfaceNode node, HorizontalOffset direction, List<Connection<SurfaceNode>> connections) {
        if (!canAttemptJump(direction)) {
            return;
        }
        SurfaceNode candidate = jumpSurfaceNode(node, direction);
        if (candidate == null) {
            return;
        }
        SurfaceBlock block = surfaceBlock(candidate.blockPosition());
        if (!canReachJump(node, candidate, block, direction)) {
            return;
        }
        connections.add(new Connection<>(node, candidate, movementCost(node, candidate)));
    }

    private boolean canAttemptJump(HorizontalOffset direction) {
        return !direction.isDiagonal() && capabilities.maxJumpHeight() > capabilities.maxStepUp();
    }

    private SurfaceNode jumpSurfaceNode(SurfaceNode node, HorizontalOffset direction) {
        int destinationX = globalX(node) + direction.x() * 2;
        int destinationZ = globalZ(node) + direction.z() * 2;
        return surfaceNode(destinationX, node.blockPosition().y() + 1, destinationZ);
    }

    private boolean canReachJump(
            SurfaceNode from, SurfaceNode to, SurfaceBlock block, HorizontalOffset direction) {
        return insideBounds(to)
                && isJumpUp(from, to)
                && hasBodyClearance(to)
                && destinationAllowsMovement(from, to, block, direction);
    }

    private boolean isJumpUp(SurfaceNode from, SurfaceNode to) {
        double delta = to.floorY() - from.floorY();
        return delta > capabilities.maxStepUp() + FLOOR_EPSILON
                && delta <= capabilities.maxJumpHeight() + FLOOR_EPSILON;
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

    SurfaceNode nearestSurfaceAt(int globalX, int globalZ, double floorY) {
        int baseY = (int) Math.floor(floorY);
        SurfaceNode nearest = null;
        for (int yOffset : SUPPORT_Y_OFFSETS) {
            nearest = nearestOf(nearest, surfaceNodeAt(globalX, baseY + yOffset, globalZ), floorY);
        }
        return nearest;
    }

    boolean hasBodyClearanceAt(SurfaceNode node) {
        return insideBounds(node) && hasExactBodyClearance(node);
    }

    private boolean canStandOn(SurfaceNode node) {
        SurfaceNode surface = surfaceNode(globalX(node), node.blockPosition().y(), globalZ(node));
        return surface != null && sameFloor(surface, node) && hasBodyClearance(node);
    }

    private boolean hasBodyClearance(SurfaceNode node) {
        return cachedBodyClearance(node, bodyClearance, bodyClearanceLoaded, bodyClearanceMode);
    }

    private boolean hasExactBodyClearance(SurfaceNode node) {
        return cachedBodyClearance(
                node,
                exactBodyClearance,
                exactBodyClearanceLoaded,
                SurfaceBodyClearanceMode.EXACT);
    }

    private boolean cachedBodyClearance(
            SurfaceNode node,
            boolean[] cache,
            boolean[] loaded,
            SurfaceBodyClearanceMode mode) {
        int index = nodeIndex.indexOf(node);
        if (loaded[index]) {
            return cache[index];
        }
        boolean clear = computeBodyClearance(node, mode);
        cache[index] = clear;
        loaded[index] = true;
        return clear;
    }

    private boolean computeBodyClearance(SurfaceNode node, SurfaceBodyClearanceMode mode) {
        double minY = node.floorY() + BODY_EPSILON;
        double maxY = node.floorY() + dimensions.height();
        SurfaceBodyFootprint footprint = bodyFootprint(node, mode);
        for (int y = (int) Math.floor(minY); y <= (int) Math.floor(maxY); y++) {
            if (collidesWithFootprint(node, footprint, y, minY, maxY)) {
                return false;
            }
        }
        return true;
    }

    private SurfaceBodyFootprint bodyFootprint(SurfaceNode node, SurfaceBodyClearanceMode mode) {
        if (mode == SurfaceBodyClearanceMode.ADJUSTED) {
            return SurfaceBodyFootprint.adjustedAround(node, dimensions);
        }
        return SurfaceBodyFootprint.around(node, dimensions);
    }

    private boolean collidesWithFootprint(
            SurfaceNode node, SurfaceBodyFootprint footprint, int blockY, double minY, double maxY) {
        for (int globalX = footprint.minGlobalX(); globalX <= footprint.maxGlobalX(); globalX++) {
            if (collidesWithFootprintColumn(node, footprint, globalX, blockY, minY, maxY)) {
                return true;
            }
        }
        return false;
    }

    private boolean collidesWithFootprintColumn(
            SurfaceNode node, SurfaceBodyFootprint footprint, int globalX, int blockY, double minY, double maxY) {
        for (int globalZ = footprint.minGlobalZ(); globalZ <= footprint.maxGlobalZ(); globalZ++) {
            if (collidesWithBodyCell(node, globalX, globalZ, blockY, minY, maxY)) {
                return true;
            }
        }
        return false;
    }

    private boolean collidesWithBodyCell(
            SurfaceNode node, int globalX, int globalZ, int blockY, double minY, double maxY) {
        int blockX = blockCoordinate(globalX);
        int blockZ = blockCoordinate(globalZ);
        BlockPosition support = node.blockPosition();
        if (blockY == support.y() && blockX == support.x() && blockZ == support.z()) {
            return false;
        }
        SurfaceBlock block = surfaceBlock(blockX, blockY, blockZ);
        double localMinY = clamp(minY - blockY);
        double localMaxY = clamp(maxY - blockY);
        if (localMaxY <= 0.0 || localMinY >= 1.0) {
            return false;
        }
        return block.shape().collidesWithCellBody(
                cellCoordinate(globalX), cellCoordinate(globalZ), localMinY, localMaxY);
    }

    private boolean insideBounds(SurfaceNode node) {
        return bounds.contains(node.blockPosition());
    }

    private double movementCost(SurfaceNode from, SurfaceNode to) {
        double horizontalCost = horizontalCost(from, to);
        double climbCost = climbCost(to.floorY() - from.floorY());
        double clearanceCost = clearanceCost(to);
        return horizontalCost + climbCost + clearanceCost;
    }

    private double clearanceCost(SurfaceNode node) {
        if (!clearanceScorer.isEnabled()) {
            return 0.0;
        }
        int index = nodeIndex.indexOf(node);
        if (clearanceScoreLoaded[index]) {
            return clearanceScores[index];
        }
        double score = clearanceScorer.score(this, node);
        clearanceScores[index] = score;
        clearanceScoreLoaded[index] = true;
        return score;
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
        return movementEvaluator.decision(from, to, block, direction).allowed();
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

    private static SurfaceNode nearestOf(SurfaceNode current, SurfaceNode candidate, double floorY) {
        if (candidate == null) {
            return current;
        }
        if (current == null || floorDistance(candidate, floorY) < floorDistance(current, floorY)) {
            return candidate;
        }
        return current;
    }

    private static double floorDistance(SurfaceNode node, double floorY) {
        return Math.abs(node.floorY() - floorY);
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

package dev.traveler.core.world.navigation;

import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.api.FluidSemantics;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.EntityDimensions;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.movement.MovementProfiles;
import dev.traveler.core.world.movement.TraversalRules;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.KeyedGraph;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SurfaceTraversalGraph implements KeyedGraph<SurfaceNode>, SurfaceTraversalContext {
    private static final double BODY_EPSILON = 0.0001;
    private static final double FLOOR_EPSILON = 0.001;
    private static final int SPECIAL_CONNECTIONS_PER_NODE = 64;
    private static final int MAX_CONNECTIONS_PER_NODE =
            SurfaceConnectionDirections.EIGHT_WAY.size() * SurfaceSearchOffsets.SUPPORT_Y.length
                    + SPECIAL_CONNECTIONS_PER_NODE;

    private final SurfaceWorldLayer worldLayer;
    private final SearchBounds bounds;
    private final EntityDimensions dimensions;
    private final MovementCapabilities capabilities;
    private final TraversalRules rules;
    private final SurfaceClearanceScorer clearanceScorer;
    private final SurfaceBodyClearanceMode bodyClearanceMode;
    private final SurfaceTransitionEvaluator transitionEvaluator;
    private final List<SurfaceConnectionProvider> connectionProviders;
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
        this.rules = profile.rules();
        this.clearanceScorer = safeSettings.clearanceScorer();
        this.bodyClearanceMode = safeSettings.bodyClearanceMode();
        this.transitionEvaluator = new SurfaceTransitionEvaluator(capabilities, safeSettings.transitionResolver());
        this.connectionProviders = safeSettings.connectionProviders();
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
        if (!insideBounds(node) || !canUseAsExpansionOrigin(node)) {
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
        for (SurfaceConnectionProvider provider : connectionProviders) {
            provider.addConnections(this, node, connections);
        }
        return connections;
    }

    @Override
    public boolean canReach(SurfaceNode from, SurfaceNode to, MovementDirection direction) {
        SurfaceBlock block = surfaceBlock(to.blockPosition());
        return insideBounds(to)
                && canUseVerticalTransition(from, to)
                && hasBodyClearance(to)
                && destinationAllowsMovement(from, to, block, direction)
                && canUseDirection(from, to, direction);
    }

    @Override
    public boolean canReachDrop(SurfaceNode from, SurfaceNode to, MovementDirection direction) {
        SurfaceBlock block = surfaceBlock(to.blockPosition());
        return insideBounds(to)
                && rules.allowVertical()
                && isDropDown(from, to)
                && hasBodyClearance(to)
                && destinationAllowsMovement(from, to, block, direction);
    }

    private boolean isDropDown(SurfaceNode from, SurfaceNode to) {
        double delta = from.floorY() - to.floorY();
        return delta > capabilities.maxStepUp() + FLOOR_EPSILON
                && delta <= capabilities.maxSafeFallDistance() + FLOOR_EPSILON;
    }

    @Override
    public boolean canReachJump(SurfaceNode from, SurfaceNode to, MovementDirection direction) {
        SurfaceBlock block = surfaceBlock(to.blockPosition());
        return insideBounds(to)
                && rules.allowVertical()
                && !isSwimNode(from)
                && isJumpUp(from, to)
                && hasBodyClearance(to)
                && destinationAllowsMovement(from, to, block, direction);
    }

    @Override
    public boolean canReachClimb(SurfaceNode from, SurfaceNode to) {
        return insideBounds(to)
                && rules.allowVertical()
                && hasBodyClearance(to)
                && SurfaceClimbTraversal.canClimbWithLookup(surfaceBlocks::get, from, to, capabilities)
                && hasClimbTargetClearance(from, to);
    }

    @Override
    public boolean canReachSwim(SurfaceNode from, SurfaceNode to) {
        return insideBounds(to)
                && rules.allowVertical()
                && capabilities.canSwim()
                && sameHorizontalCell(from, to)
                && to.floorY() > from.floorY() + FLOOR_EPSILON
                && isSwimNode(from)
                && isSwimNode(to)
                && hasBodyClearance(to);
    }

    private boolean hasClimbTargetClearance(SurfaceNode from, SurfaceNode to) {
        List<SurfaceNode> climbNodes = SurfaceClimbTraversal.climbRouteNodes(worldLayer, from, to, capabilities)
                .orElseGet(List::of);
        SurfaceNode current = from;
        for (SurfaceNode climbNode : climbNodes) {
            if (!hasClimbStepTargetClearance(current, climbNode)) {
                return false;
            }
            current = climbNode;
        }
        return hasClimbStepTargetClearance(current, to);
    }

    private boolean hasClimbStepTargetClearance(SurfaceNode from, SurfaceNode to) {
        if (!SurfaceClimbTraversal.isClimbable(surfaceBlock(to.blockPosition()), capabilities)) {
            return true;
        }
        return SurfaceClimbTraversal.climbFaceTarget(worldLayer, from, to, capabilities)
                .filter(target -> SurfaceBodyClearance.hasClearance(worldLayer, target, dimensions))
                .isPresent();
    }

    @Override
    public boolean hasClimbableAtGlobalCell(int globalX, int blockY, int globalZ) {
        if (!bounds.contains(blockCoordinate(globalX), blockY, blockCoordinate(globalZ))) {
            return false;
        }
        SurfaceBlock block = surfaceBlock(blockCoordinate(globalX), blockY, blockCoordinate(globalZ));
        return SurfaceClimbTraversal.isClimbable(block, capabilities);
    }

    @Override
    public List<SurfaceNode> climbNodesAt(int blockX, int blockY, int blockZ) {
        if (!bounds.contains(blockX, blockY, blockZ)) {
            return List.of();
        }
        return SurfaceClimbTraversal.climbStartNodes(
                worldLayer,
                new BlockPosition(blockX, blockY, blockZ),
                capabilities);
    }

    private boolean isJumpUp(SurfaceNode from, SurfaceNode to) {
        double delta = to.floorY() - from.floorY();
        return delta > capabilities.maxStepUp() + FLOOR_EPSILON
                && delta <= capabilities.maxJumpHeight() + FLOOR_EPSILON;
    }

    private boolean canUseDirection(SurfaceNode from, SurfaceNode to, MovementDirection direction) {
        if (!direction.isDiagonal()) {
            return true;
        }
        if (!rules.allowDiagonal()) {
            return false;
        }
        if (isJumpUp(from, to) && !rules.allowDiagonalJump()) {
            return false;
        }
        return canMoveDiagonally(from, to, direction);
    }

    private boolean canMoveDiagonally(SurfaceNode from, SurfaceNode to, MovementDirection direction) {
        int originX = globalX(from);
        int originZ = globalZ(from);
        return hasReachableSide(from, originX + direction.x(), originZ, to.floorY())
                && hasReachableSide(from, originX, originZ + direction.z(), to.floorY());
    }

    private boolean hasReachableSide(SurfaceNode from, int globalX, int globalZ, double targetFloorY) {
        for (int yOffset : SurfaceSearchOffsets.SUPPORT_Y) {
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
        if (!bounds.contains(blockX, blockY, blockZ)) {
            return null;
        }
        int cellX = cellCoordinate(globalX);
        int cellZ = cellCoordinate(globalZ);
        SurfaceBlock block = surfaceBlock(blockX, blockY, blockZ);
        double floorHeight = block.shape().floorHeightForCellOrNaN(cellX, cellZ);
        if (Double.isNaN(floorHeight)) {
            return capabilities.canSwim()
                    ? swimNode(block, blockX, blockY, blockZ, cellX, cellZ)
                    : null;
        }
        if (!restingSemantics(block, blockX, blockY, blockZ, cellX, cellZ, blockY + floorHeight)
                .support()
                .supportsStanding()) {
            return null;
        }
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        return new SurfaceNode(position, cellX, cellZ, blockY + floorHeight);
    }

    private SurfaceNode swimNode(
            SurfaceBlock block,
            int blockX,
            int blockY,
            int blockZ,
            int cellX,
            int cellZ) {
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        if (!hasFluid(block, position, cellX, cellZ)) {
            return null;
        }
        return new SurfaceNode(position, cellX, cellZ, blockY + 1.0);
    }

    private boolean hasFluid(SurfaceBlock block, BlockPosition position, int cellX, int cellZ) {
        return restingSemantics(block, position.x(), position.y(), position.z(), cellX, cellZ, position.y() + 1.0)
                        .fluid()
                == FluidSemantics.SWIMMABLE;
    }

    private BlockSemantics restingSemantics(
            SurfaceBlock block,
            int blockX,
            int blockY,
            int blockZ,
            int cellX,
            int cellZ,
            double floorY) {
        BlockPosition position = new BlockPosition(blockX, blockY, blockZ);
        SurfaceNode node = new SurfaceNode(position, cellX, cellZ, floorY);
        SurfaceMovementContext context =
                new SurfaceMovementContext(node, node, block, block, capabilities, MovementDirection.north());
        return block.behavior().describe(context);
    }

    @Override
    public SurfaceNode surfaceNodeAt(int globalX, int blockY, int globalZ) {
        return surfaceNode(globalX, blockY, globalZ);
    }

    SurfaceNode nearestSurfaceAt(int globalX, int globalZ, double floorY) {
        int baseY = (int) Math.floor(floorY);
        SurfaceNode nearest = null;
        for (int yOffset : SurfaceSearchOffsets.SUPPORT_Y) {
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

    private boolean canUseAsExpansionOrigin(SurfaceNode node) {
        return canStandOn(node) || canOccupyClimbNode(node) || canOccupySwimNode(node);
    }

    private boolean canOccupySwimNode(SurfaceNode node) {
        return isSwimNode(node) && hasBodyClearance(node);
    }

    private boolean canOccupyClimbNode(SurfaceNode node) {
        return SurfaceClimbTraversal.climbStartTarget(worldLayer, node, capabilities)
                .filter(target -> SurfaceBodyClearance.hasClearance(worldLayer, target, dimensions))
                .isPresent();
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
        for (int y = (int) Math.floor(minY) - 1; y <= (int) Math.floor(maxY); y++) {
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
        double localMinY = minY - blockY;
        double localMaxY = maxY - blockY;
        if (localMaxY <= 0.0) {
            return false;
        }
        return block.shape().collidesWithCellBody(
                cellCoordinate(globalX), cellCoordinate(globalZ), localMinY, localMaxY);
    }

    private boolean insideBounds(SurfaceNode node) {
        return bounds.contains(node.blockPosition());
    }

    @Override
    public double movementCost(SurfaceNode from, SurfaceNode to) {
        double horizontalCost = horizontalCost(from, to);
        double climbCost = climbCost(to.floorY() - from.floorY());
        double clearanceCost = clearanceCost(to);
        return (horizontalCost + climbCost) * rules.defaultCost().value() + clearanceCost;
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
            MovementDirection direction) {
        return transitionEvaluator.decision(worldLayer, from, to, block, direction).allowed();
    }

    private boolean canUseVerticalTransition(SurfaceNode from, SurfaceNode to) {
        return rules.allowVertical() || sameFloor(from, to);
    }

    private double upwardClearance() {
        return Math.max(capabilities.maxStepUp(), capabilities.maxJumpHeight());
    }

    private static MovementDirection directionBetween(SurfaceNode from, SurfaceNode to) {
        int xOffset = Double.compare(to.centerX(), from.centerX());
        int zOffset = Double.compare(to.centerZ(), from.centerZ());
        return MovementDirection.fromOffset(xOffset, zOffset);
    }

    @Override
    public MovementCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public int globalXOf(SurfaceNode node) {
        return globalX(node);
    }

    @Override
    public int globalZOf(SurfaceNode node) {
        return globalZ(node);
    }

    @Override
    public int minBlockY() {
        return bounds.minY();
    }

    @Override
    public int maxBlockY() {
        return bounds.maxY();
    }

    private static boolean sameFloor(SurfaceNode first, SurfaceNode second) {
        return Math.abs(first.floorY() - second.floorY()) <= FLOOR_EPSILON;
    }

    private boolean isSwimNode(SurfaceNode node) {
        return capabilities.canSwim()
                && Math.abs(node.floorY() - (node.blockPosition().y() + 1.0)) <= FLOOR_EPSILON
                && hasFluid(
                        surfaceBlock(node.blockPosition()),
                        node.blockPosition(),
                        node.cellX(),
                        node.cellZ());
    }

    private static boolean sameHorizontalCell(SurfaceNode first, SurfaceNode second) {
        return first.blockPosition().x() == second.blockPosition().x()
                && first.blockPosition().z() == second.blockPosition().z()
                && first.cellX() == second.cellX()
                && first.cellZ() == second.cellZ();
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

package dev.traveler.core.world.navigation;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.WorldLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BlockTraversalGraph implements Graph<BlockPosition> {
    private static final int[] STEP_OFFSETS = {0, 1, -1};
    private static final int MAX_CONNECTIONS_PER_NODE = HorizontalDirections.EIGHT_WAY.length;

    private final WorldLayer worldLayer;
    private final SearchBounds bounds;

    public BlockTraversalGraph(
            WorldLayer worldLayer,
            BlockPosition origin,
            BlockPosition target,
            int horizontalMargin,
            int verticalMargin) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
        BlockPosition safeStart = Objects.requireNonNull(origin, "origin");
        BlockPosition safeGoal = Objects.requireNonNull(target, "target");
        this.bounds = SearchBounds.around(safeStart, safeGoal, horizontalMargin, verticalMargin);
    }

    @Override
    public Iterable<Connection<BlockPosition>> outgoingConnections(BlockPosition node) {
        Objects.requireNonNull(node, "node");
        if (!insideBounds(node)) {
            return List.of();
        }
        return connectionsFrom(node);
    }

    private List<Connection<BlockPosition>> connectionsFrom(BlockPosition node) {
        List<Connection<BlockPosition>> connections = new ArrayList<>(MAX_CONNECTIONS_PER_NODE);
        for (HorizontalOffset direction : HorizontalDirections.EIGHT_WAY) {
            addDirectionConnections(node, direction, connections);
        }
        return connections;
    }

    private void addDirectionConnections(
            BlockPosition node,
            HorizontalOffset direction,
            List<Connection<BlockPosition>> connections) {
        for (int yOffset : STEP_OFFSETS) {
            BlockPosition candidate = node.offset(direction.x(), yOffset, direction.z());
            if (canReach(node, candidate, direction)) {
                connections.add(new Connection<>(node, candidate, movementCost(node, candidate)));
                return;
            }
        }
    }

    private boolean canReach(BlockPosition from, BlockPosition to, HorizontalOffset direction) {
        return isReachable(to) && canUseDirection(from, to, direction);
    }

    private boolean canUseDirection(BlockPosition from, BlockPosition to, HorizontalOffset direction) {
        if (!direction.isDiagonal()) {
            return true;
        }
        return canMoveDiagonally(from, to);
    }

    private boolean canMoveDiagonally(BlockPosition from, BlockPosition to) {
        int yOffset = to.y() - from.y();
        int xOffset = Integer.compare(to.x(), from.x());
        int zOffset = Integer.compare(to.z(), from.z());
        return isReachable(from.offset(xOffset, yOffset, 0)) && isReachable(from.offset(0, yOffset, zOffset));
    }

    private boolean isReachable(BlockPosition position) {
        return insideBounds(position) && BlockStandability.isStandable(worldLayer, position);
    }

    private boolean insideBounds(BlockPosition position) {
        return bounds.contains(position);
    }

    private static double movementCost(BlockPosition from, BlockPosition to) {
        double horizontalCost = isDiagonal(from, to) ? Math.sqrt(2.0) : 1.0;
        return horizontalCost + Math.abs(to.y() - from.y()) * 0.5;
    }

    private static boolean isDiagonal(BlockPosition from, BlockPosition to) {
        return from.x() != to.x() && from.z() != to.z();
    }

}

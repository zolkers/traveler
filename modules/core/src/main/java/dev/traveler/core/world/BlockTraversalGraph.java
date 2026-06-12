package dev.traveler.core.world;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.WorldLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BlockTraversalGraph implements Graph<BlockPosition> {
    private static final int[][] CARDINAL_DIRECTIONS = {
        {1, 0},
        {-1, 0},
        {0, 1},
        {0, -1}
    };
    private static final int[] STEP_OFFSETS = {0, 1, -1};

    private final WorldLayer worldLayer;
    private final BlockPosition start;
    private final BlockPosition goal;
    private final int horizontalMargin;
    private final int verticalMargin;

    public BlockTraversalGraph(
            WorldLayer worldLayer,
            BlockPosition start,
            BlockPosition goal,
            int horizontalMargin,
            int verticalMargin) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
        this.start = Objects.requireNonNull(start, "start");
        this.goal = Objects.requireNonNull(goal, "goal");
        this.horizontalMargin = requirePositive(horizontalMargin, "horizontalMargin");
        this.verticalMargin = requirePositive(verticalMargin, "verticalMargin");
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
        List<Connection<BlockPosition>> connections = new ArrayList<>();
        for (int[] direction : CARDINAL_DIRECTIONS) {
            addDirectionConnections(node, direction, connections);
        }
        return connections;
    }

    private void addDirectionConnections(
            BlockPosition node,
            int[] direction,
            List<Connection<BlockPosition>> connections) {
        for (int yOffset : STEP_OFFSETS) {
            BlockPosition candidate = node.offset(direction[0], yOffset, direction[1]);
            if (isReachable(candidate)) {
                connections.add(new Connection<>(node, candidate, movementCost(node, candidate)));
                return;
            }
        }
    }

    private boolean isReachable(BlockPosition position) {
        return insideBounds(position) && isStandable(position);
    }

    private boolean isStandable(BlockPosition position) {
        return isBodyPassable(position) && supportsEntity(position.below());
    }

    private boolean isBodyPassable(BlockPosition position) {
        return passable(position) && passable(position.above());
    }

    private boolean supportsEntity(BlockPosition position) {
        BlockPassability passability = classification(position).passability();
        return passability == BlockPassability.SOLID || passability == BlockPassability.WALKABLE;
    }

    private boolean passable(BlockPosition position) {
        BlockClassification classification = classification(position);
        return classification.passability() == BlockPassability.PASSABLE
                || classification.passability() == BlockPassability.WALKABLE;
    }

    private BlockClassification classification(BlockPosition position) {
        return worldLayer.classify(position);
    }

    private boolean insideBounds(BlockPosition position) {
        return insideAxis(position.x(), start.x(), goal.x(), horizontalMargin)
                && insideAxis(position.z(), start.z(), goal.z(), horizontalMargin)
                && insideAxis(position.y(), start.y(), goal.y(), verticalMargin);
    }

    private static boolean insideAxis(int value, int first, int second, int margin) {
        return value >= Math.min(first, second) - margin && value <= Math.max(first, second) + margin;
    }

    private static double movementCost(BlockPosition from, BlockPosition to) {
        return 1.0 + Math.abs(to.y() - from.y()) * 0.5;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}

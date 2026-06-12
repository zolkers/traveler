package dev.traveler.core.render;

import dev.traveler.core.debug.PathfinderDebugSnapshot;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.BlockPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PathDebugRenderModel {
    private static final ColorRgba DEFAULT_COLOR = new ColorRgba(0.1f, 0.75f, 1.0f, 0.9f);
    private static final double CENTER_OFFSET = 0.5;
    private static final double DEFAULT_NODE_HALF_SIZE = 0.25;

    private final ColorRgba pathColor;
    private final double yOffset;
    private final double nodeHalfSize;

    public PathDebugRenderModel(ColorRgba pathColor, double yOffset) {
        this(pathColor, yOffset, DEFAULT_NODE_HALF_SIZE);
    }

    public PathDebugRenderModel(ColorRgba pathColor, double yOffset, double nodeHalfSize) {
        this.pathColor = Objects.requireNonNull(pathColor, "pathColor");
        this.yOffset = yOffset;
        this.nodeHalfSize = requirePositive(nodeHalfSize, "nodeHalfSize");
    }

    public static PathDebugRenderModel defaultModel() {
        return new PathDebugRenderModel(DEFAULT_COLOR, 0.35);
    }

    public DebugRenderFrame frameFor(Optional<PathfinderDebugSnapshot> snapshot) {
        Optional<PathfinderDebugSnapshot> current = Objects.requireNonNull(snapshot, "snapshot");
        if (current.isEmpty()) {
            return DebugRenderFrame.empty();
        }
        return frameFor(current.orElseThrow().result());
    }

    private DebugRenderFrame frameFor(PathfinderResult<BlockPosition> result) {
        if (result.status() != PathfinderStatus.FOUND) {
            return DebugRenderFrame.empty();
        }
        if (result.path().nodeCount() < 2) {
            return DebugRenderFrame.empty();
        }
        return new DebugRenderFrame(linesFor(result.path()));
    }

    private List<DebugLine> linesFor(GraphPath<BlockPosition> path) {
        List<DebugLine> lines = new ArrayList<>();
        addPathSegments(path, lines);
        addNodeSquares(path, lines);
        return lines;
    }

    private void addPathSegments(GraphPath<BlockPosition> path, List<DebugLine> lines) {
        for (int index = 1; index < path.nodeCount(); index++) {
            addStepAwareSegment(lines, vertexFor(path.nodeAt(index - 1)), vertexFor(path.nodeAt(index)));
        }
    }

    private void addStepAwareSegment(List<DebugLine> lines, RenderVertex from, RenderVertex to) {
        if (Double.compare(from.y(), to.y()) == 0) {
            addLine(lines, from, to);
            return;
        }
        if (to.y() > from.y()) {
            RenderVertex raisedFrom = new RenderVertex(from.x(), to.y(), from.z());
            addLine(lines, from, raisedFrom);
            addLine(lines, raisedFrom, to);
            return;
        }
        RenderVertex horizontalTo = new RenderVertex(to.x(), from.y(), to.z());
        addLine(lines, from, horizontalTo);
        addLine(lines, horizontalTo, to);
    }

    private void addNodeSquares(GraphPath<BlockPosition> path, List<DebugLine> lines) {
        for (BlockPosition node : path) {
            addNodeSquare(lines, vertexFor(node));
        }
    }

    private void addNodeSquare(List<DebugLine> lines, RenderVertex center) {
        RenderVertex northWest = squareVertex(center, -nodeHalfSize, -nodeHalfSize);
        RenderVertex northEast = squareVertex(center, nodeHalfSize, -nodeHalfSize);
        RenderVertex southEast = squareVertex(center, nodeHalfSize, nodeHalfSize);
        RenderVertex southWest = squareVertex(center, -nodeHalfSize, nodeHalfSize);
        addLine(lines, northWest, northEast);
        addLine(lines, northEast, southEast);
        addLine(lines, southEast, southWest);
        addLine(lines, southWest, northWest);
    }

    private RenderVertex squareVertex(RenderVertex center, double xOffset, double zOffset) {
        return new RenderVertex(center.x() + xOffset, center.y(), center.z() + zOffset);
    }

    private void addLine(List<DebugLine> lines, RenderVertex from, RenderVertex to) {
        if (from.equals(to)) {
            return;
        }
        lines.add(new DebugLine(from, to, pathColor));
    }

    private RenderVertex vertexFor(BlockPosition position) {
        return new RenderVertex(
                position.x() + CENTER_OFFSET, position.y() + yOffset, position.z() + CENTER_OFFSET);
    }

    private static double requirePositive(double value, String name) {
        if (value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}

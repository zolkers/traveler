package dev.traveler.core.render;

import dev.traveler.core.debug.PathfinderDebugSnapshot;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PathDebugRenderModel {
    private static final ColorRgba DEFAULT_COLOR = new ColorRgba(0.1f, 0.75f, 1.0f, 0.9f);
    private static final double CENTER_OFFSET = 0.5;
    private static final double SURFACE_Y_OFFSET = 0.08;
    private static final float DEFAULT_NODE_ALPHA = 0.22f;

    private final ColorRgba pathColor;
    private final ColorRgba nodeColor;
    private final double yOffset;

    public PathDebugRenderModel(ColorRgba pathColor, double yOffset) {
        this(pathColor, yOffset, transparentNodeColor(pathColor));
    }

    public PathDebugRenderModel(ColorRgba pathColor, double yOffset, ColorRgba nodeColor) {
        this.pathColor = Objects.requireNonNull(pathColor, "pathColor");
        this.nodeColor = Objects.requireNonNull(nodeColor, "nodeColor");
        this.yOffset = yOffset;
    }

    public static PathDebugRenderModel defaultModel() {
        return new PathDebugRenderModel(DEFAULT_COLOR, 0.35);
    }

    public DebugRenderFrame frameFor(Optional<PathfinderDebugSnapshot> snapshot) {
        Optional<PathfinderDebugSnapshot> current = Objects.requireNonNull(snapshot, "snapshot");
        if (current.isEmpty()) {
            return DebugRenderFrame.empty();
        }
        return frameFor(current.orElseThrow());
    }

    private DebugRenderFrame frameFor(PathfinderDebugSnapshot snapshot) {
        if (snapshot.result().status() != PathfinderStatus.FOUND) {
            return DebugRenderFrame.empty();
        }
        if (snapshot.hasSurfaceNodes()) {
            return surfaceFrameFor(snapshot.surfaceNodes());
        }
        return frameFor(snapshot.result());
    }

    private DebugRenderFrame frameFor(PathfinderResult<BlockPosition> result) {
        if (result.status() != PathfinderStatus.FOUND) {
            return DebugRenderFrame.empty();
        }
        if (result.path().nodeCount() < 2) {
            return DebugRenderFrame.empty();
        }
        return new DebugRenderFrame(linesFor(result.path()), boxesFor(result.path()));
    }

    private DebugRenderFrame surfaceFrameFor(List<SurfaceNode> nodes) {
        if (nodes.size() < 2) {
            return DebugRenderFrame.empty();
        }
        return new DebugRenderFrame(surfaceLinesFor(nodes), surfaceBoxesFor(nodes));
    }

    private List<DebugLine> linesFor(GraphPath<BlockPosition> path) {
        List<DebugLine> lines = new ArrayList<>();
        addPathSegments(path, lines);
        return lines;
    }

    private List<DebugBox> boxesFor(GraphPath<BlockPosition> path) {
        List<DebugBox> boxes = new ArrayList<>();
        for (BlockPosition node : path) {
            boxes.add(boxFor(node));
        }
        return boxes;
    }

    private List<DebugLine> surfaceLinesFor(List<SurfaceNode> nodes) {
        List<DebugLine> lines = new ArrayList<>();
        for (int index = 1; index < nodes.size(); index++) {
            addLine(lines, surfaceVertexFor(nodes.get(index - 1)), surfaceVertexFor(nodes.get(index)));
        }
        return lines;
    }

    private List<DebugBox> surfaceBoxesFor(List<SurfaceNode> nodes) {
        List<DebugBox> boxes = new ArrayList<>();
        for (BlockPosition position : visualBlocks(nodes)) {
            boxes.add(boxFor(position));
        }
        return boxes;
    }

    private static Set<BlockPosition> visualBlocks(List<SurfaceNode> nodes) {
        Set<BlockPosition> positions = new LinkedHashSet<>();
        for (SurfaceNode node : nodes) {
            positions.add(node.renderBlockPosition());
        }
        return positions;
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

    private void addLine(List<DebugLine> lines, RenderVertex from, RenderVertex to) {
        if (from.equals(to)) {
            return;
        }
        lines.add(new DebugLine(from, to, pathColor));
    }

    private DebugBox boxFor(BlockPosition position) {
        RenderVertex min = new RenderVertex(position.x(), position.y(), position.z());
        RenderVertex max = new RenderVertex(position.x() + 1.0, position.y() + 1.0, position.z() + 1.0);
        return new DebugBox(min, max, nodeColor);
    }

    private RenderVertex vertexFor(BlockPosition position) {
        return new RenderVertex(
                position.x() + CENTER_OFFSET, position.y() + yOffset, position.z() + CENTER_OFFSET);
    }

    private RenderVertex surfaceVertexFor(SurfaceNode node) {
        return new RenderVertex(node.centerX(), node.floorY() + SURFACE_Y_OFFSET, node.centerZ());
    }

    private static ColorRgba transparentNodeColor(ColorRgba color) {
        Objects.requireNonNull(color, "color");
        return new ColorRgba(color.red(), color.green(), color.blue(), DEFAULT_NODE_ALPHA);
    }
}

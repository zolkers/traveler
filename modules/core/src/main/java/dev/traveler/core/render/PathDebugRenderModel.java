package dev.traveler.core.render;

import dev.traveler.core.debug.snapshots.NavigationDebugSnapshot;
import dev.traveler.core.debug.snapshots.PathfinderDebugSnapshot;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.graph.GraphPath;
import dev.traveler.core.navigation.spatial.NavigationPoint;
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
    private static final double DEFAULT_LINE_THICKNESS = 0.08;
    private static final float DEFAULT_NODE_ALPHA = 0.22f;
    private static final double TARGET_BOX_RADIUS = 0.25;
    private static final ColorRgba TARGET_COLOR = new ColorRgba(1.0f, 0.85f, 0.15f, 0.85f);

    private final ColorRgba pathColor;
    private final ColorRgba nodeColor;
    private final double yOffset;
    private final double lineThickness;

    public PathDebugRenderModel(ColorRgba pathColor, double yOffset) {
        this(pathColor, yOffset, DEFAULT_LINE_THICKNESS);
    }

    public PathDebugRenderModel(ColorRgba pathColor, double yOffset, double lineThickness) {
        this(pathColor, yOffset, lineThickness, transparentNodeColor(pathColor));
    }

    public PathDebugRenderModel(ColorRgba pathColor, double yOffset, ColorRgba nodeColor) {
        this(pathColor, yOffset, DEFAULT_LINE_THICKNESS, nodeColor);
    }

    public PathDebugRenderModel(
            ColorRgba pathColor,
            double yOffset,
            double lineThickness,
            ColorRgba nodeColor) {
        this.pathColor = Objects.requireNonNull(pathColor, "pathColor");
        this.nodeColor = Objects.requireNonNull(nodeColor, "nodeColor");
        requireFinite(yOffset, "yOffset");
        requirePositive(lineThickness, "lineThickness");
        this.yOffset = yOffset;
        this.lineThickness = lineThickness;
    }

    public static PathDebugRenderModel defaultModel() {
        return new PathDebugRenderModel(DEFAULT_COLOR, CENTER_OFFSET, DEFAULT_LINE_THICKNESS);
    }

    public DebugRenderFrame frameFor(PathfinderDebugState state) {
        PathfinderDebugState debugState = Objects.requireNonNull(state, "state");
        return combine(frameFor(debugState.snapshot()), navigationFrameFor(debugState.latestNavigation()));
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
        if (snapshot.hasRoutePoints()) {
            return routeFrameFor(snapshot.routePoints(), snapshot.surfaceNodes());
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

    private DebugRenderFrame routeFrameFor(List<NavigationPoint> points, List<SurfaceNode> surfaceNodes) {
        if (points.size() < 2) {
            return DebugRenderFrame.empty();
        }
        return new DebugRenderFrame(routeLinesFor(points), surfaceBoxesFor(surfaceNodes));
    }

    private DebugRenderFrame navigationFrameFor(Optional<NavigationDebugSnapshot> snapshot) {
        Optional<NavigationDebugSnapshot> current = Objects.requireNonNull(snapshot, "snapshot");
        if (current.isEmpty()) {
            return DebugRenderFrame.empty();
        }
        return navigationFrameFor(current.orElseThrow());
    }

    private DebugRenderFrame navigationFrameFor(NavigationDebugSnapshot snapshot) {
        return new DebugRenderFrame(List.of(), List.of(targetBoxFor(snapshot)));
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

    private List<DebugLine> routeLinesFor(List<NavigationPoint> points) {
        List<DebugLine> lines = new ArrayList<>();
        for (int index = 1; index < points.size(); index++) {
            addLine(lines, routeVertexFor(points.get(index - 1)), routeVertexFor(points.get(index)));
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
        lines.add(new DebugLine(from, to, pathColor, lineThickness));
    }

    private DebugBox boxFor(BlockPosition position) {
        RenderVertex min = new RenderVertex(position.x(), position.y(), position.z());
        RenderVertex max = new RenderVertex(position.x() + 1.0, position.y() + 1.0, position.z() + 1.0);
        return new DebugBox(min, max, nodeColor);
    }

    private static DebugBox targetBoxFor(NavigationDebugSnapshot snapshot) {
        NavigationPoint target = Objects.requireNonNull(snapshot, "snapshot").movementTarget();
        RenderVertex min = new RenderVertex(
                target.x() - TARGET_BOX_RADIUS,
                target.y() - TARGET_BOX_RADIUS,
                target.z() - TARGET_BOX_RADIUS);
        RenderVertex max = new RenderVertex(
                target.x() + TARGET_BOX_RADIUS,
                target.y() + TARGET_BOX_RADIUS,
                target.z() + TARGET_BOX_RADIUS);
        return new DebugBox(min, max, TARGET_COLOR);
    }

    private RenderVertex vertexFor(BlockPosition position) {
        return new RenderVertex(
                position.x() + CENTER_OFFSET, position.y() + yOffset, position.z() + CENTER_OFFSET);
    }

    private RenderVertex surfaceVertexFor(SurfaceNode node) {
        return new RenderVertex(node.centerX(), node.floorY() + yOffset, node.centerZ());
    }

    private RenderVertex routeVertexFor(NavigationPoint point) {
        return new RenderVertex(point.x(), point.y() + yOffset, point.z());
    }

    private static DebugRenderFrame combine(DebugRenderFrame first, DebugRenderFrame second) {
        List<DebugLine> lines = new ArrayList<>(first.lines());
        List<DebugBox> boxes = new ArrayList<>(first.boxes());
        lines.addAll(second.lines());
        boxes.addAll(second.boxes());
        return new DebugRenderFrame(lines, boxes);
    }

    private static ColorRgba transparentNodeColor(ColorRgba color) {
        Objects.requireNonNull(color, "color");
        return new ColorRgba(color.red(), color.green(), color.blue(), DEFAULT_NODE_ALPHA);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}

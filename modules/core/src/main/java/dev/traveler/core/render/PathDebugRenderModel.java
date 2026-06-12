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

    private final ColorRgba pathColor;
    private final double yOffset;

    public PathDebugRenderModel(ColorRgba pathColor, double yOffset) {
        this.pathColor = Objects.requireNonNull(pathColor, "pathColor");
        this.yOffset = yOffset;
    }

    public static PathDebugRenderModel defaultModel() {
        return new PathDebugRenderModel(DEFAULT_COLOR, 0.12);
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
        for (int index = 1; index < path.nodeCount(); index++) {
            lines.add(new DebugLine(vertexFor(path.nodeAt(index - 1)), vertexFor(path.nodeAt(index)), pathColor));
        }
        return lines;
    }

    private RenderVertex vertexFor(BlockPosition position) {
        return new RenderVertex(
                position.x() + CENTER_OFFSET, position.y() + yOffset, position.z() + CENTER_OFFSET);
    }
}

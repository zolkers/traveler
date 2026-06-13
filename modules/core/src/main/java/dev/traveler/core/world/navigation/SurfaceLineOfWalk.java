package dev.traveler.core.world.navigation;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.smooth.LineOfWalk;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Objects;

public final class SurfaceLineOfWalk implements LineOfWalk<SurfaceNode> {
    private static final double FLOOR_EPSILON = 0.001;

    private final SurfaceTraversalGraph graph;
    private final SurfaceLineOfWalkSettings settings;

    public SurfaceLineOfWalk(
            SurfaceWorldLayer worldLayer,
            SurfaceNode boundsStart,
            SurfaceNode boundsGoal,
            MovementCapabilities capabilities,
            int horizontalMargin,
            int verticalMargin) {
        this(
                worldLayer,
                boundsStart,
                boundsGoal,
                capabilities,
                SurfaceLineOfWalkSettings.standard(horizontalMargin, verticalMargin));
    }

    public SurfaceLineOfWalk(
            SurfaceWorldLayer worldLayer,
            SurfaceNode boundsStart,
            SurfaceNode boundsGoal,
            MovementCapabilities capabilities,
            SurfaceLineOfWalkSettings settings) {
        SurfaceLineOfWalkSettings safeSettings = Objects.requireNonNull(settings, "settings");
        this.settings = safeSettings;
        graph = new SurfaceTraversalGraph(
                worldLayer,
                boundsStart,
                boundsGoal,
                capabilities,
                safeSettings.horizontalMargin(),
                safeSettings.verticalMargin());
    }

    @Override
    public boolean hasLineOfWalk(SurfaceNode from, SurfaceNode to) {
        SurfaceNode start = Objects.requireNonNull(from, "from");
        SurfaceNode goal = Objects.requireNonNull(to, "to");
        return hasSampledLineOfWalk(graph, start, goal);
    }

    private boolean hasSampledLineOfWalk(SurfaceTraversalGraph graph, SurfaceNode from, SurfaceNode to) {
        int steps = horizontalSteps(from, to);
        SurfaceNode previous = from;
        for (int step = 1; step <= steps; step++) {
            SurfaceNode sample = sampleNode(graph, from, to, step, steps);
            if (!canUseSample(graph, previous, sample, step, steps)) {
                return false;
            }
            previous = sample;
        }
        return previous.sameSubcell(to);
    }

    private boolean canUseSample(
            SurfaceTraversalGraph graph,
            SurfaceNode previous,
            SurfaceNode sample,
            int step,
            int steps) {
        if (sample == null || !canWalkStep(graph, previous, sample)) {
            return false;
        }
        if (step == steps) {
            return true;
        }
        return hasAdjacentClearance(graph, sample);
    }

    private SurfaceNode sampleNode(
            SurfaceTraversalGraph graph,
            SurfaceNode from,
            SurfaceNode to,
            int step,
            int steps) {
        if (step == steps) {
            return to;
        }
        int globalX = interpolate(globalX(from), globalX(to), step, steps);
        int globalZ = interpolate(globalZ(from), globalZ(to), step, steps);
        double floorY = interpolateFloorY(from, to, step, steps);
        return graph.nearestSurfaceAt(globalX, globalZ, floorY);
    }

    private static boolean canWalkStep(SurfaceTraversalGraph graph, SurfaceNode from, SurfaceNode to) {
        if (from.sameSubcell(to)) {
            return true;
        }
        for (Connection<SurfaceNode> connection : graph.outgoingConnections(from)) {
            if (connection.to().sameSubcell(to)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdjacentClearance(SurfaceTraversalGraph graph, SurfaceNode sample) {
        if (!settings.requiresAdjacentClearance()) {
            return true;
        }
        for (HorizontalOffset direction : HorizontalDirections.CARDINAL) {
            if (!hasClearanceAt(graph, sample, direction)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasClearanceAt(SurfaceTraversalGraph graph, SurfaceNode sample, HorizontalOffset direction) {
        SurfaceNode adjacent = graph.nearestSurfaceAt(
                globalX(sample) + direction.x(),
                globalZ(sample) + direction.z(),
                sample.floorY());
        return adjacent != null && sameFloor(sample, adjacent) && graph.hasBodyClearanceAt(adjacent);
    }

    private static int horizontalSteps(SurfaceNode from, SurfaceNode to) {
        return Math.max(Math.abs(globalX(to) - globalX(from)), Math.abs(globalZ(to) - globalZ(from)));
    }

    private static int interpolate(int from, int to, int step, int steps) {
        double progress = (double) step / steps;
        return (int) Math.round(from + (to - from) * progress);
    }

    private static double interpolateFloorY(SurfaceNode from, SurfaceNode to, int step, int steps) {
        double progress = (double) step / steps;
        return from.floorY() + (to.floorY() - from.floorY()) * progress;
    }

    private static boolean sameFloor(SurfaceNode first, SurfaceNode second) {
        return Math.abs(first.floorY() - second.floorY()) <= FLOOR_EPSILON;
    }

    private static int globalX(SurfaceNode node) {
        return SurfaceTraversalGraph.globalX(node);
    }

    private static int globalZ(SurfaceNode node) {
        return SurfaceTraversalGraph.globalZ(node);
    }
}

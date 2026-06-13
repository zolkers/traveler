package dev.traveler.core.world.navigation;

import dev.traveler.core.world.surface.SurfaceNode;

public record SurfaceClearanceScorer(int radiusCells, double blockedCellPenalty) {
    private static final double FLOOR_EPSILON = 0.001;
    private static final SurfaceClearanceScorer DISABLED = new SurfaceClearanceScorer(0, 0.0);
    private static final SurfaceClearanceScorer STANDARD = new SurfaceClearanceScorer(2, 0.35);

    public SurfaceClearanceScorer {
        if (radiusCells < 0) {
            throw new IllegalArgumentException("radiusCells must be non-negative");
        }
        if (!(blockedCellPenalty >= 0.0) || !Double.isFinite(blockedCellPenalty)) {
            throw new IllegalArgumentException("blockedCellPenalty must be non-negative and finite");
        }
    }

    public static SurfaceClearanceScorer disabled() {
        return DISABLED;
    }

    public static SurfaceClearanceScorer standard() {
        return STANDARD;
    }

    boolean isEnabled() {
        return radiusCells > 0 && blockedCellPenalty > 0.0;
    }

    double score(SurfaceTraversalGraph graph, SurfaceNode node) {
        if (!isEnabled()) {
            return 0.0;
        }
        double score = 0.0;
        for (HorizontalOffset direction : HorizontalDirections.EIGHT_WAY) {
            score += directionPenalty(graph, node, direction);
        }
        return score;
    }

    private double directionPenalty(SurfaceTraversalGraph graph, SurfaceNode node, HorizontalOffset direction) {
        for (int distance = 1; distance <= radiusCells; distance++) {
            if (!hasClearance(graph, node, direction, distance)) {
                return blockedCellPenalty / distance;
            }
        }
        return 0.0;
    }

    private boolean hasClearance(
            SurfaceTraversalGraph graph,
            SurfaceNode node,
            HorizontalOffset direction,
            int distance) {
        SurfaceNode adjacent = graph.nearestSurfaceAt(
                SurfaceTraversalGraph.globalX(node) + direction.x() * distance,
                SurfaceTraversalGraph.globalZ(node) + direction.z() * distance,
                node.floorY());
        return adjacent != null && sameFloor(node, adjacent) && graph.hasBodyClearanceAt(adjacent);
    }

    private static boolean sameFloor(SurfaceNode first, SurfaceNode second) {
        return Math.abs(first.floorY() - second.floorY()) <= FLOOR_EPSILON;
    }
}

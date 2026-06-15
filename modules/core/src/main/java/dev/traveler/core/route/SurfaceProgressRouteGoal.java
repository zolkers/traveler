package dev.traveler.core.route;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;

/**
 * A route goal that can accept the best reachable surface node making progress
 * when its exact sampled goal surfaces are unavailable or blocked.
 */
public interface SurfaceProgressRouteGoal extends RouteGoal {
    /**
     * Returns the synthetic surface node used only to bound the local graph
     * exploration toward the desired frontier.
     */
    SurfaceNode progressAnchor(BlockPosition start);

    /**
     * Returns true when the reachable node is useful enough to become the next
     * local frontier.
     */
    boolean isProgressCandidate(SurfaceNode node, BlockPosition start);

    /**
     * Scores the reachable node from the current segment start. Higher scores
     * mean better progress toward the final goal.
     */
    double progressScore(SurfaceNode node, BlockPosition start);
}

package dev.traveler.core.navigation.internal;

import dev.traveler.core.navigation.api.Traversal;
import dev.traveler.core.navigation.api.TraversalGeometry;
import dev.traveler.core.navigation.api.TraversalKind;
import dev.traveler.core.route.api.RouteSegment;
import dev.traveler.core.route.api.RouteTraversalHint;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;

public final class DefaultTraversalPlanner {
    private static final double DEFAULT_LATERAL_TOLERANCE = 0.35;
    private static final double DEFAULT_YAW_TOLERANCE = 35.0;

    public Traversal traversalFor(RouteSegment segment) {
        RouteSegment routeSegment = Objects.requireNonNull(segment, "segment");
        WorldPoint start = blockCenter(routeSegment.start());
        WorldPoint end = blockCenter(routeSegment.end());
        return new PlannedTraversal(
                traversalKind(routeSegment.traversalHint()),
                new TraversalGeometry(start, end, end, DEFAULT_LATERAL_TOLERANCE, DEFAULT_YAW_TOLERANCE));
    }

    private static TraversalKind traversalKind(RouteTraversalHint hint) {
        return switch (Objects.requireNonNull(hint, "hint")) {
            case WALK -> TraversalKind.WALK;
            case JUMP -> TraversalKind.JUMP;
            case CLIMB -> TraversalKind.CLIMB;
            case DROP -> TraversalKind.DROP;
            case SWIM -> TraversalKind.SWIM;
        };
    }

    private static WorldPoint blockCenter(BlockPosition position) {
        BlockPosition blockPosition = Objects.requireNonNull(position, "position");
        return new WorldPoint(blockPosition.x() + 0.5, blockPosition.y(), blockPosition.z() + 0.5);
    }
}

package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class RouteProgressPolicy {
    private static final double PASSED_NODE_RADIUS_MULTIPLIER = 2.5;
    private static final double PASSED_NODE_HEIGHT_TOLERANCE = 0.65;
    private static final double SKIPPED_NODE_CORRIDOR_MULTIPLIER = 3.0;

    private final double reachedDistance;

    public RouteProgressPolicy(double reachedDistance) {
        if (!Double.isFinite(reachedDistance) || reachedDistance <= 0.0) {
            throw new IllegalArgumentException("reachedDistance must be positive.");
        }
        this.reachedDistance = reachedDistance;
    }

    public PathProgress progress(NavigationPath path, NavigationPoint position, PathProgress current) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        NavigationPoint currentPosition = Objects.requireNonNull(position, "position");
        PathProgress progress = Objects.requireNonNull(current, "current");
        int nextIndex = Math.clamp(progress.nextNodeIndex(), 1, navigationPath.nodeCount() - 1);
        while (canAdvance(navigationPath, currentPosition, nextIndex)) {
            nextIndex++;
        }
        return new PathProgress(nextIndex);
    }

    public boolean completed(NavigationPath path, NavigationPoint position) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        NavigationPoint currentPosition = Objects.requireNonNull(position, "position");
        return currentPosition.distanceTo(navigationPath.lastNode()) <= reachedDistance;
    }

    private boolean canAdvance(NavigationPath path, NavigationPoint position, int nextIndex) {
        if (nextIndex >= path.nodeCount() - 1) {
            return false;
        }
        if (position.distanceTo(path.nodeAt(nextIndex)) <= reachedDistance) {
            return true;
        }
        return hasPassedNodeGate(path, position, nextIndex) || hasSkippedNodeOnCorridor(path, position, nextIndex);
    }

    private boolean hasPassedNodeGate(NavigationPath path, NavigationPoint position, int nextIndex) {
        NavigationPoint previous = path.nodeAt(nextIndex - 1);
        NavigationPoint node = path.nodeAt(nextIndex);
        if (!isInsidePassedNodeGate(position, node)) {
            return false;
        }
        HorizontalVector incoming = previous.horizontalVectorTo(node);
        if (!incoming.isZero()) {
            return hasPassedAlongSegment(node, position, incoming);
        }
        return hasPassedAlongOutgoingSegment(path, node, position, nextIndex);
    }

    private boolean isInsidePassedNodeGate(NavigationPoint position, NavigationPoint node) {
        double allowedRadius = reachedDistance * PASSED_NODE_RADIUS_MULTIPLIER;
        double allowedHeight = Math.max(reachedDistance, PASSED_NODE_HEIGHT_TOLERANCE);
        return position.horizontalDistanceTo(node) <= allowedRadius
                && Math.abs(position.y() - node.y()) <= allowedHeight;
    }

    private static boolean hasPassedAlongSegment(
            NavigationPoint node,
            NavigationPoint position,
            HorizontalVector direction) {
        return node.horizontalVectorTo(position).dot(direction.normalized()) >= 0.0;
    }

    private static boolean hasPassedAlongOutgoingSegment(
            NavigationPath path,
            NavigationPoint node,
            NavigationPoint position,
            int nextIndex) {
        HorizontalVector outgoing = node.horizontalVectorTo(path.nodeAt(nextIndex + 1));
        if (outgoing.isZero()) {
            return false;
        }
        return node.horizontalVectorTo(position).dot(outgoing.normalized()) > 0.0;
    }

    private boolean hasSkippedNodeOnCorridor(NavigationPath path, NavigationPoint position, int nextIndex) {
        NavigationPoint previous = path.nodeAt(nextIndex - 1);
        NavigationPoint node = path.nodeAt(nextIndex);
        NavigationPoint next = path.nodeAt(nextIndex + 1);
        HorizontalVector incoming = previous.horizontalVectorTo(node);
        HorizontalVector outgoing = node.horizontalVectorTo(next);
        if (!hasCompatibleHeight(position, node, next)) {
            return false;
        }
        if (incoming.isZero()) {
            return hasAdvancedAlongOutgoing(position, node, outgoing);
        }
        return hasCrossedIncoming(position, node, incoming)
                && hasAdvancedAlongOutgoing(position, node, outgoing);
    }

    private boolean hasAdvancedAlongOutgoing(
            NavigationPoint position,
            NavigationPoint node,
            HorizontalVector outgoing) {
        if (outgoing.isZero()) {
            return false;
        }
        HorizontalVector offset = node.horizontalVectorTo(position);
        return offset.dot(outgoing.normalized()) > 0.0
                && lateralDistance(offset, outgoing) <= skippedNodeCorridorRadius();
    }

    private static boolean hasCrossedIncoming(
            NavigationPoint position,
            NavigationPoint node,
            HorizontalVector incoming) {
        return node.horizontalVectorTo(position).dot(incoming.normalized()) >= 0.0;
    }

    private boolean hasCompatibleHeight(
            NavigationPoint position,
            NavigationPoint node,
            NavigationPoint next) {
        double allowedHeight = Math.max(reachedDistance, PASSED_NODE_HEIGHT_TOLERANCE);
        return Math.abs(position.y() - node.y()) <= allowedHeight
                || Math.abs(position.y() - next.y()) <= allowedHeight;
    }

    private double skippedNodeCorridorRadius() {
        return reachedDistance * SKIPPED_NODE_CORRIDOR_MULTIPLIER;
    }

    private static double lateralDistance(HorizontalVector offset, HorizontalVector direction) {
        HorizontalVector unit = direction.normalized();
        double along = offset.dot(unit);
        double squared = offset.length() * offset.length() - along * along;
        return Math.sqrt(Math.max(0.0, squared));
    }
}

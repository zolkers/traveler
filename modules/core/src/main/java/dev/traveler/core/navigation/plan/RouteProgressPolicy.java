package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
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

    public PathProgress progress(NavigationPath path, WorldPoint position, PathProgress current) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        WorldPoint currentPosition = Objects.requireNonNull(position, "position");
        PathProgress progress = Objects.requireNonNull(current, "current");
        int nextIndex = Math.clamp(progress.nextNodeIndex(), 1, navigationPath.nodeCount() - 1);
        while (canAdvance(navigationPath, currentPosition, nextIndex)) {
            nextIndex++;
        }
        return new PathProgress(nextIndex);
    }

    public boolean completed(NavigationPath path, WorldPoint position) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        WorldPoint currentPosition = Objects.requireNonNull(position, "position");
        return currentPosition.distanceTo(navigationPath.lastNode()) <= reachedDistance;
    }

    private boolean canAdvance(NavigationPath path, WorldPoint position, int nextIndex) {
        if (nextIndex >= path.nodeCount() - 1) {
            return false;
        }
        if (position.distanceTo(path.nodeAt(nextIndex)) <= reachedDistance) {
            return true;
        }
        if (requiresPreciseReach(path.actionBeforeNode(nextIndex))) {
            return hasClearlySkippedSpecialActionNode(path, position, nextIndex);
        }
        return hasPassedNodeGate(path, position, nextIndex) || hasSkippedNodeOnCorridor(path, position, nextIndex);
    }

    private static boolean requiresPreciseReach(MovementAction action) {
        return action == MovementAction.JUMP
                || action == MovementAction.STEP_UP
                || action == MovementAction.DROP;
    }

    private boolean hasClearlySkippedSpecialActionNode(NavigationPath path, WorldPoint position, int nextIndex) {
        WorldPoint node = path.nodeAt(nextIndex);
        WorldPoint next = path.nodeAt(nextIndex + 1);
        HorizontalVector outgoing = node.horizontalVectorTo(next);
        if (!hasCompatibleHeight(position, node, next) || outgoing.isZero()) {
            return false;
        }
        HorizontalVector offset = node.horizontalVectorTo(position);
        double advanced = offset.dot(outgoing.normalized());
        double requiredAdvance = reachedDistance * PASSED_NODE_RADIUS_MULTIPLIER;
        return advanced >= requiredAdvance
                && lateralDistance(offset, outgoing) <= skippedNodeCorridorRadius();
    }

    private boolean hasPassedNodeGate(NavigationPath path, WorldPoint position, int nextIndex) {
        WorldPoint previous = path.nodeAt(nextIndex - 1);
        WorldPoint node = path.nodeAt(nextIndex);
        if (!isInsidePassedNodeGate(position, node)) {
            return false;
        }
        HorizontalVector incoming = previous.horizontalVectorTo(node);
        if (!incoming.isZero()) {
            return hasPassedAlongSegment(node, position, incoming);
        }
        return hasPassedAlongOutgoingSegment(path, node, position, nextIndex);
    }

    private boolean isInsidePassedNodeGate(WorldPoint position, WorldPoint node) {
        double allowedRadius = reachedDistance * PASSED_NODE_RADIUS_MULTIPLIER;
        double allowedHeight = Math.max(reachedDistance, PASSED_NODE_HEIGHT_TOLERANCE);
        return position.horizontalDistanceTo(node) <= allowedRadius
                && Math.abs(position.y() - node.y()) <= allowedHeight;
    }

    private static boolean hasPassedAlongSegment(
            WorldPoint node,
            WorldPoint position,
            HorizontalVector direction) {
        return node.horizontalVectorTo(position).dot(direction.normalized()) >= 0.0;
    }

    private static boolean hasPassedAlongOutgoingSegment(
            NavigationPath path,
            WorldPoint node,
            WorldPoint position,
            int nextIndex) {
        HorizontalVector outgoing = node.horizontalVectorTo(path.nodeAt(nextIndex + 1));
        if (outgoing.isZero()) {
            return false;
        }
        return node.horizontalVectorTo(position).dot(outgoing.normalized()) > 0.0;
    }

    private boolean hasSkippedNodeOnCorridor(NavigationPath path, WorldPoint position, int nextIndex) {
        WorldPoint previous = path.nodeAt(nextIndex - 1);
        WorldPoint node = path.nodeAt(nextIndex);
        WorldPoint next = path.nodeAt(nextIndex + 1);
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
            WorldPoint position,
            WorldPoint node,
            HorizontalVector outgoing) {
        if (outgoing.isZero()) {
            return false;
        }
        HorizontalVector offset = node.horizontalVectorTo(position);
        return offset.dot(outgoing.normalized()) > 0.0
                && lateralDistance(offset, outgoing) <= skippedNodeCorridorRadius();
    }

    private static boolean hasCrossedIncoming(
            WorldPoint position,
            WorldPoint node,
            HorizontalVector incoming) {
        return node.horizontalVectorTo(position).dot(incoming.normalized()) >= 0.0;
    }

    private boolean hasCompatibleHeight(
            WorldPoint position,
            WorldPoint node,
            WorldPoint next) {
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

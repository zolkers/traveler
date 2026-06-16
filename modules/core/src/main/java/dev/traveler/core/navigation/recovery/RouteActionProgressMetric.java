package dev.traveler.core.navigation.recovery;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Objects;

public final class RouteActionProgressMetric implements MovementProgressMetric {
    @Override
    public ProgressSample sample(NavigationPath path, NavigationFrameInput input, NavigationControlFrame frame) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        NavigationControlFrame controlFrame = Objects.requireNonNull(frame, "frame");
        int nextNodeIndex = nextNodeIndex(navigationPath, controlFrame);
        NavigationPoint start = navigationPath.nodeAt(nextNodeIndex - 1);
        NavigationPoint end = navigationPath.nodeAt(nextNodeIndex);
        MovementAction action = navigationPath.actionBeforeNode(nextNodeIndex);
        NavigationPoint actionTarget = navigationPath.actionTargetBeforeNode(nextNodeIndex);
        NavigationPoint position = frameInput.position();
        return new ProgressSample(
                routeProgress(action, start, end, position),
                lateralDistance(action, start, end, position),
                position.distanceTo(actionTarget),
                action,
                controlFrame.plan().phase());
    }

    private static int nextNodeIndex(NavigationPath path, NavigationControlFrame frame) {
        int requested = frame.state().progress().nextNodeIndex();
        return Math.clamp(requested, 1, path.nodeCount() - 1);
    }

    private static double routeProgress(
            MovementAction action,
            NavigationPoint start,
            NavigationPoint end,
            NavigationPoint position) {
        if (action == MovementAction.CLIMB || action == MovementAction.DROP) {
            double verticalDelta = end.y() - start.y();
            if (Math.abs(verticalDelta) > 1.0E-6) {
                return (position.y() - start.y()) * Math.signum(verticalDelta);
            }
        }
        return horizontalProjection(start, end, position);
    }

    private static double lateralDistance(
            MovementAction action,
            NavigationPoint start,
            NavigationPoint end,
            NavigationPoint position) {
        if (action == MovementAction.CLIMB) {
            return position.horizontalDistanceTo(start);
        }
        return horizontalDistanceToSegment(start, end, position);
    }

    private static double horizontalProjection(
            NavigationPoint start,
            NavigationPoint end,
            NavigationPoint position) {
        double deltaX = end.x() - start.x();
        double deltaZ = end.z() - start.z();
        double length = Math.hypot(deltaX, deltaZ);
        if (length <= 1.0E-6) {
            return 0.0;
        }
        double unitX = deltaX / length;
        double unitZ = deltaZ / length;
        return (position.x() - start.x()) * unitX + (position.z() - start.z()) * unitZ;
    }

    private static double horizontalDistanceToSegment(
            NavigationPoint start,
            NavigationPoint end,
            NavigationPoint position) {
        double deltaX = end.x() - start.x();
        double deltaZ = end.z() - start.z();
        double lengthSquared = deltaX * deltaX + deltaZ * deltaZ;
        if (lengthSquared <= 1.0E-12) {
            return position.horizontalDistanceTo(start);
        }
        double progress = ((position.x() - start.x()) * deltaX + (position.z() - start.z()) * deltaZ)
                / lengthSquared;
        double clamped = Math.clamp(progress, 0.0, 1.0);
        double closestX = start.x() + deltaX * clamped;
        double closestZ = start.z() + deltaZ * clamped;
        return Math.hypot(position.x() - closestX, position.z() - closestZ);
    }
}

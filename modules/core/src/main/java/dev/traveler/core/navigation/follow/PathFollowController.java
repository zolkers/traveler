package dev.traveler.core.navigation.follow;

import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class PathFollowController {
    private static final double STEP_UP_HEIGHT = 0.25;
    private static final double JUMP_HEIGHT = 0.75;
    private static final double DROP_HEIGHT = -0.75;

    private final PathFollowSettings settings;

    public PathFollowController(PathFollowSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public PathFollowFrame update(NavigationPath path, NavigationPoint position, PathProgress progress) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        NavigationPoint currentPosition = Objects.requireNonNull(position, "position");
        PathProgress currentProgress = Objects.requireNonNull(progress, "progress");
        if (isCompleted(navigationPath, currentPosition)) {
            return completedFrame(navigationPath);
        }
        int nextIndex = advanceReachedNode(navigationPath, currentPosition, currentProgress.nextNodeIndex());
        NavigationPoint target = lookAheadTarget(navigationPath, currentPosition, nextIndex);
        double speed = speedScale(currentPosition.horizontalDistanceTo(navigationPath.lastNode()));
        return new PathFollowFrame(
                MovementTarget.follow(target),
                new PathProgress(nextIndex),
                speed,
                locomotionPlan(currentPosition, target),
                false);
    }

    private boolean isCompleted(NavigationPath path, NavigationPoint position) {
        return position.distanceTo(path.lastNode()) <= settings.reachedDistance();
    }

    private PathFollowFrame completedFrame(NavigationPath path) {
        int lastIndex = path.nodeCount() - 1;
        return new PathFollowFrame(MovementTarget.stopAt(path.lastNode()), new PathProgress(lastIndex), 0.0, true);
    }

    private int advanceReachedNode(NavigationPath path, NavigationPoint position, int requestedIndex) {
        int nextIndex = Math.clamp(requestedIndex, 1, path.nodeCount() - 1);
        while (canAdvance(path, position, nextIndex)) {
            nextIndex++;
        }
        return nextIndex;
    }

    private boolean canAdvance(NavigationPath path, NavigationPoint position, int nextIndex) {
        return nextIndex < path.nodeCount() - 1
                && position.distanceTo(path.nodeAt(nextIndex)) <= settings.reachedDistance();
    }

    private NavigationPoint lookAheadTarget(NavigationPath path, NavigationPoint position, int nextIndex) {
        double remainingDistance = settings.lookAheadDistance();
        NavigationPoint cursor = position;
        for (int index = nextIndex; index < path.nodeCount(); index++) {
            NavigationPoint node = path.nodeAt(index);
            if (isVerticalStep(cursor, node)) {
                return node;
            }
            double segmentDistance = cursor.horizontalDistanceTo(node);
            if (segmentDistance >= remainingDistance) {
                return cursor.interpolate(node, remainingDistance / segmentDistance);
            }
            remainingDistance -= segmentDistance;
            cursor = node;
        }
        return path.lastNode();
    }

    private static boolean isVerticalStep(NavigationPoint from, NavigationPoint to) {
        return from.horizontalDistanceTo(to) <= 1.0E-6 && from.distanceTo(to) > 1.0E-6;
    }

    private static LocomotionPlan locomotionPlan(NavigationPoint position, NavigationPoint target) {
        double heightDelta = target.y() - position.y();
        if (heightDelta >= JUMP_HEIGHT) {
            return LocomotionPlan.jump();
        }
        if (heightDelta >= STEP_UP_HEIGHT) {
            return LocomotionPlan.stepUp();
        }
        if (heightDelta <= DROP_HEIGHT) {
            return LocomotionPlan.drop();
        }
        return LocomotionPlan.walk();
    }

    private double speedScale(double distanceToGoal) {
        if (distanceToGoal >= settings.arrivalDistance()) {
            return 1.0;
        }
        return Math.max(settings.minimumSpeedScale(), distanceToGoal / settings.arrivalDistance());
    }
}

package dev.traveler.core.navigation.follow;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class PathFollowController {
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
        return new PathFollowFrame(MovementTarget.follow(target), new PathProgress(nextIndex), speed, false);
    }

    private boolean isCompleted(NavigationPath path, NavigationPoint position) {
        return position.horizontalDistanceTo(path.lastNode()) <= settings.reachedDistance();
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
                && position.horizontalDistanceTo(path.nodeAt(nextIndex)) <= settings.reachedDistance();
    }

    private NavigationPoint lookAheadTarget(NavigationPath path, NavigationPoint position, int nextIndex) {
        double remainingDistance = settings.lookAheadDistance();
        NavigationPoint cursor = position;
        for (int index = nextIndex; index < path.nodeCount(); index++) {
            NavigationPoint node = path.nodeAt(index);
            double segmentDistance = cursor.horizontalDistanceTo(node);
            if (segmentDistance >= remainingDistance) {
                return cursor.interpolate(node, remainingDistance / segmentDistance);
            }
            remainingDistance -= segmentDistance;
            cursor = node;
        }
        return path.lastNode();
    }

    private double speedScale(double distanceToGoal) {
        if (distanceToGoal >= settings.arrivalDistance()) {
            return 1.0;
        }
        return Math.max(settings.minimumSpeedScale(), distanceToGoal / settings.arrivalDistance());
    }
}

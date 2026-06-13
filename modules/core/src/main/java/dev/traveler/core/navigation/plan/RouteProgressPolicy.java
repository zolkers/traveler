package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class RouteProgressPolicy {
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
        return nextIndex < path.nodeCount() - 1
                && position.distanceTo(path.nodeAt(nextIndex)) <= reachedDistance;
    }
}

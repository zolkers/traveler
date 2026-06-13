package dev.traveler.core.navigation.follow;

import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class PathCorridor {
    private static final double ZERO_LENGTH = 1.0E-6;
    private static final double SAME_DISTANCE = 1.0E-9;

    private final NavigationPath path;
    private final int startIndex;
    private final double length;

    private PathCorridor(NavigationPath path, int startIndex, double length) {
        this.path = path;
        this.startIndex = startIndex;
        this.length = length;
    }

    public static PathCorridor from(NavigationPath path) {
        return from(path, 0);
    }

    public static PathCorridor from(NavigationPath path, int startNodeIndex) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        int startIndex = Math.clamp(startNodeIndex, 0, navigationPath.nodeCount() - 2);
        return new PathCorridor(navigationPath, startIndex, pathLength(navigationPath, startIndex));
    }

    public double length() {
        return length;
    }

    public PathProjection project(NavigationPoint position) {
        NavigationPoint point = Objects.requireNonNull(position, "position");
        BestProjection projection = new BestProjection(point, path.nodeAt(startIndex));
        double distance = 0.0;
        for (int index = startIndex + 1; index < path.nodeCount(); index++) {
            NavigationPoint from = path.nodeAt(index - 1);
            NavigationPoint to = path.nodeAt(index);
            double segmentLength = horizontalDistance(from, to);
            projection.consider(point, from, to, distance, segmentLength);
            distance += segmentLength;
        }
        return projection.toProjection();
    }

    public NavigationPoint targetAt(double distanceOnPath) {
        requireFinite(distanceOnPath, "distanceOnPath");
        if (length <= ZERO_LENGTH) {
            return path.nodeAt(startIndex);
        }
        double targetDistance = Math.clamp(distanceOnPath, 0.0, length);
        double distance = 0.0;
        for (int index = startIndex + 1; index < path.nodeCount(); index++) {
            NavigationPoint from = path.nodeAt(index - 1);
            NavigationPoint to = path.nodeAt(index);
            double segmentLength = horizontalDistance(from, to);
            if (segmentLength <= ZERO_LENGTH) {
                continue;
            }
            double endDistance = distance + segmentLength;
            if (targetDistance <= endDistance) {
                return from.interpolate(to, (targetDistance - distance) / segmentLength);
            }
            distance = endDistance;
        }
        return path.lastNode();
    }

    private static double pathLength(NavigationPath path, int startIndex) {
        double distance = 0.0;
        for (int index = startIndex + 1; index < path.nodeCount(); index++) {
            distance += horizontalDistance(path.nodeAt(index - 1), path.nodeAt(index));
        }
        return distance;
    }

    private static double horizontalDistance(NavigationPoint first, NavigationPoint second) {
        double dx = first.x() - second.x();
        double dz = first.z() - second.z();
        if (dx == 0.0) {
            return Math.abs(dz);
        }
        if (dz == 0.0) {
            return Math.abs(dx);
        }
        return Math.hypot(dx, dz);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }

    private static double projectionRatio(
            NavigationPoint point,
            NavigationPoint from,
            NavigationPoint to,
            double segmentLength) {
        if (segmentLength <= ZERO_LENGTH) {
            return 0.0;
        }
        double segmentX = to.x() - from.x();
        double segmentZ = to.z() - from.z();
        double offsetX = point.x() - from.x();
        double offsetZ = point.z() - from.z();
        return Math.clamp((offsetX * segmentX + offsetZ * segmentZ) / (segmentLength * segmentLength), 0.0, 1.0);
    }

    private static final class BestProjection {
        private NavigationPoint nearestPoint;
        private HorizontalVector tangent = new HorizontalVector(0.0, 0.0);
        private double distanceOnPath;
        private double lateralError;

        BestProjection(NavigationPoint point, NavigationPoint fallback) {
            nearestPoint = fallback;
            lateralError = horizontalDistance(point, fallback);
        }

        void consider(
                NavigationPoint point,
                NavigationPoint from,
                NavigationPoint to,
                double segmentStart,
                double segmentLength) {
            if (segmentLength <= ZERO_LENGTH) {
                return;
            }
            double ratio = projectionRatio(point, from, to, segmentLength);
            double nearestX = from.x() + (to.x() - from.x()) * ratio;
            double nearestY = from.y() + (to.y() - from.y()) * ratio;
            double nearestZ = from.z() + (to.z() - from.z()) * ratio;
            double error = Math.hypot(point.x() - nearestX, point.z() - nearestZ);
            double distance = segmentStart + segmentLength * ratio;
            updateIfCloser(from, to, nearestX, nearestY, nearestZ, distance, error);
        }

        private void updateIfCloser(
                NavigationPoint from,
                NavigationPoint to,
                double nearestX,
                double nearestY,
                double nearestZ,
                double distance,
                double error) {
            if (!isCloser(error, distance)) {
                return;
            }
            nearestPoint = new NavigationPoint(nearestX, nearestY, nearestZ);
            tangent = from.horizontalVectorTo(to).normalized();
            distanceOnPath = distance;
            lateralError = error;
        }

        private boolean isCloser(double error, double distance) {
            if (error < lateralError - SAME_DISTANCE) {
                return true;
            }
            return Math.abs(error - lateralError) <= SAME_DISTANCE && distance >= distanceOnPath;
        }

        PathProjection toProjection() {
            return new PathProjection(nearestPoint, tangent, distanceOnPath, lateralError);
        }
    }
}

package dev.traveler.core.navigation.camera;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class CameraTargetPlanner {
    private static final double MIN_SEGMENT_DISTANCE = 1.0E-6;

    private final CameraTargetSettings settings;

    public CameraTargetPlanner(CameraTargetSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public static CameraTargetPlanner standard() {
        return new CameraTargetPlanner(CameraTargetSettings.standard());
    }

    public CameraAngles targetAngles(
            NavigationPath path,
            NavigationPoint position,
            PathProgress progress,
            CameraAngles currentAngles) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        NavigationPoint currentPosition = Objects.requireNonNull(position, "position");
        PathProgress currentProgress = Objects.requireNonNull(progress, "progress");
        CameraAngles current = Objects.requireNonNull(currentAngles, "currentAngles");
        NavigationPoint target = lookAheadTarget(navigationPath, currentPosition, currentProgress);
        if (currentPosition.horizontalDistanceTo(target) < settings.minimumHorizontalDistance()) {
            return new CameraAngles(current.yawDegrees(), settings.neutralPitchDegrees());
        }
        return CameraAimController.targetAngles(currentPosition, horizontalTarget(currentPosition, target));
    }

    private NavigationPoint lookAheadTarget(
            NavigationPath path,
            NavigationPoint position,
            PathProgress progress) {
        double remainingDistance = settings.lookAheadDistance();
        NavigationPoint cursor = position;
        int startIndex = Math.clamp(progress.nextNodeIndex(), 1, path.nodeCount() - 1);
        for (int index = startIndex; index < path.nodeCount(); index++) {
            NavigationPoint node = path.nodeAt(index);
            double segmentDistance = cursor.horizontalDistanceTo(node);
            if (segmentDistance <= MIN_SEGMENT_DISTANCE) {
                cursor = node;
                continue;
            }
            if (segmentDistance >= remainingDistance) {
                return cursor.interpolate(node, remainingDistance / segmentDistance);
            }
            remainingDistance -= segmentDistance;
            cursor = node;
        }
        return cursor;
    }

    private static NavigationPoint horizontalTarget(NavigationPoint position, NavigationPoint target) {
        return new NavigationPoint(target.x(), position.y(), target.z());
    }
}

package dev.traveler.core.navigation.camera;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathCorridor;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.follow.PathProjection;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class CameraTargetPlanner {
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
        PathCorridor corridor = PathCorridor.from(path, progress.nextNodeIndex() - 1);
        PathProjection projection = corridor.project(position);
        return corridor.targetAt(projection.distanceOnPath() + settings.lookAheadDistance());
    }

    private static NavigationPoint horizontalTarget(NavigationPoint position, NavigationPoint target) {
        return new NavigationPoint(target.x(), position.y(), target.z());
    }
}

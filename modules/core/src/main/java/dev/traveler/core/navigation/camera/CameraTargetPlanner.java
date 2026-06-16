package dev.traveler.core.navigation.camera;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathCorridor;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.follow.PathProjection;
import dev.traveler.core.common.geometry.WorldPoint;
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
            WorldPoint position,
            PathProgress progress,
            CameraAngles currentAngles) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        WorldPoint currentPosition = Objects.requireNonNull(position, "position");
        PathProgress currentProgress = Objects.requireNonNull(progress, "progress");
        CameraAngles current = Objects.requireNonNull(currentAngles, "currentAngles");
        WorldPoint target = lookAheadTarget(navigationPath, currentPosition, currentProgress);
        if (currentPosition.horizontalDistanceTo(target) < settings.minimumHorizontalDistance()) {
            return new CameraAngles(current.yawDegrees(), settings.neutralPitchDegrees());
        }
        CameraAngles targetAngles = CameraAimController.targetAngles(
                currentPosition,
                smartVerticalTarget(currentPosition, target));
        return new CameraAngles(targetAngles.yawDegrees(), clampedPitch(targetAngles.pitchDegrees()));
    }

    private WorldPoint lookAheadTarget(
            NavigationPath path,
            WorldPoint position,
            PathProgress progress) {
        PathCorridor corridor = PathCorridor.from(path, progress.nextNodeIndex() - 1);
        PathProjection projection = corridor.project(position);
        return corridor.targetAt(projection.distanceOnPath() + settings.lookAheadDistance());
    }

    private WorldPoint smartVerticalTarget(WorldPoint position, WorldPoint target) {
        double targetY = position.y() + (target.y() - position.y()) * settings.verticalAimScale();
        return new WorldPoint(target.x(), targetY, target.z());
    }

    private double clampedPitch(double pitchDegrees) {
        return Math.clamp(
                pitchDegrees,
                -settings.maxPitchDegrees(),
                settings.maxPitchDegrees());
    }
}

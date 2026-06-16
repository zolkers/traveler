package dev.traveler.core.navigation.steering;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathCorridor;
import dev.traveler.core.navigation.follow.PathProjection;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;

public final class PathSteeringController {
    private final PathSteeringSettings settings;
    private NavigationPath cachedPath;
    private int cachedStartIndex = -1;
    private PathCorridor cachedCorridor;

    public PathSteeringController(PathSteeringSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public static PathSteeringController standard() {
        return new PathSteeringController(PathSteeringSettings.standard());
    }

    public SteeringPlan plan(
            NavigationPath path,
            WorldPoint position,
            AgentMotionState motionState,
            int nextNodeIndex) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        WorldPoint currentPosition = Objects.requireNonNull(position, "position");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        PathCorridor corridor = corridor(navigationPath, nextNodeIndex - 1);
        WorldPoint predicted = predictedPosition(currentPosition, motion);
        PathProjection projection = corridor.project(predicted);
        double targetDistance = projection.distanceOnPath() + settings.pathOffset();
        WorldPoint pathTarget = corridor.targetAt(targetDistance);
        HorizontalVector correction = lateralCorrection(predicted, projection);
        WorldPoint steeringTarget = offset(pathTarget, correction);
        boolean outsideCorridor = projection.lateralError() > settings.corridorRadius();
        return SteeringPlan.corridor(
                steeringTarget,
                pathTarget,
                projection.nearestPoint(),
                projection.tangent(),
                correction,
                projection.lateralError(),
                projection.signedLateralError(),
                Math.clamp(targetDistance, 0.0, corridor.length()),
                outsideCorridor,
                projection.lateralError() >= settings.clearanceWarningLateralError());
    }

    private WorldPoint predictedPosition(WorldPoint position, AgentMotionState motion) {
        HorizontalVector drift = motion.horizontalVelocity().scaled(settings.predictionSeconds());
        double y = position.y() + motion.verticalVelocity() * settings.predictionSeconds();
        return new WorldPoint(position.x() + drift.x(), y, position.z() + drift.z());
    }

    private HorizontalVector lateralCorrection(WorldPoint position, PathProjection projection) {
        double absoluteError = Math.abs(projection.signedLateralError());
        if (absoluteError <= settings.lateralCorrectionDeadband()) {
            return new HorizontalVector(0.0, 0.0);
        }
        HorizontalVector toCenter = position.horizontalVectorTo(projection.nearestPoint()).normalized();
        double excess = absoluteError - settings.lateralCorrectionDeadband();
        double distance = Math.min(settings.maxCorrectionDistance(), excess * settings.lateralCorrectionGain());
        return toCenter.scaled(distance);
    }

    private static WorldPoint offset(WorldPoint point, HorizontalVector correction) {
        return new WorldPoint(point.x() + correction.x(), point.y(), point.z() + correction.z());
    }

    private PathCorridor corridor(NavigationPath path, int requestedStartIndex) {
        int startIndex = Math.clamp(requestedStartIndex, 0, path.nodeCount() - 2);
        if (path == cachedPath && startIndex == cachedStartIndex) {
            return cachedCorridor;
        }
        cachedPath = path;
        cachedStartIndex = startIndex;
        cachedCorridor = PathCorridor.from(path, startIndex);
        return cachedCorridor;
    }
}

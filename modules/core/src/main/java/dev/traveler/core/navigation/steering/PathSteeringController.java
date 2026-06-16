package dev.traveler.core.navigation.steering;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathCorridor;
import dev.traveler.core.navigation.follow.PathProjection;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
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
        int startIndex = corridorStartIndex(navigationPath, nextNodeIndex - 1);
        PathCorridor corridor = corridor(navigationPath, startIndex);
        WorldPoint predicted = predictedPosition(currentPosition, motion);
        PathProjection projection = corridor.project(predicted);
        double targetDistance = projection.distanceOnPath()
                + adaptivePathOffset(navigationPath, projection, nextNodeIndex);
        targetDistance = Math.min(targetDistance, actionBoundaryDistance(navigationPath, startIndex, nextNodeIndex));
        WorldPoint pathTarget = corridor.targetAt(targetDistance);
        HorizontalVector correction = lateralCorrection(predicted, projection, motion);
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

    private double adaptivePathOffset(
            NavigationPath path,
            PathProjection projection,
            int nextNodeIndex) {
        double lateralExcess = Math.max(
                0.0,
                projection.lateralError() - settings.lateralCorrectionDeadband());
        double lookahead = settings.pathOffset()
                - lateralExcess * settings.lateralErrorLookaheadReductionGain();
        if (upcomingActionRequiresPrecision(path, nextNodeIndex)) {
            lookahead = Math.min(lookahead, settings.actionApproachPathOffset());
        }
        return Math.clamp(lookahead, settings.minimumPathOffset(), settings.pathOffset());
    }

    private HorizontalVector lateralCorrection(
            WorldPoint position,
            PathProjection projection,
            AgentMotionState motion) {
        double absoluteError = Math.abs(projection.signedLateralError());
        if (absoluteError <= settings.lateralCorrectionDeadband()) {
            return new HorizontalVector(0.0, 0.0);
        }
        HorizontalVector toCenter = position.horizontalVectorTo(projection.nearestPoint()).normalized();
        double excess = absoluteError - settings.lateralCorrectionDeadband();
        double lateralErrorRate = lateralErrorRate(projection, motion.horizontalVelocity());
        double distance = excess * settings.lateralCorrectionGain()
                + lateralErrorRate * settings.lateralCorrectionDerivativeGain();
        distance = Math.clamp(distance, 0.0, settings.maxCorrectionDistance());
        return toCenter.scaled(distance);
    }

    private static double lateralErrorRate(PathProjection projection, HorizontalVector velocity) {
        double errorDirection = Math.signum(projection.signedLateralError());
        if (errorDirection == 0.0 || projection.tangent().isZero() || velocity.isZero()) {
            return 0.0;
        }
        HorizontalVector signedLateralAxis = new HorizontalVector(
                -projection.tangent().z(),
                projection.tangent().x());
        return errorDirection * velocity.dot(signedLateralAxis);
    }

    private static WorldPoint offset(WorldPoint point, HorizontalVector correction) {
        return new WorldPoint(point.x() + correction.x(), point.y(), point.z() + correction.z());
    }

    private double actionBoundaryDistance(NavigationPath path, int startIndex, int nextNodeIndex) {
        if (!upcomingActionRequiresPrecision(path, nextNodeIndex)) {
            return Double.POSITIVE_INFINITY;
        }
        int boundaryNodeIndex = Math.clamp(nextNodeIndex, startIndex + 1, path.nodeCount() - 1);
        return distanceFromStartToNode(path, startIndex, boundaryNodeIndex);
    }

    private static boolean upcomingActionRequiresPrecision(NavigationPath path, int nextNodeIndex) {
        int upcomingSegmentEnd = nextNodeIndex + 1;
        if (upcomingSegmentEnd >= path.nodeCount()) {
            return false;
        }
        return requiresPrecision(path.actionBeforeNode(upcomingSegmentEnd));
    }

    private static boolean requiresPrecision(MovementAction action) {
        return switch (action) {
            case WALK, SWIM -> false;
            case BLOCKED, STEP_UP, DROP, JUMP, CLIMB -> true;
        };
    }

    private static double distanceFromStartToNode(NavigationPath path, int startIndex, int nodeIndex) {
        double distance = 0.0;
        for (int index = startIndex + 1; index <= nodeIndex; index++) {
            distance += path.nodeAt(index - 1).horizontalDistanceTo(path.nodeAt(index));
        }
        return distance;
    }

    private PathCorridor corridor(NavigationPath path, int requestedStartIndex) {
        int startIndex = corridorStartIndex(path, requestedStartIndex);
        if (path == cachedPath && startIndex == cachedStartIndex) {
            return cachedCorridor;
        }
        cachedPath = path;
        cachedStartIndex = startIndex;
        cachedCorridor = PathCorridor.from(path, startIndex);
        return cachedCorridor;
    }

    private static int corridorStartIndex(NavigationPath path, int requestedStartIndex) {
        return Math.clamp(requestedStartIndex, 0, path.nodeCount() - 2);
    }
}

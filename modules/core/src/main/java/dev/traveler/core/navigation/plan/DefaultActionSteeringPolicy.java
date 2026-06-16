package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.navigation.steering.SteeringPlan;
import java.util.Objects;

public final class DefaultActionSteeringPolicy implements ActionSteeringPolicy {
    private static final double ZERO_LENGTH = 1.0E-6;

    @Override
    public SteeringPlan steeringFor(
            LocomotionPlan action,
            NavigationPath path,
            WorldPoint position,
            int nextNodeIndex,
            WorldPoint actionTarget,
            SteeringPlan pathSteering) {
        LocomotionPlan requestedAction = Objects.requireNonNull(action, "action");
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        WorldPoint currentPosition = Objects.requireNonNull(position, "position");
        WorldPoint target = Objects.requireNonNull(actionTarget, "actionTarget");
        SteeringPlan fallbackSteering = Objects.requireNonNull(pathSteering, "pathSteering");
        return switch (requestedAction.action()) {
            case CLIMB -> SteeringPlan.seek(target, currentPosition.horizontalDistanceTo(target));
            case JUMP, STEP_UP -> jumpSteering(
                    navigationPath,
                    currentPosition,
                    nextNodeIndex,
                    target,
                    fallbackSteering);
            default -> fallbackSteering;
        };
    }

    private static SteeringPlan jumpSteering(
            NavigationPath path,
            WorldPoint position,
            int nextNodeIndex,
            WorldPoint actionTarget,
            SteeringPlan fallbackSteering) {
        WorldPoint start = path.nodeAt(Math.clamp(nextNodeIndex - 1, 0, path.nodeCount() - 2));
        if (horizontalLength(start, actionTarget) <= ZERO_LENGTH) {
            return fallbackSteering;
        }
        return SteeringPlan.seek(
                actionTarget,
                lateralDistanceToActionSegment(path, position, nextNodeIndex, actionTarget));
    }

    private static double lateralDistanceToActionSegment(
            NavigationPath path,
            WorldPoint position,
            int nextNodeIndex,
            WorldPoint actionTarget) {
        WorldPoint start = path.nodeAt(Math.clamp(nextNodeIndex - 1, 0, path.nodeCount() - 2));
        double dx = actionTarget.x() - start.x();
        double dz = actionTarget.z() - start.z();
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared <= ZERO_LENGTH) {
            return position.horizontalDistanceTo(actionTarget);
        }
        double offsetX = position.x() - start.x();
        double offsetZ = position.z() - start.z();
        double ratio = Math.clamp((offsetX * dx + offsetZ * dz) / lengthSquared, 0.0, 1.0);
        double nearestX = start.x() + dx * ratio;
        double nearestZ = start.z() + dz * ratio;
        return Math.hypot(position.x() - nearestX, position.z() - nearestZ);
    }

    private static double horizontalLength(WorldPoint from, WorldPoint to) {
        return Math.hypot(to.x() - from.x(), to.z() - from.z());
    }
}

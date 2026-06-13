package dev.traveler.core.navigation.follow;

import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.steering.PathSteeringController;
import dev.traveler.core.navigation.steering.PathSteeringSettings;
import dev.traveler.core.navigation.steering.SteeringPlan;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class PathFollowController {
    private static final double STEP_UP_HEIGHT = 0.25;
    private static final double JUMP_HEIGHT = 0.75;
    private static final double DROP_HEIGHT = -0.75;

    private final PathFollowSettings settings;
    private final PathSteeringController steeringController;

    public PathFollowController(PathFollowSettings settings) {
        this(settings, steeringControllerFor(settings));
    }

    public PathFollowController(
            PathFollowSettings settings,
            PathSteeringController steeringController) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.steeringController = Objects.requireNonNull(steeringController, "steeringController");
    }

    public PathFollowFrame update(NavigationPath path, NavigationPoint position, PathProgress progress) {
        return update(path, position, progress, AgentMotionState.groundedStill());
    }

    public PathFollowFrame update(
            NavigationPath path,
            NavigationPoint position,
            PathProgress progress,
            AgentMotionState motionState) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        NavigationPoint currentPosition = Objects.requireNonNull(position, "position");
        PathProgress currentProgress = Objects.requireNonNull(progress, "progress");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        if (isCompleted(navigationPath, currentPosition)) {
            return completedFrame(navigationPath);
        }
        int nextIndex = advanceReachedNode(navigationPath, currentPosition, currentProgress.nextNodeIndex());
        NavigationPoint actionTarget = navigationPath.nodeAt(nextIndex);
        LocomotionPlan plan = locomotionPlan(currentPosition, actionTarget);
        SteeringPlan steering = steeringPlan(navigationPath, currentPosition, nextIndex, plan, motion);
        MovementTarget movementTarget = movementTarget(plan, actionTarget, steering.steeringTarget());
        double speed = speedScale(currentPosition.horizontalDistanceTo(navigationPath.lastNode()));
        return new PathFollowFrame(
                movementTarget,
                new PathProgress(nextIndex),
                speed,
                steering,
                plan,
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

    private NavigationPoint actionLookAheadTarget(NavigationPath path, int nextIndex) {
        double remainingDistance = settings.lookAheadDistance();
        NavigationPoint cursor = path.nodeAt(nextIndex);
        for (int index = nextIndex + 1; index < path.nodeCount(); index++) {
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

    private SteeringPlan steeringPlan(
            NavigationPath path,
            NavigationPoint position,
            int nextIndex,
            LocomotionPlan plan,
            AgentMotionState motionState) {
        if (plan.action() == LocomotionAction.WALK) {
            return steeringController.plan(path, position, motionState, nextIndex);
        }
        return SteeringPlan.seek(actionLookAheadTarget(path, nextIndex));
    }

    private static MovementTarget movementTarget(
            LocomotionPlan plan,
            NavigationPoint actionTarget,
            NavigationPoint steeringTarget) {
        if (plan.action() == LocomotionAction.WALK) {
            return MovementTarget.follow(steeringTarget);
        }
        return MovementTarget.follow(actionTarget);
    }

    private double speedScale(double distanceToGoal) {
        if (distanceToGoal >= settings.arrivalDistance()) {
            return 1.0;
        }
        return Math.max(settings.minimumSpeedScale(), distanceToGoal / settings.arrivalDistance());
    }

    private static PathSteeringController steeringControllerFor(PathFollowSettings settings) {
        PathFollowSettings followSettings = Objects.requireNonNull(settings, "settings");
        PathSteeringSettings steeringSettings = PathSteeringSettings.standard(followSettings.lookAheadDistance());
        return new PathSteeringController(steeringSettings);
    }
}

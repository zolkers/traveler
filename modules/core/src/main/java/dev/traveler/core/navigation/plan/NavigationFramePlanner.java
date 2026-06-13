package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathFollowSettings;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.locomotion.LocomotionDecision;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.navigation.steering.PathSteeringController;
import dev.traveler.core.navigation.steering.PathSteeringSettings;
import dev.traveler.core.navigation.steering.SteeringPlan;
import java.util.Objects;

public final class NavigationFramePlanner {
    private final PathFollowSettings settings;
    private final RouteProgressPolicy routeProgressPolicy;
    private final MovementActionPolicy actionPolicy;
    private final PathSteeringController steeringController;
    private final MovementVectorPolicy movementVectorPolicy;
    private final ActionTimingPolicy actionTimingPolicy;
    private final CameraTargetPolicy cameraTargetPolicy;

    public NavigationFramePlanner(
            PathFollowSettings settings,
            RouteProgressPolicy routeProgressPolicy,
            MovementActionPolicy actionPolicy,
            PathSteeringController steeringController,
            MovementVectorPolicy movementVectorPolicy,
            ActionTimingPolicy actionTimingPolicy,
            CameraTargetPolicy cameraTargetPolicy) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.routeProgressPolicy = Objects.requireNonNull(routeProgressPolicy, "routeProgressPolicy");
        this.actionPolicy = Objects.requireNonNull(actionPolicy, "actionPolicy");
        this.steeringController = Objects.requireNonNull(steeringController, "steeringController");
        this.movementVectorPolicy = Objects.requireNonNull(movementVectorPolicy, "movementVectorPolicy");
        this.actionTimingPolicy = Objects.requireNonNull(actionTimingPolicy, "actionTimingPolicy");
        this.cameraTargetPolicy = Objects.requireNonNull(cameraTargetPolicy, "cameraTargetPolicy");
    }

    public static NavigationFramePlanner standard() {
        PathFollowSettings settings = PathFollowSettings.standard();
        PathSteeringSettings steeringSettings = PathSteeringSettings.standard(settings.lookAheadDistance());
        return new NavigationFramePlanner(
                settings,
                new RouteProgressPolicy(settings.reachedDistance()),
                new MovementActionPolicy(),
                new PathSteeringController(steeringSettings),
                MovementVectorPolicy.standard(),
                ActionTimingPolicy.standard(),
                CameraTargetPolicy.standard());
    }

    public NavigationFramePlan plan(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControllerState state) {
        NavigationPath navigationPath = Objects.requireNonNull(path, "path");
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        NavigationControllerState controllerState = Objects.requireNonNull(state, "state");
        if (routeProgressPolicy.completed(navigationPath, frameInput.position())) {
            return completedPlan(navigationPath, frameInput, controllerState);
        }
        return activePlan(navigationPath, frameInput, controllerState);
    }

    private NavigationFramePlan activePlan(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControllerState state) {
        PathProgress progress = routeProgressPolicy.progress(path, input.position(), state.progress());
        NavigationPoint actionTarget = path.nodeAt(progress.nextNodeIndex());
        LocomotionPlan requestedAction = actionPolicy.plan(input.position(), actionTarget, input.motionState());
        SteeringPlan steering = steeringController.plan(
                path,
                input.position(),
                input.motionState(),
                progress.nextNodeIndex());
        MovementVectorIntent movementVector =
                movementVectorPolicy.plan(input.position(), steering, input.cameraAngles(), requestedAction);
        LocomotionPlan timedRequest = timedRequest(requestedAction, movementVector);
        LocomotionDecision timing = actionTimingPolicy.decide(state, progress, timedRequest, input.motionState());
        ActionIntent actionIntent = ActionIntent.from(timing.plan().action());
        SpeedIntent speedIntent = speedIntent(path, input.position(), movementVector);
        return new NavigationFramePlan(
                phaseFor(actionIntent, movementVector),
                progress,
                movementTarget(timing.plan(), actionTarget, steering),
                movementVector,
                cameraTargetPolicy.target(path, input.position(), progress, input.cameraAngles()),
                actionIntent,
                speedIntent,
                ToleranceProfile.standard(),
                timing.state(),
                false);
    }

    private NavigationFramePlan completedPlan(
            NavigationPath path,
            NavigationFrameInput input,
            NavigationControllerState state) {
        int lastIndex = path.nodeCount() - 1;
        return new NavigationFramePlan(
                NavigationPhase.ARRIVE,
                new PathProgress(lastIndex),
                MovementTarget.stopAt(path.lastNode()),
                MovementVectorIntent.idle(),
                input.cameraAngles(),
                ActionIntent.none(),
                SpeedIntent.stop(),
                ToleranceProfile.standard(),
                state.locomotionState(),
                true);
    }

    private static LocomotionPlan timedRequest(
            LocomotionPlan requestedAction,
            MovementVectorIntent movementVector) {
        if (movementVector.specialActionAllowed()) {
            return requestedAction;
        }
        return LocomotionPlan.walk();
    }

    private MovementTarget movementTarget(
            LocomotionPlan action,
            NavigationPoint actionTarget,
            SteeringPlan steering) {
        if (action.action() == LocomotionAction.WALK) {
            return MovementTarget.follow(steering.steeringTarget());
        }
        return MovementTarget.follow(actionTarget);
    }

    private SpeedIntent speedIntent(
            NavigationPath path,
            NavigationPoint position,
            MovementVectorIntent movementVector) {
        double scale = speedScale(position.horizontalDistanceTo(path.lastNode()));
        boolean sprint = scale >= 0.5 && movementVector.mode() == PlannedMovementMode.DIRECT;
        return new SpeedIntent(scale, sprint);
    }

    private double speedScale(double distanceToGoal) {
        if (distanceToGoal >= settings.arrivalDistance()) {
            return 1.0;
        }
        return Math.max(settings.minimumSpeedScale(), distanceToGoal / settings.arrivalDistance());
    }

    private static NavigationPhase phaseFor(
            ActionIntent actionIntent,
            MovementVectorIntent movementVector) {
        if (actionIntent.recoveryRequested()) {
            return NavigationPhase.RECOVER;
        }
        if (actionIntent.action() != LocomotionAction.WALK) {
            return NavigationPhase.EXECUTE_ACTION;
        }
        if (!movementVector.specialActionAllowed()) {
            return NavigationPhase.ALIGN;
        }
        if (movementVector.mode() == PlannedMovementMode.DIRECT) {
            return NavigationPhase.APPROACH;
        }
        return NavigationPhase.ALIGN;
    }
}

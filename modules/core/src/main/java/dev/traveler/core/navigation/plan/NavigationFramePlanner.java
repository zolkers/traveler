package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathFollowSettings;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.camera.CameraTargetPlanner;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.locomotion.LocomotionDecision;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.locomotion.LocomotionSequencer;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.navigation.steering.PathSteeringController;
import dev.traveler.core.navigation.steering.PathSteeringSettings;
import dev.traveler.core.navigation.steering.SteeringPlan;
import dev.traveler.core.settings.TravelerSettings;
import java.util.Objects;

public final class NavigationFramePlanner {
    private static final double CLIMB_UP_JUMP_THRESHOLD =
            TravelerSettings.standard().get(TravelerSettings.CLIMB_UP_JUMP_THRESHOLD);

    private final PathFollowSettings settings;
    private final RouteProgressPolicy routeProgressPolicy;
    private final MovementActionPolicy actionPolicy;
    private final PathSteeringController steeringController;
    private final ActionSteeringPolicy actionSteeringPolicy;
    private final JumpTraversalController jumpTraversalController;
    private final MovementVectorPolicy movementVectorPolicy;
    private final ActionTimingPolicy actionTimingPolicy;
    private final CameraTargetPolicy cameraTargetPolicy;

    public NavigationFramePlanner(
            PathFollowSettings settings,
            RouteProgressPolicy routeProgressPolicy,
            MovementActionPolicy actionPolicy,
            PathSteeringController steeringController,
            ActionSteeringPolicy actionSteeringPolicy,
            JumpTraversalController jumpTraversalController,
            MovementVectorPolicy movementVectorPolicy,
            ActionTimingPolicy actionTimingPolicy,
            CameraTargetPolicy cameraTargetPolicy) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.routeProgressPolicy = Objects.requireNonNull(routeProgressPolicy, "routeProgressPolicy");
        this.actionPolicy = Objects.requireNonNull(actionPolicy, "actionPolicy");
        this.steeringController = Objects.requireNonNull(steeringController, "steeringController");
        this.actionSteeringPolicy = Objects.requireNonNull(actionSteeringPolicy, "actionSteeringPolicy");
        this.jumpTraversalController = Objects.requireNonNull(jumpTraversalController, "jumpTraversalController");
        this.movementVectorPolicy = Objects.requireNonNull(movementVectorPolicy, "movementVectorPolicy");
        this.actionTimingPolicy = Objects.requireNonNull(actionTimingPolicy, "actionTimingPolicy");
        this.cameraTargetPolicy = Objects.requireNonNull(cameraTargetPolicy, "cameraTargetPolicy");
    }

    public static NavigationFramePlanner standard() {
        return standard(TravelerSettings.standard());
    }

    public static NavigationFramePlanner standard(TravelerSettings travelerSettings) {
        TravelerSettings traveler = Objects.requireNonNull(travelerSettings, "travelerSettings");
        PathFollowSettings settings = traveler.pathFollowSettings();
        PathSteeringSettings steeringSettings = traveler.pathSteeringSettings();
        return new NavigationFramePlanner(
                settings,
                new RouteProgressPolicy(settings.reachedDistance()),
                new MovementActionPolicy(),
                new PathSteeringController(steeringSettings),
                new DefaultActionSteeringPolicy(),
                new JumpTraversalController(traveler.movementVectorSettings()),
                new MovementVectorPolicy(traveler.movementVectorSettings()),
                new ActionTimingPolicy(new LocomotionSequencer(traveler.locomotionSequencerSettings())),
                new CameraTargetPolicy(new CameraTargetPlanner(traveler.cameraTargetSettings())));
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
        WorldPoint actionTarget = path.actionTargetBeforeNode(progress.nextNodeIndex());
        WorldPoint segmentStart = path.nodeAt(progress.nextNodeIndex() - 1);
        MovementActionDecision actionDecision = actionPolicy.decide(
                input.position(),
                segmentStart,
                actionTarget,
                input.motionState(),
                path.actionBeforeNode(progress.nextNodeIndex()));
        LocomotionPlan requestedAction = actionDecision.locomotionPlan();
        SteeringPlan steering = steeringController.plan(
                path,
                input.position(),
                input.motionState(),
                progress.nextNodeIndex());
        SteeringPlan movementSteering = actionDecision.retainActionTarget()
                ? SteeringPlan.seek(actionTarget, input.position().horizontalDistanceTo(actionTarget))
                : actionSteeringPolicy.steeringFor(
                        requestedAction,
                        path,
                        input.position(),
                        progress.nextNodeIndex(),
                        actionTarget,
                        steering);
        MovementVectorIntent movementVector = jumpTraversalController
                .movementVectorFor(
                        path.actionBeforeNode(progress.nextNodeIndex()),
                        actionDecision,
                        input.position(),
                        segmentStart,
                        actionTarget,
                        input.motionState())
                .orElseGet(() -> movementVectorPolicy.plan(
                        input.position(),
                        movementSteering,
                        input.cameraAngles(),
                        requestedAction));
        LocomotionPlan timedRequest = timedRequest(requestedAction, movementVector);
        LocomotionDecision timing = actionTimingPolicy.decide(state, progress, timedRequest, input.motionState());
        ClimbDirection requestedClimbDirection =
                climbDirection(requestedAction.action(), input.position(), actionTarget);
        ActionIntent actionIntent = actionIntent(timing.plan().action(), requestedClimbDirection);
        SpeedIntent speedIntent = speedIntent(path, input.position(), movementVector);
        return new NavigationFramePlan(
                phaseFor(actionIntent, movementVector),
                progress,
                movementTarget(timing.plan(), actionTarget, movementSteering, actionDecision.retainActionTarget()),
                movementVector,
                cameraTargetPolicy.target(path, input.position(), progress, input.cameraAngles()),
                actionIntent,
                speedIntent,
                timing.state(),
                false,
                NavigationSteeringDebug.from(steering));
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
            WorldPoint actionTarget,
            SteeringPlan steering,
            boolean retainActionTarget) {
        if (retainActionTarget) {
            return MovementTarget.follow(actionTarget);
        }
        if (usesSteeringTarget(action.action())) {
            return MovementTarget.follow(steering.steeringTarget());
        }
        return MovementTarget.follow(actionTarget);
    }

    private SpeedIntent speedIntent(
            NavigationPath path,
            WorldPoint position,
            MovementVectorIntent movementVector) {
        double scale = speedScale(position.horizontalDistanceTo(path.lastNode()));
        boolean sprint = scale >= 0.5 && movementVector.mode().forwardAllowed();
        return new SpeedIntent(scale, sprint);
    }

    private static ClimbDirection climbDirection(
            LocomotionAction requestedAction,
            WorldPoint position,
            WorldPoint actionTarget) {
        if (requestedAction != LocomotionAction.CLIMB) {
            return ClimbDirection.NONE;
        }
        if (actionTarget.y() > position.y() + CLIMB_UP_JUMP_THRESHOLD) {
            return ClimbDirection.UP;
        }
        if (actionTarget.y() < position.y() - CLIMB_UP_JUMP_THRESHOLD) {
            return ClimbDirection.DOWN;
        }
        return ClimbDirection.LEVEL;
    }

    private static ActionIntent actionIntent(
            LocomotionAction action,
            ClimbDirection requestedClimbDirection) {
        if (requestedClimbDirection != ClimbDirection.NONE && climbActionCanOverride(action)) {
            return ActionIntent.climb(requestedClimbDirection);
        }
        return ActionIntent.from(action);
    }

    private static boolean climbActionCanOverride(LocomotionAction action) {
        return action == LocomotionAction.CLIMB || action == LocomotionAction.JUMP;
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
        if (!usesSteeringTarget(actionIntent.action())) {
            return NavigationPhase.EXECUTE_ACTION;
        }
        if (!movementVector.specialActionAllowed()) {
            return NavigationPhase.ALIGN;
        }
        if (movementVector.mode().approachPhase()) {
            return NavigationPhase.APPROACH;
        }
        return NavigationPhase.ALIGN;
    }

    private static boolean usesSteeringTarget(LocomotionAction action) {
        return action == LocomotionAction.WALK || action == LocomotionAction.SWIM;
    }
}

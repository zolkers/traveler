package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.spatial.CameraMovementBasis;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.navigation.steering.SteeringPlan;
import java.util.Objects;

public final class MovementVectorPolicy {
    private final MovementVectorSettings settings;

    public MovementVectorPolicy(MovementVectorSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public static MovementVectorPolicy standard() {
        return new MovementVectorPolicy(MovementVectorSettings.standard());
    }

    public MovementVectorIntent plan(
            NavigationPoint position,
            SteeringPlan steering,
            CameraAngles cameraAngles,
            LocomotionPlan actionPlan) {
        NavigationPoint currentPosition = Objects.requireNonNull(position, "position");
        SteeringPlan steeringPlan = Objects.requireNonNull(steering, "steering");
        CameraAngles camera = Objects.requireNonNull(cameraAngles, "cameraAngles");
        LocomotionPlan action = Objects.requireNonNull(actionPlan, "actionPlan");
        MovementVectorDecision decision = vectorDecision(currentPosition, steeringPlan, action);
        PlannedMovementMode mode = modeFor(decision, camera);
        boolean actionAllowed = allowsSpecialAction(action, currentPosition, steeringPlan);
        return new MovementVectorIntent(decision.desiredVector(), mode, actionAllowed);
    }

    private MovementVectorDecision vectorDecision(
            NavigationPoint position,
            SteeringPlan steering,
        LocomotionPlan action) {
        if (shouldRecenter(steering, action)) {
            return new MovementVectorDecision(steering.lateralCorrection(), true);
        }
        if (!steering.tangent().isZero()) {
            return new MovementVectorDecision(steering.desiredVectorFrom(position), false);
        }
        return new MovementVectorDecision(position.horizontalVectorTo(steering.steeringTarget()), false);
    }

    private boolean shouldRecenter(SteeringPlan steering, LocomotionPlan action) {
        return steering.outsideCorridor()
                && !nearEnoughForSpecialAction(action, null, steering)
                && steering.lateralCorrection().length() >= settings.centeringCorrectionThreshold();
    }

    private PlannedMovementMode modeFor(MovementVectorDecision decision, CameraAngles cameraAngles) {
        if (decision.recentering()) {
            return PlannedMovementMode.SIDESTEP_RECENTER;
        }
        if (decision.desiredVector().isZero()) {
            return PlannedMovementMode.WAIT_FOR_CAMERA;
        }
        CameraMovementBasis basis = CameraMovementBasis.fromMinecraftYaw(cameraAngles.yawDegrees());
        HorizontalVector desired = decision.desiredVector().normalized();
        double forwardAmount = desired.dot(basis.forward());
        double sideAmount = Math.abs(desired.dot(basis.right()));
        return modeForAmounts(forwardAmount, sideAmount, decision.desiredVector().length());
    }

    private PlannedMovementMode modeForAmounts(double forwardAmount, double sideAmount, double distance) {
        if (shouldBackpedal(forwardAmount, sideAmount, distance)) {
            return PlannedMovementMode.BACKPEDAL;
        }
        if (shouldForwardArc(forwardAmount, sideAmount)) {
            return PlannedMovementMode.FORWARD_ARC;
        }
        if (forwardAmount >= settings.pressThreshold()) {
            return PlannedMovementMode.DIRECT;
        }
        if (sideAmount >= settings.turnStrafeThreshold()) {
            return PlannedMovementMode.STRAFE_TURN;
        }
        return PlannedMovementMode.WAIT_FOR_CAMERA;
    }

    private boolean shouldForwardArc(double forwardAmount, double sideAmount) {
        return forwardAmount >= settings.forwardArcMinimumForward()
                && sideAmount >= settings.turnStrafeThreshold();
    }

    private boolean shouldBackpedal(double forwardAmount, double sideAmount, double distance) {
        return forwardAmount <= -settings.pressThreshold()
                && sideAmount < settings.turnStrafeThreshold()
                && distance <= settings.backpedalMaximumDistance();
    }

    private boolean allowsSpecialAction(
            LocomotionPlan action,
            NavigationPoint position,
            SteeringPlan steering) {
        if (isContinuousMovement(action.action())) {
            return true;
        }
        return nearEnoughForSpecialAction(action, position, steering);
    }

    private boolean nearEnoughForSpecialAction(
            LocomotionPlan action,
            NavigationPoint position,
            SteeringPlan steering) {
        if (isContinuousMovement(action.action())) {
            return false;
        }
        if (steering.lateralError() <= specialActionLateralTolerance(action.action())) {
            return true;
        }
        if (position == null) {
            return false;
        }
        return actionCanStayCommitted(action.action(), position, steering);
    }

    private double specialActionLateralTolerance(LocomotionAction action) {
        if (action == LocomotionAction.JUMP || action == LocomotionAction.STEP_UP) {
            return settings.jumpActionLateralTolerance();
        }
        return settings.specialActionLateralTolerance();
    }

    private boolean actionCanStayCommitted(
            LocomotionAction action,
            NavigationPoint position,
            SteeringPlan steering) {
        if (action != LocomotionAction.JUMP && action != LocomotionAction.STEP_UP) {
            return false;
        }
        return position.horizontalDistanceTo(steering.steeringTarget()) <= settings.specialActionLateralTolerance();
    }

    private static boolean isContinuousMovement(LocomotionAction action) {
        return action == LocomotionAction.WALK || action == LocomotionAction.SWIM;
    }

    private record MovementVectorDecision(
            HorizontalVector desiredVector,
            boolean recentering) {}
}

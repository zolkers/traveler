package dev.traveler.core.navigation.input;

import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.steering.SteeringPlan;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class MovementInputPlanner {
    private static final double JUMP_HEIGHT_THRESHOLD = 0.25;

    private final MovementInputSettings settings;
    private final MovementVectorSelector vectorSelector;

    public MovementInputPlanner(MovementInputSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.vectorSelector = new MovementVectorSelector();
    }

    public MovementIntent plan(
            NavigationPoint current,
            NavigationPoint target,
            double cameraYawDegrees,
            MovementIntent previousIntent) {
        return plan(
                current,
                target,
                cameraYawDegrees,
                previousIntent,
                LocomotionPlan.walk(),
                AgentMotionState.groundedStill());
    }

    public MovementIntent plan(
            NavigationPoint current,
            NavigationPoint target,
            double cameraYawDegrees,
            MovementIntent previousIntent,
            LocomotionPlan locomotionPlan,
            AgentMotionState motionState) {
        NavigationPoint position = Objects.requireNonNull(current, "current");
        NavigationPoint destination = Objects.requireNonNull(target, "target");
        return plan(
                position,
                SteeringPlan.seek(destination),
                cameraYawDegrees,
                previousIntent,
                locomotionPlan,
                motionState);
    }

    public MovementIntent plan(
            NavigationPoint current,
            SteeringPlan steeringPlan,
            double cameraYawDegrees,
            MovementIntent previousIntent,
            LocomotionPlan locomotionPlan,
            AgentMotionState motionState) {
        NavigationPoint position = Objects.requireNonNull(current, "current");
        SteeringPlan steering = Objects.requireNonNull(steeringPlan, "steeringPlan");
        MovementIntent previous = Objects.requireNonNull(previousIntent, "previousIntent");
        LocomotionPlan plan = Objects.requireNonNull(locomotionPlan, "locomotionPlan");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        if (plan.action() == LocomotionAction.RECOVER || motion.blockedOnGround()) {
            return recoveryIntent(previous);
        }
        NavigationPoint destination = steering.steeringTarget();
        MovementDirective directive = vectorSelector.select(position, steering, settings);
        HorizontalVector desired = directive.desiredVector();
        if (desired.isZero()) {
            return verticalIntent(position, destination, plan, motion);
        }
        return withJump(
                intentFor(desired, cameraYawDegrees, previous),
                plan,
                motion,
                directive.allowsSpecialAction());
    }

    private MovementIntent intentFor(HorizontalVector desired, double yawDegrees, MovementIntent previous) {
        CameraMovementBasis basis = CameraMovementBasis.fromMinecraftYaw(yawDegrees);
        HorizontalVector scaled = desired.normalized().scaled(Math.min(1.0, desired.length()));
        double forwardAmount = scaled.dot(basis.forward());
        double rightAmount = scaled.dot(basis.right());
        TurnInputMode mode = TurnInputMode.select(
                forwardAmount,
                Math.abs(rightAmount),
                desired.length(),
                settings);
        boolean forward = mode.allowsForward() && pressed(forwardAmount, previous.forward());
        boolean back = mode.allowsBack() && pressed(-forwardAmount, previous.back());
        boolean left = mode.allowsStrafe() && pressed(-rightAmount, previous.left());
        boolean right = mode.allowsStrafe() && pressed(rightAmount, previous.right());
        return new MovementIntent(forward, back, left, right, false, forward && !back);
    }

    private boolean pressed(double amount, boolean wasPressed) {
        double threshold = wasPressed ? settings.releaseThreshold() : settings.pressThreshold();
        return amount >= threshold;
    }

    private static MovementIntent verticalIntent(
            NavigationPoint current,
            NavigationPoint target,
            LocomotionPlan plan,
            AgentMotionState motion) {
        boolean jump = shouldJumpVertically(current, target, plan, motion);
        return new MovementIntent(false, false, false, false, jump, false);
    }

    private static boolean shouldJumpVertically(
            NavigationPoint current,
            NavigationPoint target,
            LocomotionPlan plan,
            AgentMotionState motion) {
        if (!motion.onGround()) {
            return false;
        }
        boolean jumpAction = plan.action() == LocomotionAction.WALK || plan.jumpRequested();
        return jumpAction && target.y() - current.y() > JUMP_HEIGHT_THRESHOLD;
    }

    private static MovementIntent withJump(
            MovementIntent intent,
            LocomotionPlan plan,
            AgentMotionState motion,
            boolean allowsSpecialAction) {
        if (!plan.jumpRequested() || !motion.onGround() || !allowsSpecialAction) {
            return intent;
        }
        return new MovementIntent(
                intent.forward(),
                intent.back(),
                intent.left(),
                intent.right(),
                true,
                intent.sprint());
    }

    private static MovementIntent recoveryIntent(MovementIntent previous) {
        boolean right = previous.left();
        boolean left = !right;
        return new MovementIntent(false, true, left, right, false, false);
    }
}

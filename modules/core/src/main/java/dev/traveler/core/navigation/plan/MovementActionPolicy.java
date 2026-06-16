package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Objects;

public final class MovementActionPolicy {
    private static final double JUMP_LANDING_HEIGHT_TOLERANCE = 0.2;
    private static final double JUMP_LANDING_DISTANCE_TOLERANCE = 1.0;
    private static final double JUMP_TAKEOFF_EXIT_DISTANCE = 0.35;
    private static final double JUMP_TAKEOFF_EXIT_HEIGHT = 0.35;

    public LocomotionPlan plan(
            NavigationPoint position,
            NavigationPoint target,
            AgentMotionState motionState) {
        return decide(position, target, motionState, MovementAction.WALK).locomotionPlan();
    }

    public LocomotionPlan plan(
            NavigationPoint position,
            NavigationPoint segmentStart,
            NavigationPoint target,
            AgentMotionState motionState,
            MovementAction segmentAction) {
        return decide(position, segmentStart, target, motionState, segmentAction).locomotionPlan();
    }

    public LocomotionPlan plan(
            NavigationPoint position,
            NavigationPoint target,
            AgentMotionState motionState,
            MovementAction segmentAction) {
        return decide(position, target, motionState, segmentAction).locomotionPlan();
    }

    public MovementActionDecision decide(
            NavigationPoint position,
            NavigationPoint target,
            AgentMotionState motionState,
            MovementAction segmentAction) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(target, "target");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        MovementAction action = Objects.requireNonNull(segmentAction, "segmentAction");
        if (motion.blockedOnGround()) {
            return new MovementActionDecision(LocomotionPlan.recover(), false);
        }
        return new MovementActionDecision(planForAction(action), false);
    }

    public MovementActionDecision decide(
            NavigationPoint position,
            NavigationPoint segmentStart,
            NavigationPoint target,
            AgentMotionState motionState,
            MovementAction segmentAction) {
        NavigationPoint currentPosition = Objects.requireNonNull(position, "position");
        NavigationPoint start = Objects.requireNonNull(segmentStart, "segmentStart");
        NavigationPoint actionTarget = Objects.requireNonNull(target, "target");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        MovementAction action = Objects.requireNonNull(segmentAction, "segmentAction");
        if (motion.blockedOnGround()) {
            return new MovementActionDecision(LocomotionPlan.recover(), false);
        }
        boolean followThrough = isJumpFollowThrough(action, currentPosition, start, actionTarget, motion);
        if (followThrough) {
            return new MovementActionDecision(LocomotionPlan.walk(), true);
        }
        return new MovementActionDecision(planForAction(action), false);
    }

    private static LocomotionPlan planForAction(MovementAction action) {
        return switch (action) {
            case WALK -> LocomotionPlan.walk();
            case STEP_UP -> LocomotionPlan.stepUp();
            case JUMP -> LocomotionPlan.jump();
            case DROP -> LocomotionPlan.drop();
            case SWIM -> LocomotionPlan.swim();
            case CLIMB -> LocomotionPlan.climb();
            case BLOCKED -> LocomotionPlan.recover();
        };
    }

    private static boolean isJumpFollowThrough(
            MovementAction action,
            NavigationPoint position,
            NavigationPoint segmentStart,
            NavigationPoint target,
            AgentMotionState motionState) {
        if (action != MovementAction.JUMP && action != MovementAction.STEP_UP) {
            return false;
        }
        if (!motionState.onGround()
                || position.y() < target.y() - JUMP_LANDING_HEIGHT_TOLERANCE
                || !hasLeftTakeoffZone(position, segmentStart)) {
            return false;
        }
        if (position.horizontalDistanceTo(target) <= JUMP_LANDING_DISTANCE_TOLERANCE) {
            return true;
        }
        double segmentLength = segmentStart.horizontalDistanceTo(target);
        if (segmentLength <= 1.0E-6) {
            return false;
        }
        double along = segmentStart
                .horizontalVectorTo(position)
                .dot(segmentStart.horizontalVectorTo(target).normalized());
        return along >= segmentLength - JUMP_LANDING_DISTANCE_TOLERANCE;
    }

    private static boolean hasLeftTakeoffZone(
            NavigationPoint position,
            NavigationPoint segmentStart) {
        return position.horizontalDistanceTo(segmentStart) >= JUMP_TAKEOFF_EXIT_DISTANCE
                || position.y() >= segmentStart.y() + JUMP_TAKEOFF_EXIT_HEIGHT;
    }
}

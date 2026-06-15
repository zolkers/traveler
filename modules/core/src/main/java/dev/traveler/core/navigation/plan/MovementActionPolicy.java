package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Objects;

public final class MovementActionPolicy {
    public LocomotionPlan plan(
            NavigationPoint position,
            NavigationPoint target,
            AgentMotionState motionState) {
        return plan(position, target, motionState, MovementAction.WALK);
    }

    public LocomotionPlan plan(
            NavigationPoint position,
            NavigationPoint target,
            AgentMotionState motionState,
            MovementAction segmentAction) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(target, "target");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        MovementAction action = Objects.requireNonNull(segmentAction, "segmentAction");
        if (motion.blockedOnGround()) {
            return LocomotionPlan.recover();
        }
        return planForAction(action);
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
}

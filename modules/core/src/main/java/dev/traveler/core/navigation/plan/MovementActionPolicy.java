package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class MovementActionPolicy {
    private static final double STEP_UP_HEIGHT = 0.25;
    private static final double JUMP_HEIGHT = 0.75;
    private static final double DROP_HEIGHT = -0.75;

    public LocomotionPlan plan(
            NavigationPoint position,
            NavigationPoint target,
            AgentMotionState motionState) {
        NavigationPoint currentPosition = Objects.requireNonNull(position, "position");
        NavigationPoint actionTarget = Objects.requireNonNull(target, "target");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        if (motion.blockedOnGround()) {
            return LocomotionPlan.recover();
        }
        return planForHeight(actionTarget.y() - currentPosition.y());
    }

    private static LocomotionPlan planForHeight(double heightDelta) {
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
}

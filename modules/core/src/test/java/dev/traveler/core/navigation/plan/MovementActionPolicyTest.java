package dev.traveler.core.navigation.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import org.junit.jupiter.api.Test;

class MovementActionPolicyTest {
    private final MovementActionPolicy policy = new MovementActionPolicy();

    @Test
    void jumpSegmentStopsRequestingJumpAfterLandingFollowThroughBegins() {
        LocomotionPlan plan = policy.plan(
                point(0.35, 65.0, 0.85),
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 1.0),
                AgentMotionState.groundedStill(),
                MovementAction.JUMP);

        assertEquals(LocomotionPlan.walk(), plan);
    }

    @Test
    void jumpSegmentStillRequestsJumpBeforeLandingHeightIsReached() {
        LocomotionPlan plan = policy.plan(
                point(0.0, 64.0, 0.0),
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 1.0),
                AgentMotionState.groundedStill(),
                MovementAction.JUMP);

        assertEquals(LocomotionPlan.jump(), plan);
    }

    @Test
    void jumpSegmentStillRequestsJumpWhenLandingWasNotReachedOnGround() {
        LocomotionPlan plan = policy.plan(
                point(0.35, 64.0, 0.85),
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 1.0),
                new AgentMotionState(true, false, new HorizontalVector(0.0, 0.0), 0.0),
                MovementAction.JUMP);

        assertEquals(LocomotionPlan.jump(), plan);
    }

    private static NavigationPoint point(double x, double y, double z) {
        return new NavigationPoint(x, y, z);
    }
}

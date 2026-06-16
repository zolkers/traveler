package dev.traveler.core.navigation.steering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathSteeringControllerTest {
    @Test
    void predictsPositionAndTargetsDistanceAheadLikeGdxAiFollowPath() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(2.0, 0.5, 0.35, 1.0, 0.75));
        NavigationPath path = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 8.0)));
        AgentMotionState motion = new AgentMotionState(
                true,
                false,
                new HorizontalVector(0.0, 2.0),
                0.0);

        SteeringPlan plan = controller.plan(path, new WorldPoint(1.0, 64.0, 2.0), motion, 1);

        assertEquals(5.0, plan.distanceOnPath());
        assertEquals(new WorldPoint(0.0, 64.0, 5.0), plan.pathTarget());
        assertTrue(plan.steeringTarget().x() < 0.0);
        assertEquals(new HorizontalVector(0.0, 1.0), plan.tangent());
    }

    @Test
    void keepsTargetOnCenterlineWhenAgentIsInsideCorridor() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(2.0, 0.0, 0.35, 1.0, 0.75));
        NavigationPath path = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 8.0)));

        SteeringPlan plan = controller.plan(
                path,
                new WorldPoint(0.05, 64.0, 2.0),
                AgentMotionState.groundedStill(),
                1);

        assertEquals(new WorldPoint(0.0, 64.0, 4.0), plan.steeringTarget());
        assertEquals(0.05, plan.lateralError());
    }

    @Test
    void appliesSignedLateralCorrectionBeforeLeavingCorridor() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(2.0, 0.0, 0.35, 1.0, 0.75));
        NavigationPath path = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 8.0)));

        SteeringPlan plan = controller.plan(
                path,
                new WorldPoint(0.2, 64.0, 2.0),
                AgentMotionState.groundedStill(),
                1);

        assertEquals(-0.2, plan.signedLateralError());
        assertTrue(plan.lateralCorrection().x() < 0.0);
        assertTrue(plan.steeringTarget().x() < 0.0);
        assertTrue(plan.desiredVectorFrom(new WorldPoint(0.2, 64.0, 2.0)).x() < 0.0);
        assertTrue(plan.desiredVectorFrom(new WorldPoint(0.2, 64.0, 2.0)).z() > 0.0);
    }

    @Test
    void steersBackTowardFinalNodeAfterOvershoot() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(2.0, 0.0, 0.35, 1.0, 0.75));
        NavigationPath path = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 4.0)));
        WorldPoint position = new WorldPoint(0.0, 64.0, 6.0);

        SteeringPlan plan = controller.plan(path, position, AgentMotionState.groundedStill(), 1);

        assertTrue(plan.steeringTarget().z() < position.z());
        assertTrue(plan.desiredVectorFrom(position).z() < 0.0);
    }

    @Test
    void flagsClearanceWarningWhenLateralCorrectionIsHigh() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(2.0, 0.0, 0.35, 1.0, 0.75, 0.65));
        NavigationPath path = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 8.0)));

        SteeringPlan plan = controller.plan(
                path,
                new WorldPoint(1.5, 64.0, 2.0),
                AgentMotionState.groundedStill(),
                1);

        assertTrue(plan.clearanceWarning());
        assertEquals(0.75, plan.lateralCorrection().length());
    }

    @Test
    void dampsLateralCorrectionWhenMotionAlreadyConvergesTowardLine() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(2.0, 0.0, 0.6, 1.0, 1.0, 0.65, 0.08, 0.35, 2.0, 0.0, 2.0));
        NavigationPath path = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 8.0)));
        WorldPoint position = new WorldPoint(0.4, 64.0, 2.0);

        SteeringPlan still = controller.plan(path, position, AgentMotionState.groundedStill(), 1);
        SteeringPlan movingAway = controller.plan(
                path,
                position,
                new AgentMotionState(true, false, new HorizontalVector(1.0, 0.0), 0.0),
                1);
        SteeringPlan movingToward = controller.plan(
                path,
                position,
                new AgentMotionState(true, false, new HorizontalVector(-1.0, 0.0), 0.0),
                1);

        assertTrue(movingAway.lateralCorrection().length() > still.lateralCorrection().length());
        assertTrue(movingToward.lateralCorrection().length() < still.lateralCorrection().length());
    }

    @Test
    void shortensLookaheadWhenLateralErrorIsHigh() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(4.0, 0.0, 0.6, 1.0, 1.0, 0.65, 0.08, 0.0, 1.0, 2.0, 4.0));
        NavigationPath path = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 10.0)));

        SteeringPlan centered = controller.plan(
                path,
                new WorldPoint(0.0, 64.0, 1.0),
                AgentMotionState.groundedStill(),
                1);
        SteeringPlan displaced = controller.plan(
                path,
                new WorldPoint(1.5, 64.0, 1.0),
                AgentMotionState.groundedStill(),
                1);

        assertEquals(5.0, centered.distanceOnPath());
        assertTrue(displaced.distanceOnPath() < centered.distanceOnPath());
        assertTrue(displaced.distanceOnPath() <= 2.2);
    }

    @Test
    void shortensLookaheadBeforeNonWalkAction() {
        PathSteeringController controller = new PathSteeringController(
                new PathSteeringSettings(4.0, 0.0, 0.6, 1.0, 1.0, 0.65, 0.08, 0.0, 1.0, 0.0, 1.25));
        NavigationPath path = NavigationPath.of(
                List.of(
                        new WorldPoint(0.0, 64.0, 0.0),
                        new WorldPoint(0.0, 64.0, 3.0),
                        new WorldPoint(0.0, 65.0, 4.0)),
                List.of(MovementAction.WALK, MovementAction.JUMP));

        SteeringPlan plan = controller.plan(
                path,
                new WorldPoint(0.0, 64.0, 1.0),
                AgentMotionState.groundedStill(),
                1);

        assertTrue(plan.distanceOnPath() < 3.0);
        assertTrue(plan.pathTarget().z() < 3.0);
    }
}

package dev.traveler.core.navigation.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.List;
import org.junit.jupiter.api.Test;

class NavigationFramePlannerTest {
    private final NavigationFramePlanner planner = NavigationFramePlanner.standard();

    @Test
    void jumpActionKeepsActionTargetButLooksAheadInsteadOfTurningBack() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 0.0),
                point(0.0, 65.0, 4.0)),
                List.of(MovementAction.JUMP, MovementAction.WALK));
        NavigationFrameInput input = frameInput(point(0.0, 64.2, 0.3), neutralCamera());

        NavigationFramePlan plan = planner.plan(path, input, NavigationControllerState.start());

        assertEquals(NavigationPhase.EXECUTE_ACTION, plan.phase());
        assertEquals(LocomotionAction.JUMP, plan.actionIntent().action());
        assertTrue(plan.actionIntent().jumpRequested());
        assertEquals(point(0.0, 65.0, 0.0), plan.movementTarget().point());
        assertTrue(plan.cameraTarget().pitchDegrees() < 0.0);
        assertTrue(plan.cameraTarget().pitchDegrees() > -18.0);
        assertEquals(PlannedMovementMode.DIRECT, plan.movementVector().mode());
    }

    @Test
    void explicitRouteActionDrivesLocomotionWithoutHeightGuessing() {
        NavigationPath path = NavigationPath.of(
                List.of(point(0.0, 64.0, 0.0), point(0.0, 64.0, 1.0)),
                List.of(MovementAction.JUMP));

        NavigationFramePlan plan = planner.plan(
                path,
                frameInput(point(0.0, 64.0, 0.0), neutralCamera()),
                NavigationControllerState.start());

        assertEquals(NavigationPhase.EXECUTE_ACTION, plan.phase());
        assertEquals(LocomotionAction.JUMP, plan.actionIntent().action());
    }

    @Test
    void jumpMovementVectorTargetsLandingInsteadOfFollowingPostLandingTurn() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 1.0),
                point(3.0, 65.0, 1.0)),
                List.of(MovementAction.JUMP, MovementAction.WALK));

        NavigationFramePlan plan = planner.plan(
                path,
                frameInput(point(0.0, 64.0, 0.0), neutralCamera()),
                NavigationControllerState.start());

        assertEquals(NavigationPhase.EXECUTE_ACTION, plan.phase());
        assertTrue(plan.actionIntent().jumpRequested());
        assertEquals(new HorizontalVector(0.0, 1.0), plan.movementVector().desiredVector());
    }

    @Test
    void swimActionMovesForwardWhileHoldingSurfaceJump() {
        NavigationPath path = NavigationPath.of(
                List.of(point(0.0, 64.0, 0.0), point(0.0, 64.0, 3.0)),
                List.of(MovementAction.SWIM));

        NavigationFramePlan plan = planner.plan(
                path,
                frameInput(point(0.0, 64.0, 0.0), neutralCamera()),
                NavigationControllerState.start());

        assertEquals(NavigationPhase.APPROACH, plan.phase());
        assertEquals(LocomotionAction.SWIM, plan.actionIntent().action());
        assertTrue(plan.actionIntent().jumpRequested());
        assertEquals(PlannedMovementMode.DIRECT, plan.movementVector().mode());
    }

    @Test
    void climbActionSteersTowardClimbTargetInsteadOfLandingNode() {
        NavigationPoint climbFace = point(1.3, 70.0, 0.5);
        NavigationPath path = NavigationPath.of(
                List.of(point(0.0, 64.0, 0.0), point(1.25, 70.0, 1.25)),
                List.of(MovementAction.CLIMB),
                List.of(climbFace));
        NavigationPoint position = point(0.0, 64.0, 0.0);

        NavigationFramePlan plan = planner.plan(
                path,
                frameInput(position, neutralCamera()),
                NavigationControllerState.start());

        assertEquals(NavigationPhase.ALIGN, plan.phase());
        assertEquals(LocomotionAction.WALK, plan.actionIntent().action());
        assertEquals(ClimbDirection.NONE, plan.actionIntent().climbDirection());
        assertEquals(position.horizontalVectorTo(climbFace), plan.movementVector().desiredVector());
        assertEquals(climbFace, plan.movementTarget().point());
    }

    @Test
    void climbUpRequestsJumpAndClimbDownRequestsDescent() {
        NavigationPoint climbFaceUp = point(1.3, 70.0, 0.5);
        NavigationPath upward = NavigationPath.of(
                List.of(point(0.0, 64.0, 0.0), point(1.25, 70.0, 1.25)),
                List.of(MovementAction.CLIMB),
                List.of(climbFaceUp));
        NavigationPath downward = NavigationPath.of(
                List.of(point(1.25, 70.0, 1.25), point(0.0, 64.0, 0.0)),
                List.of(MovementAction.CLIMB),
                List.of(point(1.3, 64.0, 0.5)));

        NavigationFramePlan up = planner.plan(
                upward,
                frameInput(point(1.3, 64.0, 0.5), neutralCamera()),
                NavigationControllerState.start());
        NavigationFramePlan down = planner.plan(
                downward,
                frameInput(point(1.3, 70.0, 0.5), neutralCamera()),
                NavigationControllerState.start());

        assertTrue(up.actionIntent().jumpRequested());
        assertFalse(down.actionIntent().jumpRequested());
        assertFalse(up.actionIntent().descendRequested());
        assertTrue(down.actionIntent().descendRequested());
        assertEquals(ClimbDirection.UP, up.actionIntent().climbDirection());
        assertEquals(ClimbDirection.DOWN, down.actionIntent().climbDirection());
    }

    @Test
    void climbDownDirectionOverridesLatchedJumpAction() {
        NavigationPath path = NavigationPath.of(
                List.of(point(1.3, 70.0, 0.5), point(1.3, 64.0, 0.5)),
                List.of(MovementAction.CLIMB));
        NavigationControllerState latchedJump = new NavigationControllerState(
                new PathProgress(1),
                new MovementIntent(true, false, false, false, true, true),
                LocomotionExecutionState.settling(2, LocomotionAction.JUMP));

        NavigationFramePlan plan = planner.plan(
                path,
                frameInput(point(1.3, 70.0, 0.5), neutralCamera()),
                latchedJump);

        assertEquals(LocomotionAction.CLIMB, plan.actionIntent().action());
        assertEquals(ClimbDirection.DOWN, plan.actionIntent().climbDirection());
        assertFalse(plan.actionIntent().jumpRequested());
        assertTrue(plan.actionIntent().descendRequested());
    }

    @Test
    void turnStrafeIsChosenByThePlannerForWideTurns() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(4.0, 64.0, -4.0)));

        NavigationFramePlan plan = planner.plan(
                path,
                frameInput(point(0.0, 64.0, 0.0), neutralCamera()),
                NavigationControllerState.start());

        assertEquals(NavigationPhase.ALIGN, plan.phase());
        assertEquals(PlannedMovementMode.STRAFE_TURN, plan.movementVector().mode());
        assertFalse(plan.actionIntent().jumpRequested());
    }

    @Test
    void plannerWaitsForStableGroundBeforeSecondJump() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 1.0),
                point(0.0, 66.0, 2.0)),
                List.of(MovementAction.JUMP, MovementAction.JUMP));
        NavigationFramePlan first = planner.plan(
                path,
                frameInput(point(0.0, 64.0, 0.0), neutralCamera()),
                NavigationControllerState.start());
        NavigationControllerState afterFirst = new NavigationControllerState(
                first.routeProgress(),
                new MovementIntent(true, false, false, false, true, true),
                first.locomotionState());
        NavigationFrameInput landed = frameInput(point(0.0, 65.0, 1.0), neutralCamera());

        NavigationFramePlan firstGroundFrame = planner.plan(path, landed, afterFirst);
        NavigationControllerState afterFirstGround = stateAfter(firstGroundFrame);
        NavigationFramePlan secondGroundFrame = planner.plan(path, landed, afterFirstGround);
        NavigationFramePlan thirdGroundFrame = planner.plan(path, landed, stateAfter(secondGroundFrame));

        assertTrue(first.actionIntent().jumpRequested());
        assertFalse(firstGroundFrame.actionIntent().jumpRequested());
        assertFalse(secondGroundFrame.actionIntent().jumpRequested());
        assertTrue(thirdGroundFrame.actionIntent().jumpRequested());
    }

    @Test
    void recenteringCorridorDisablesSpecialActionUntilTheLineIsRecovered() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 4.0)),
                List.of(MovementAction.JUMP));
        NavigationFrameInput input = frameInput(point(2.0, 64.0, 1.0), neutralCamera());

        NavigationFramePlan plan = planner.plan(path, input, NavigationControllerState.start());

        assertEquals(NavigationPhase.ALIGN, plan.phase());
        assertFalse(plan.actionIntent().jumpRequested());
        assertFalse(plan.movementVector().specialActionAllowed());
    }

    @Test
    void jumpActionHasForgivenessNearTheCorridorBeforeRecentering() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 4.0)),
                List.of(MovementAction.JUMP));
        NavigationFrameInput input = frameInput(point(0.18, 64.0, 1.0), neutralCamera());

        NavigationFramePlan plan = planner.plan(path, input, NavigationControllerState.start());

        assertEquals(NavigationPhase.EXECUTE_ACTION, plan.phase());
        assertTrue(plan.actionIntent().jumpRequested());
        assertTrue(plan.movementVector().specialActionAllowed());
    }

    @Test
    void jumpActionWaitsForCenterlineBeforePressingJump() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 4.0)),
                List.of(MovementAction.JUMP));
        NavigationFrameInput input = frameInput(point(0.45, 64.0, 1.0), neutralCamera());

        NavigationFramePlan plan = planner.plan(path, input, NavigationControllerState.start());

        assertEquals(NavigationPhase.ALIGN, plan.phase());
        assertFalse(plan.actionIntent().jumpRequested());
        assertFalse(plan.movementVector().specialActionAllowed());
    }

    @Test
    void jumpSegmentStopsRejumpingAfterLandingOnTheTargetLevel() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 1.0),
                point(2.0, 65.0, 1.0)),
                List.of(MovementAction.JUMP, MovementAction.WALK));
        NavigationFrameInput input = new NavigationFrameInput(
                point(0.35, 65.0, 0.85),
                neutralCamera(),
                0.016,
                AgentMotionState.groundedStill());

        NavigationFramePlan plan = planner.plan(path, input, NavigationControllerState.start());

        assertFalse(plan.actionIntent().jumpRequested());
    }

    @Test
    void skippedNodeKeepsPlannerLookingForwardOnTheCorridor() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 64.0, 2.0),
                point(0.0, 64.0, 6.0)));
        NavigationFrameInput input = frameInput(point(0.9, 64.0, 4.0), neutralCamera());

        NavigationFramePlan plan = planner.plan(path, input, NavigationControllerState.start());

        assertEquals(2, plan.routeProgress().nextNodeIndex());
        assertTrue(plan.cameraTarget().yawDegrees() <= 45.0);
        assertEquals(PlannedMovementMode.SIDESTEP_RECENTER, plan.movementVector().mode());
    }

    @Test
    void skippedVerticalActionNodeDoesNotPullThePlayerBackToTheJumpPoint() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 0.0),
                point(0.0, 65.0, 4.0)),
                List.of(MovementAction.JUMP, MovementAction.WALK));
        NavigationFrameInput input = frameInput(point(0.2, 65.0, 2.0), neutralCamera());

        NavigationFramePlan plan = planner.plan(path, input, NavigationControllerState.start());

        assertEquals(2, plan.routeProgress().nextNodeIndex());
        assertFalse(plan.actionIntent().jumpRequested());
        assertEquals(PlannedMovementMode.DIRECT, plan.movementVector().mode());
    }

    private static NavigationControllerState stateAfter(NavigationFramePlan plan) {
        return new NavigationControllerState(
                plan.routeProgress(),
                MovementIntent.idle(),
                plan.locomotionState());
    }

    private static NavigationFrameInput frameInput(NavigationPoint position, CameraAngles cameraAngles) {
        return new NavigationFrameInput(position, cameraAngles, 0.016, AgentMotionState.groundedStill());
    }

    private static CameraAngles neutralCamera() {
        return new CameraAngles(0.0, 0.0);
    }

    private static NavigationPoint point(double x, double y, double z) {
        return new NavigationPoint(x, y, z);
    }
}

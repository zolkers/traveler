package dev.traveler.core.navigation.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.NavigationSegmentAction;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class NavigationFramePlannerTest {
    private final NavigationFramePlanner planner = NavigationFramePlanner.standard();

    @Test
    void jumpActionKeepsActionTargetButLooksAheadInsteadOfTurningBack() {
        NavigationPath path = NavigationPath.of(List.of(
                point(0.0, 64.0, 0.0),
                point(0.0, 65.0, 0.0),
                point(0.0, 65.0, 4.0)));
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
                List.of(NavigationSegmentAction.JUMP));

        NavigationFramePlan plan = planner.plan(
                path,
                frameInput(point(0.0, 64.0, 0.0), neutralCamera()),
                NavigationControllerState.start());

        assertEquals(NavigationPhase.EXECUTE_ACTION, plan.phase());
        assertEquals(LocomotionAction.JUMP, plan.actionIntent().action());
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
                point(0.0, 66.0, 2.0)));
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
                point(0.0, 65.0, 4.0)));
        NavigationFrameInput input = frameInput(point(2.0, 64.0, 1.0), neutralCamera());

        NavigationFramePlan plan = planner.plan(path, input, NavigationControllerState.start());

        assertEquals(NavigationPhase.ALIGN, plan.phase());
        assertFalse(plan.actionIntent().jumpRequested());
        assertFalse(plan.movementVector().specialActionAllowed());
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
                point(0.0, 65.0, 4.0)));
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

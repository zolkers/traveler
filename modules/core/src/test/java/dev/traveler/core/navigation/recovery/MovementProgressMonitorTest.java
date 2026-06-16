package dev.traveler.core.navigation.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import dev.traveler.core.navigation.plan.ActionIntent;
import dev.traveler.core.navigation.plan.MovementVectorIntent;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.plan.SpeedIntent;
import dev.traveler.core.navigation.api.RecoveryAction;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.navigation.testing.NavigationDebugFrames;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.List;
import org.junit.jupiter.api.Test;

class MovementProgressMonitorTest {
    private static final MovementIntent MOVING = new MovementIntent(true, false, false, false, false, true);

    @Test
    void reportsStuckWhenMovementIntentDoesNotMovePositionLongEnough() {
        MovementProgressMonitor monitor =
                new MovementProgressMonitor(new MovementHealthSettings(0.05, 0.25, 0.2));

        assertTrue(monitor.update(input(0.0, 0.10), MOVING).isEmpty());
        assertTrue(monitor.update(input(0.0, 0.10), MOVING).isEmpty());

        MovementFailure failure = monitor.update(input(0.0, 0.10), MOVING).orElseThrow();

        assertEquals(MovementFailureKind.STUCK_NO_PROGRESS, failure.kind());
    }

    @Test
    void reportsNoProgressAfterCommandedTicksEvenWhenDeltaTimeDoesNotAdvance() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(tickHealthSettings());
        NavigationPath path = path(MovementAction.WALK,
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(10.0, 64.0, 0.0));
        NavigationControlFrame frame = NavigationDebugFrames.approachFrame(new WorldPoint(10.0, 64.0, 0.0));

        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.0), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.0), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.0), frame).isEmpty());

        MovementFailure failure = monitor.update(path, input(0.0, 64.0, 0.0, 0.0), frame).orElseThrow();

        assertEquals(MovementFailureKind.STUCK_NO_PROGRESS, failure.kind());
    }

    @Test
    void exposesTypedRecoveryActionsForMovementFailures() {
        MovementProgressMonitor monitor =
                new MovementProgressMonitor(new MovementHealthSettings(0.05, 0.25, 0.2));

        assertTrue(monitor.updateRecoveryAction(input(0.0, 0.10), MOVING).isEmpty());
        assertTrue(monitor.updateRecoveryAction(input(0.0, 0.10), MOVING).isEmpty());

        RecoveryAction action = monitor.updateRecoveryAction(input(0.0, 0.10), MOVING).orElseThrow();

        assertEquals(RecoveryAction.REPLAN_SEGMENT, action);
    }

    @Test
    void reportsPathDivergenceSeparatelyFromNoProgress() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.WALK,
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(10.0, 64.0, 0.0));
        NavigationControlFrame frame = NavigationDebugFrames.approachFrame(new WorldPoint(10.0, 64.0, 0.0));

        assertTrue(monitor.update(path, input(0.0, 64.0, 1.1, 0.10), frame).isEmpty());
        MovementFailure failure = monitor.update(path, input(0.0, 64.0, 1.1, 0.10), frame).orElseThrow();

        assertEquals(MovementFailureKind.PATH_DIVERGENCE, failure.kind());
    }

    @Test
    void climbProgressUsesVerticalRouteProgress() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.CLIMB,
                new WorldPoint(0.5, 64.0, 0.5),
                new WorldPoint(0.5, 67.0, 0.5));
        NavigationControlFrame frame = NavigationDebugFrames.approachFrame(new WorldPoint(0.5, 67.0, 0.5));

        assertTrue(monitor.update(path, input(0.5, 64.0, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 64.1, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 64.2, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 64.3, 0.5, 0.10), frame).isEmpty());
    }

    @Test
    void dropProgressAcceptsDescendingYWithoutHorizontalMovement() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.DROP,
                new WorldPoint(0.5, 66.0, 0.5),
                new WorldPoint(0.5, 64.0, 0.5));
        NavigationControlFrame frame = NavigationDebugFrames.approachFrame(new WorldPoint(0.5, 64.0, 0.5));

        assertTrue(monitor.update(path, input(0.5, 66.0, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 65.9, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 65.8, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 65.7, 0.5, 0.10), frame).isEmpty());
    }

    @Test
    void jumpSetupWaitsForSetupTimeoutBeforeRecovery() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.JUMP,
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(1.0, 65.0, 0.0));
        NavigationControlFrame frame = frame(
                PathProgress.start(),
                NavigationPhase.ALIGN,
                ActionIntent.jump(),
                new WorldPoint(1.0, 65.0, 0.0),
                LocomotionExecutionState.start());

        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());

        MovementFailure failure = monitor.update(path, input(0.0, 64.0, 0.0, 0.20), frame).orElseThrow();
        assertEquals(MovementFailureKind.ACTION_SETUP_TIMEOUT, failure.kind());
    }

    @Test
    void jumpCommitDoesNotTriggerNoProgressRecoveryWhileAirborne() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.JUMP,
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(1.0, 65.0, 0.0));
        NavigationControlFrame frame = frame(
                PathProgress.start(),
                NavigationPhase.EXECUTE_ACTION,
                ActionIntent.jump(),
                new WorldPoint(1.0, 65.0, 0.0),
                LocomotionExecutionState.settling(2, LocomotionAction.JUMP));
        NavigationFrameInput airborne = input(0.3, 64.5, 0.0, 0.20, false, 0.05, 0.18);

        assertTrue(monitor.update(path, airborne, frame).isEmpty());
        assertTrue(monitor.update(path, airborne, frame).isEmpty());
        assertTrue(monitor.update(path, airborne, frame).isEmpty());
    }

    @Test
    void jumpLandingFollowThroughUsesTargetDistanceProgress() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.JUMP,
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 65.0, 1.0));
        NavigationControlFrame frame = frame(
                PathProgress.start(),
                NavigationPhase.APPROACH,
                ActionIntent.none(),
                new WorldPoint(0.0, 65.0, 1.0),
                LocomotionExecutionState.start());

        assertTrue(monitor.update(path, input(0.70, 65.0, 1.20, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.45, 65.0, 1.05, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.20, 65.0, 0.95, 0.10), frame).isEmpty());
    }

    @Test
    void segmentChangeResetsProgressEvenWhenActionTypeDoesNotChange() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = NavigationPath.of(List.of(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(1.0, 64.0, 0.0),
                new WorldPoint(2.0, 64.0, 0.0)),
                List.of(MovementAction.WALK, MovementAction.WALK));
        NavigationControlFrame firstSegment = frame(
                PathProgress.start(),
                NavigationPhase.APPROACH,
                ActionIntent.none(),
                new WorldPoint(1.0, 64.0, 0.0),
                LocomotionExecutionState.start());
        NavigationControlFrame secondSegment = frame(
                new PathProgress(2),
                NavigationPhase.APPROACH,
                ActionIntent.none(),
                new WorldPoint(2.0, 64.0, 0.0),
                LocomotionExecutionState.start());

        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.20), firstSegment).isEmpty());
        assertTrue(monitor.update(path, input(1.0, 64.0, 0.0, 0.10), secondSegment).isEmpty());
    }

    @Test
    void alignPhaseWaitsForActionSetupTimeoutInsteadOfNoProgressRecovery() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.WALK,
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(10.0, 64.0, 0.0));
        NavigationControlFrame frame = frame(NavigationPhase.ALIGN);

        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());

        MovementFailure failure = monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).orElseThrow();
        assertEquals(MovementFailureKind.ACTION_SETUP_TIMEOUT, failure.kind());
    }

    @Test
    void recoverPhaseDoesNotTriggerNoProgressRecoveryLoop() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.WALK,
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(10.0, 64.0, 0.0));
        NavigationControlFrame frame = frame(NavigationPhase.RECOVER);

        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
    }

    @Test
    void clearsStuckTimerWhenPositionProgresses() {
        MovementProgressMonitor monitor =
                new MovementProgressMonitor(new MovementHealthSettings(0.05, 0.25, 0.2));

        assertTrue(monitor.update(input(0.0, 0.10), MOVING).isEmpty());
        assertTrue(monitor.update(input(0.1, 0.10), MOVING).isEmpty());
        assertTrue(monitor.update(input(0.1, 0.10), MOVING).isEmpty());

        assertTrue(monitor.update(input(0.1, 0.10), MOVING).isEmpty());
    }

    private static NavigationFrameInput input(double x, double deltaSeconds) {
        return input(x, 64.0, 0.0, deltaSeconds);
    }

    private static NavigationFrameInput input(double x, double y, double z, double deltaSeconds) {
        return input(x, y, z, deltaSeconds, true, 0.0, 0.0);
    }

    private static NavigationFrameInput input(
            double x,
            double y,
            double z,
            double deltaSeconds,
            boolean onGround,
            double horizontalSpeed,
            double verticalVelocity) {
        return new NavigationFrameInput(
                new WorldPoint(x, y, z),
                new CameraAngles(0.0, 0.0),
                deltaSeconds,
                new AgentMotionState(
                        onGround,
                        false,
                        new HorizontalVector(horizontalSpeed, 0.0),
                        verticalVelocity));
    }

    private static NavigationPath path(MovementAction action, WorldPoint first, WorldPoint second) {
        return NavigationPath.of(List.of(first, second), List.of(action));
    }

    private static NavigationControlFrame frame(NavigationPhase phase) {
        return frame(
                PathProgress.start(),
                phase,
                phase == NavigationPhase.RECOVER ? ActionIntent.recover() : ActionIntent.none(),
                new WorldPoint(10.0, 64.0, 0.0),
                LocomotionExecutionState.start());
    }

    private static NavigationControlFrame frame(
            PathProgress progress,
            NavigationPhase phase,
            ActionIntent actionIntent,
            WorldPoint targetPoint,
            LocomotionExecutionState locomotionState) {
        MovementIntent intent = new MovementIntent(true, false, false, false, false, true);
        MovementTarget target = MovementTarget.follow(targetPoint);
        NavigationFramePlan plan = new NavigationFramePlan(
                phase,
                progress,
                target,
                new MovementVectorIntent(new HorizontalVector(1.0, 0.0), PlannedMovementMode.DIRECT, true),
                new CameraAngles(0.0, 0.0),
                actionIntent,
                new SpeedIntent(1.0, true),
                locomotionState,
                false);
        return new NavigationControlFrame(
                new NavigationControllerState(progress, intent, locomotionState),
                intent,
                new CameraAngles(0.0, 0.0),
                target,
                plan,
                false);
    }

    private static MovementHealthSettings healthSettings() {
        return new MovementHealthSettings(
                0.15,
                0.25,
                0.2,
                0.75,
                0.15,
                0.50,
                0.35);
    }

    private static MovementHealthSettings tickHealthSettings() {
        return new MovementHealthSettings(
                0.15,
                99.0,
                0.2,
                1.2,
                99.0,
                99.0,
                0.35,
                3,
                20,
                40,
                7);
    }
}

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
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import dev.traveler.core.navigation.plan.ActionIntent;
import dev.traveler.core.navigation.plan.MovementVectorIntent;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.plan.SpeedIntent;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.navigation.spatial.HorizontalVector;
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
    void reportsPathDivergenceSeparatelyFromNoProgress() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.WALK,
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(10.0, 64.0, 0.0));
        NavigationControlFrame frame = NavigationDebugFrames.approachFrame(new NavigationPoint(10.0, 64.0, 0.0));

        assertTrue(monitor.update(path, input(0.0, 64.0, 1.1, 0.10), frame).isEmpty());
        MovementFailure failure = monitor.update(path, input(0.0, 64.0, 1.1, 0.10), frame).orElseThrow();

        assertEquals(MovementFailureKind.PATH_DIVERGENCE, failure.kind());
    }

    @Test
    void climbProgressUsesVerticalRouteProgress() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.CLIMB,
                new NavigationPoint(0.5, 64.0, 0.5),
                new NavigationPoint(0.5, 67.0, 0.5));
        NavigationControlFrame frame = NavigationDebugFrames.approachFrame(new NavigationPoint(0.5, 67.0, 0.5));

        assertTrue(monitor.update(path, input(0.5, 64.0, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 64.1, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 64.2, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 64.3, 0.5, 0.10), frame).isEmpty());
    }

    @Test
    void dropProgressAcceptsDescendingYWithoutHorizontalMovement() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.DROP,
                new NavigationPoint(0.5, 66.0, 0.5),
                new NavigationPoint(0.5, 64.0, 0.5));
        NavigationControlFrame frame = NavigationDebugFrames.approachFrame(new NavigationPoint(0.5, 64.0, 0.5));

        assertTrue(monitor.update(path, input(0.5, 66.0, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 65.9, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 65.8, 0.5, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.5, 65.7, 0.5, 0.10), frame).isEmpty());
    }

    @Test
    void jumpSetupHasGraceBeforeStuckRecovery() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.JUMP,
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(1.0, 65.0, 0.0));
        NavigationControlFrame frame = NavigationDebugFrames.approachFrame(new NavigationPoint(1.0, 65.0, 0.0));

        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());
        assertTrue(monitor.update(path, input(0.0, 64.0, 0.0, 0.10), frame).isEmpty());

        MovementFailure failure = monitor.update(path, input(0.0, 64.0, 0.0, 0.20), frame).orElseThrow();
        assertEquals(MovementFailureKind.STUCK_NO_PROGRESS, failure.kind());
    }

    @Test
    void alignPhaseWaitsForActionSetupTimeoutInsteadOfNoProgressRecovery() {
        MovementProgressMonitor monitor = new MovementProgressMonitor(healthSettings());
        NavigationPath path = path(MovementAction.WALK,
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(10.0, 64.0, 0.0));
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
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(10.0, 64.0, 0.0));
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
        return new NavigationFrameInput(
                new NavigationPoint(x, y, z),
                new CameraAngles(0.0, 0.0),
                deltaSeconds);
    }

    private static NavigationPath path(MovementAction action, NavigationPoint first, NavigationPoint second) {
        return NavigationPath.of(List.of(first, second), List.of(action));
    }

    private static NavigationControlFrame frame(NavigationPhase phase) {
        MovementIntent intent = new MovementIntent(true, false, false, false, false, true);
        MovementTarget target = MovementTarget.follow(new NavigationPoint(10.0, 64.0, 0.0));
        NavigationFramePlan plan = new NavigationFramePlan(
                phase,
                PathProgress.start(),
                target,
                new MovementVectorIntent(new HorizontalVector(1.0, 0.0), PlannedMovementMode.DIRECT, true),
                new CameraAngles(0.0, 0.0),
                phase == NavigationPhase.RECOVER ? ActionIntent.recover() : ActionIntent.none(),
                new SpeedIntent(1.0, true),
                LocomotionExecutionState.start(),
                false);
        return new NavigationControlFrame(
                new NavigationControllerState(PathProgress.start(), intent, LocomotionExecutionState.start()),
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
}

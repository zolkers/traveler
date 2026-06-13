package dev.traveler.core.navigation.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAimSettings;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import dev.traveler.core.navigation.plan.ActionIntent;
import dev.traveler.core.navigation.plan.MovementVectorIntent;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.plan.SpeedIntent;
import dev.traveler.core.navigation.plan.ToleranceProfile;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import org.junit.jupiter.api.Test;

class ControlProjectorTest {
    private final ControlProjector projector = ControlProjector.standard();

    @Test
    void doesNotInferJumpFromVerticalMovementTarget() {
        NavigationFramePlan plan = plan(
                new MovementVectorIntent(new HorizontalVector(0.0, 0.0), PlannedMovementMode.WAIT_FOR_CAMERA, true),
                ActionIntent.none(),
                new NavigationPoint(0.0, 65.0, 0.0));

        ControlProjectionFrame frame = projector.project(
                plan,
                frameInput(new CameraAngles(0.0, 0.0)),
                MovementIntent.idle());

        assertFalse(frame.intent().jump());
    }

    @Test
    void mapsTurnStrafePlanToStrafeOnly() {
        NavigationFramePlan plan = plan(
                new MovementVectorIntent(new HorizontalVector(4.0, -4.0), PlannedMovementMode.TURN_STRAFE, true),
                ActionIntent.none(),
                new NavigationPoint(4.0, 64.0, -4.0));

        ControlProjectionFrame frame = projector.project(
                plan,
                frameInput(new CameraAngles(0.0, 0.0)),
                MovementIntent.idle());

        assertTrue(frame.intent().left());
        assertFalse(frame.intent().forward());
        assertFalse(frame.intent().back());
    }

    @Test
    void appliesJumpOnlyWhenTheFramePlanRequestsIt() {
        NavigationFramePlan plan = plan(
                new MovementVectorIntent(new HorizontalVector(0.0, 1.0), PlannedMovementMode.DIRECT, true),
                ActionIntent.jump(),
                new NavigationPoint(0.0, 65.0, 1.0));

        ControlProjectionFrame frame = projector.project(
                plan,
                frameInput(new CameraAngles(0.0, 0.0)),
                MovementIntent.idle());

        assertTrue(frame.intent().forward());
        assertTrue(frame.intent().jump());
    }

    @Test
    void projectsKeysAgainstTheCameraThatWillBeAppliedThisFrame() {
        ControlProjector fastProjector = ControlProjector.standard(
                new CameraAimSettings(10_000.0, 10_000.0, 100.0, 0.0, 180.0));
        NavigationFramePlan plan = plan(
                new MovementVectorIntent(new HorizontalVector(-1.0, 0.0), PlannedMovementMode.DIRECT, true),
                ActionIntent.none(),
                new NavigationPoint(-1.0, 64.0, 0.0),
                new CameraAngles(90.0, 0.0));

        ControlProjectionFrame frame = fastProjector.project(
                plan,
                frameInput(new CameraAngles(0.0, 0.0), 0.1),
                MovementIntent.idle());

        assertTrue(frame.intent().forward());
        assertFalse(frame.intent().left());
        assertFalse(frame.intent().right());
    }

    @Test
    void rotatesCameraAcrossYawWrapWithoutChoosingALongArc() {
        NavigationFramePlan plan = plan(
                MovementVectorIntent.idle(),
                ActionIntent.none(),
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(-179.0, 0.0));

        ControlProjectionFrame frame = projector.project(
                plan,
                frameInput(new CameraAngles(179.0, 0.0)),
                MovementIntent.idle());

        double delta = CameraAngles.shortestYawDelta(179.0, frame.cameraAngles().yawDegrees());
        assertTrue(Math.abs(delta) < 2.0);
    }

    @Test
    void keepsPressedKeysInsideReleaseDeadzone() {
        NavigationFramePlan plan = plan(
                new MovementVectorIntent(new HorizontalVector(0.0, 0.2), PlannedMovementMode.DIRECT, true),
                ActionIntent.none(),
                new NavigationPoint(0.0, 64.0, 0.2));
        MovementIntent previous = new MovementIntent(true, false, false, false, false, true);

        ControlProjectionFrame frame = projector.project(
                plan,
                frameInput(new CameraAngles(0.0, 0.0)),
                previous);

        assertEquals(previous, frame.intent());
    }

    private static NavigationFramePlan plan(
            MovementVectorIntent movementVector,
            ActionIntent actionIntent,
            NavigationPoint movementTarget) {
        return plan(movementVector, actionIntent, movementTarget, new CameraAngles(0.0, 0.0));
    }

    private static NavigationFramePlan plan(
            MovementVectorIntent movementVector,
            ActionIntent actionIntent,
            NavigationPoint movementTarget,
            CameraAngles cameraTarget) {
        return new NavigationFramePlan(
                NavigationPhase.APPROACH,
                PathProgress.start(),
                MovementTarget.follow(movementTarget),
                movementVector,
                cameraTarget,
                actionIntent,
                new SpeedIntent(1.0, true),
                ToleranceProfile.standard(),
                LocomotionExecutionState.start(),
                false);
    }

    private static NavigationFrameInput frameInput(CameraAngles cameraAngles) {
        return frameInput(cameraAngles, 0.016);
    }

    private static NavigationFrameInput frameInput(CameraAngles cameraAngles, double deltaSeconds) {
        return new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                cameraAngles,
                deltaSeconds,
                AgentMotionState.groundedStill());
    }
}

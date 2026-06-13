package dev.traveler.core.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.camera.CameraAimSettings;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.input.MovementIntent;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class NavigationControllerTest {
    private final NavigationController controller = NavigationController.standard();

    @Test
    void producesCameraSmoothedCameraRelativeMovementIntent() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(-4.0, 64.0, 6.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                controller.update(path, input, NavigationControllerState.start());

        assertEquals(new MovementIntent(true, false, false, true, false, true), frame.intent());
        assertTrue(frame.cameraAngles().yawDegrees() > 0.0);
        assertFalse(frame.completed());
    }

    @Test
    void returnsIdleWhenPathIsAlreadyCompleted() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.1, 64.0, 0.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.1, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                controller.update(path, input, NavigationControllerState.start());

        assertEquals(MovementIntent.idle(), frame.intent());
        assertTrue(frame.completed());
    }

    @Test
    void customSettingsCanSlowCameraForPreviewableControl() {
        NavigationController slowController = NavigationController.standard(CameraAimSettings.preview());
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(8.0, 64.0, 0.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                slowController.update(path, input, NavigationControllerState.start());

        assertTrue(Math.abs(frame.cameraAngles().yawDegrees()) < 10.0);
    }

    @Test
    void keepsForwardAndJumpPressedForOneBlockRise() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 1.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                controller.update(path, input, NavigationControllerState.start());

        assertTrue(frame.intent().forward());
        assertTrue(frame.intent().jump());
        assertEquals(NavigationPhase.EXECUTE_ACTION, frame.plan().phase());
        assertEquals(PlannedMovementMode.DIRECT, frame.plan().movementVector().mode());
    }

    @Test
    void keepsCameraPitchNeutralDuringJumpLookahead() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 0.0),
                new NavigationPoint(0.0, 65.0, 4.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                controller.update(path, input, NavigationControllerState.start());

        assertEquals(0.0, frame.cameraAngles().pitchDegrees());
    }

    @Test
    void keepsCameraYawWhenActionHasNoHorizontalLookahead() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 0.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(90.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                controller.update(path, input, NavigationControllerState.start());

        assertEquals(90.0, frame.cameraAngles().yawDegrees());
    }

    @Test
    void keepsJumpPressedAcrossRenderFramesUntilMinecraftTickCanConsumeIt() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 1.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.004,
                AgentMotionState.groundedStill());

        NavigationControlFrame first = controller.update(path, input, NavigationControllerState.start());
        NavigationControlFrame second = controller.update(path, input, first.state());

        assertTrue(first.intent().jump());
        assertTrue(second.intent().jump());
    }

    @Test
    void anticipatesAfterJumpInsteadOfTurningBackToActionNode() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 0.0),
                new NavigationPoint(0.0, 65.0, 4.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.2, 0.3),
                new CameraAngles(0.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                controller.update(path, input, NavigationControllerState.start());

        assertTrue(frame.intent().forward());
        assertTrue(frame.intent().jump());
        assertFalse(frame.intent().back());
        assertEquals(new NavigationPoint(0.0, 65.0, 0.0), frame.movementTarget().point());
    }

    @Test
    void waitsForStableGroundContactBeforeTriggeringConsecutiveJump() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 1.0),
                new NavigationPoint(0.0, 66.0, 2.0)));
        NavigationControlFrame first = controller.update(
                path,
                new NavigationFrameInput(
                        new NavigationPoint(0.0, 64.0, 0.0),
                        new CameraAngles(0.0, 0.0),
                        0.016),
                NavigationControllerState.start());
        NavigationFrameInput landed = new NavigationFrameInput(
                new NavigationPoint(0.0, 65.0, 1.0),
                new CameraAngles(0.0, 0.0),
                0.016,
                AgentMotionState.groundedStill());

        NavigationControlFrame firstGroundFrame = controller.update(path, landed, first.state());
        NavigationControlFrame secondGroundFrame = controller.update(path, landed, firstGroundFrame.state());
        NavigationControlFrame thirdGroundFrame = controller.update(path, landed, secondGroundFrame.state());

        assertTrue(first.intent().jump());
        assertFalse(firstGroundFrame.intent().jump());
        assertFalse(secondGroundFrame.intent().jump());
        assertTrue(thirdGroundFrame.intent().jump());
    }

    @Test
    void canStrafeBeforeCameraHasFinishedTurning() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(3.0, 64.0, 0.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                controller.update(path, input, NavigationControllerState.start());

        assertTrue(frame.intent().left());
        assertFalse(frame.intent().forward());
    }

    @Test
    void waitsForCameraInsteadOfBackpedalingTowardFarTargetBehind() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, -3.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                controller.update(path, input, NavigationControllerState.start());

        assertEquals(MovementIntent.idle(), frame.intent());
        assertTrue(Math.abs(frame.cameraAngles().yawDegrees()) > 0.0);
    }

    @Test
    void usesTurnStrafeForWideCameraTurn() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(4.0, 64.0, -4.0)));
        NavigationFrameInput input = new NavigationFrameInput(
                new NavigationPoint(0.0, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                0.016);

        NavigationControlFrame frame =
                controller.update(path, input, NavigationControllerState.start());

        assertTrue(frame.intent().left());
        assertFalse(frame.intent().back());
        assertFalse(frame.intent().forward());
    }

    @Test
    void doesNotTriggerJumpWhileStillFallingIntoConsecutiveActionNode() {
        NavigationPath path = NavigationPath.of(List.of(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 65.0, 1.0),
                new NavigationPoint(0.0, 66.0, 2.0)));
        NavigationControlFrame first = controller.update(
                path,
                new NavigationFrameInput(
                        new NavigationPoint(0.0, 64.0, 0.0),
                        new CameraAngles(0.0, 0.0),
                        0.016),
                NavigationControllerState.start());
        NavigationFrameInput falling = new NavigationFrameInput(
                new NavigationPoint(0.0, 65.0, 1.0),
                new CameraAngles(0.0, 0.0),
                0.016,
                new AgentMotionState(false, false, new HorizontalVector(0.0, 0.2), -0.3));

        NavigationControlFrame frame = controller.update(path, falling, first.state());

        assertFalse(frame.intent().jump());
    }
}

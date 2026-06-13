package dev.traveler.core.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.camera.CameraAimSettings;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.input.MovementIntent;
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
}

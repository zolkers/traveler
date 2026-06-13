package dev.traveler.core.navigation.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import org.junit.jupiter.api.Test;

class CameraAimControllerTest {
    @Test
    void rotatesAcrossYawWrapUsingShortestDirection() {
        CameraAimController controller = new CameraAimController(
                new CameraAimSettings(720.0, 360.0, 18.0, 0.01));

        CameraAngles next = controller.update(
                new CameraAngles(179.0, 0.0),
                new CameraAngles(-179.0, 0.0),
                0.016);

        assertTrue(next.yawDegrees() > 179.0 || next.yawDegrees() < -179.0);
        assertTrue(Math.abs(CameraAngles.shortestYawDelta(179.0, next.yawDegrees())) < 2.0);
    }

    @Test
    void snapsTinyYawDeltaToTargetToAvoidCameraBuzz() {
        CameraAimController controller = new CameraAimController(
                new CameraAimSettings(720.0, 360.0, 18.0, 0.05));

        CameraAngles next = controller.update(
                new CameraAngles(15.02, 7.0),
                new CameraAngles(15.0, 7.0),
                0.016);

        assertEquals(new CameraAngles(15.0, 7.0), next);
    }

    @Test
    void clampsAngularVelocityByFrameDelta() {
        CameraAimController controller = new CameraAimController(
                new CameraAimSettings(90.0, 45.0, 18.0, 0.01));

        CameraAngles next = controller.update(
                new CameraAngles(0.0, 0.0),
                new CameraAngles(90.0, 45.0),
                0.25);

        assertTrue(next.yawDegrees() <= 22.5);
        assertTrue(next.pitchDegrees() <= 11.25);
    }

    @Test
    void derivesMinecraftAnglesFromEyeToTarget() {
        CameraAngles target = CameraAimController.targetAngles(
                new NavigationPoint(0.0, 65.6, 0.0),
                new NavigationPoint(0.0, 65.6, 8.0));

        assertEquals(new CameraAngles(0.0, 0.0), target);
    }
}

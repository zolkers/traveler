package dev.traveler.core.navigation.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.navigation.steering.SteeringPlan;
import org.junit.jupiter.api.Test;

class MovementVectorPolicyTest {
    private final MovementVectorPolicy policy = MovementVectorPolicy.standard();

    @Test
    void mediumAngleUsesForwardArcInsteadOfPureDirect() {
        MovementVectorIntent intent = policy.plan(
                point(0.0, 0.0),
                SteeringPlan.seek(point(1.0, 2.0)),
                new CameraAngles(0.0, 0.0),
                LocomotionPlan.walk());

        assertEquals(PlannedMovementMode.FORWARD_ARC, intent.mode());
        assertTrue(intent.desiredVector().x() > 0.0);
        assertTrue(intent.specialActionAllowed());
    }

    @Test
    void wideAngleUsesStrafeTurnWhileCameraCatchesUp() {
        MovementVectorIntent intent = policy.plan(
                point(0.0, 0.0),
                SteeringPlan.seek(point(2.0, -1.0)),
                new CameraAngles(0.0, 0.0),
                LocomotionPlan.walk());

        assertEquals(PlannedMovementMode.STRAFE_TURN, intent.mode());
    }

    @Test
    void outsideCorridorUsesSidestepRecenterAndBlocksSpecialAction() {
        SteeringPlan steering = SteeringPlan.corridor(
                point(0.0, 4.0),
                point(0.0, 4.0),
                point(0.0, 1.0),
                new HorizontalVector(0.0, 1.0),
                new HorizontalVector(1.0, 0.0),
                1.2,
                1.0,
                true);

        MovementVectorIntent intent = policy.plan(
                point(0.0, 1.0),
                steering,
                new CameraAngles(0.0, 0.0),
                LocomotionPlan.jump());

        assertEquals(PlannedMovementMode.SIDESTEP_RECENTER, intent.mode());
        assertEquals(new HorizontalVector(1.0, 0.0), intent.desiredVector());
        assertFalse(intent.specialActionAllowed());
    }

    @Test
    void lateralCorrectionInsideCorridorKeepsForwardMotionWhileStrafingBack() {
        SteeringPlan steering = SteeringPlan.corridor(
                point(-0.42, 4.0),
                point(0.0, 4.0),
                point(0.0, 1.0),
                new HorizontalVector(0.0, 1.0),
                new HorizontalVector(-0.42, 0.0),
                0.5,
                -0.5,
                1.0,
                false);

        MovementVectorIntent intent = policy.plan(
                point(0.5, 1.0),
                steering,
                new CameraAngles(0.0, 0.0),
                LocomotionPlan.walk());

        assertEquals(PlannedMovementMode.FORWARD_ARC, intent.mode());
        assertTrue(intent.desiredVector().x() < 0.0);
        assertTrue(intent.desiredVector().z() > 0.0);
        assertTrue(intent.specialActionAllowed());
    }

    private static WorldPoint point(double x, double z) {
        return new WorldPoint(x, 64.0, z);
    }
}

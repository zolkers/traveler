package dev.traveler.core.navigation.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import org.junit.jupiter.api.Test;

class NavigationFramePlanTest {
    @Test
    void framePlanCarriesTheCompleteNavigationDecisionForOneFrame() {
        NavigationFramePlan plan = new NavigationFramePlan(
                NavigationPhase.EXECUTE_ACTION,
                new PathProgress(2),
                MovementTarget.follow(new NavigationPoint(0.0, 65.0, 1.0)),
                new MovementVectorIntent(new HorizontalVector(0.0, 1.0), PlannedMovementMode.DIRECT, true),
                new CameraAngles(0.0, 0.0),
                ActionIntent.jump(),
                new SpeedIntent(1.0, true),
                ToleranceProfile.standard(),
                false);

        assertEquals(NavigationPhase.EXECUTE_ACTION, plan.phase());
        assertEquals(LocomotionAction.JUMP, plan.actionIntent().action());
        assertTrue(plan.actionIntent().jumpRequested());
        assertTrue(plan.movementVector().specialActionAllowed());
        assertTrue(plan.speedIntent().sprintRequested());
    }

    @Test
    void speedIntentRejectsInvalidScale() {
        assertThrows(IllegalArgumentException.class, () -> new SpeedIntent(1.1, true));
        assertThrows(IllegalArgumentException.class, () -> new SpeedIntent(-0.1, false));
    }

    @Test
    void movementVectorIntentRequiresAMode() {
        HorizontalVector vector = new HorizontalVector(0.0, 1.0);

        assertThrows(NullPointerException.class, () -> new MovementVectorIntent(vector, null, true));
    }
}

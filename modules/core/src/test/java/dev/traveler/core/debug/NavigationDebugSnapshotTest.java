package dev.traveler.core.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
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
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NavigationDebugSnapshotTest {
    @Test
    void copiesNavigationFrameIntoDebugSnapshot() {
        NavigationDebugSnapshot snapshot = NavigationDebugSnapshot.from(frameInput(), frame(), Instant.EPOCH);

        assertEquals(Instant.EPOCH, snapshot.updatedAt());
        assertEquals(NavigationPhase.EXECUTE_ACTION, snapshot.phase());
        assertEquals(LocomotionAction.JUMP, snapshot.actionIntent().action());
        assertEquals(PlannedMovementMode.DIRECT, snapshot.movementMode());
        assertEquals(new NavigationPoint(1.0, 64.0, 2.0), snapshot.agentPosition());
        assertEquals(new NavigationPoint(1.0, 65.0, 3.0), snapshot.movementTarget());
        assertEquals(new HorizontalVector(0.0, 1.0), snapshot.movementVector());
        assertTrue(snapshot.intent().jump());
    }

    @Test
    void formatsDenseChatSummaryWithKeysAndCamera() {
        NavigationDebugSnapshot snapshot = NavigationDebugSnapshot.from(frameInput(), frame(), Instant.EPOCH);

        String summary = DebugTextFormatter.navigationSummary(snapshot);

        assertTrue(summary.contains("phase=EXECUTE_ACTION"));
        assertTrue(summary.contains("action=JUMP"));
        assertTrue(summary.contains("keys=Z+SPACE+SPRINT"));
        assertTrue(summary.contains("target=(1.00,65.00,3.00)"));
        assertTrue(summary.contains("yaw=0.0->12.0"));
    }

    private static NavigationFrameInput frameInput() {
        return new NavigationFrameInput(
                new NavigationPoint(1.0, 64.0, 2.0),
                new CameraAngles(0.0, 0.0),
                0.016);
    }

    private static NavigationControlFrame frame() {
        MovementIntent intent = new MovementIntent(true, false, false, false, true, true);
        NavigationFramePlan plan = new NavigationFramePlan(
                NavigationPhase.EXECUTE_ACTION,
                new PathProgress(2),
                MovementTarget.follow(new NavigationPoint(1.0, 65.0, 3.0)),
                new MovementVectorIntent(new HorizontalVector(0.0, 1.0), PlannedMovementMode.DIRECT, true),
                new CameraAngles(12.0, 0.0),
                ActionIntent.jump(),
                new SpeedIntent(1.0, true),
                ToleranceProfile.standard(),
                LocomotionExecutionState.start(),
                false);
        NavigationControllerState state =
                new NavigationControllerState(new PathProgress(2), intent, LocomotionExecutionState.start());
        return new NavigationControlFrame(
                state,
                intent,
                new CameraAngles(4.0, 0.0),
                MovementTarget.follow(new NavigationPoint(1.0, 65.0, 3.0)),
                plan,
                false);
    }
}

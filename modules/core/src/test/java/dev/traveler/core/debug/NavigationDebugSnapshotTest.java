package dev.traveler.core.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.debug.snapshots.NavigationDebugSnapshot;
import dev.traveler.core.debug.snapshots.PathfinderDebugSnapshot;
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
import dev.traveler.core.navigation.plan.NavigationSteeringDebug;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.plan.SpeedIntent;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.block.BlockPosition;
import java.time.Instant;
import java.util.Optional;
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

    @Test
    void formatsSteeringDebugFieldsForChat() {
        NavigationDebugSnapshot snapshot = NavigationDebugSnapshot.from(
                frameInput(),
                frame(new NavigationSteeringDebug(1.2, true, true)),
                Instant.EPOCH);

        String report = DebugTextFormatter.detailedStatus(Optional.of(snapshot), Optional.empty(), Instant.EPOCH);

        assertTrue(report.contains("actionAllowed=true lateralError=1.20 clearance=warning"));
    }

    @Test
    void formatsPathSummaryForChat() {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        path.addNode(new BlockPosition(0, 64, 0));
        path.addNode(new BlockPosition(1, 64, 0));
        path.setCost(3.5);
        PathfinderDebugSnapshot snapshot = new PathfinderDebugSnapshot(
                new PathfinderResult<>(PathfinderStatus.FOUND, path),
                "path",
                Instant.EPOCH);

        String summary = DebugTextFormatter.pathSummary(snapshot);

        assertEquals("path status=FOUND nodes=2 cost=3.50", summary);
    }

    @Test
    void formatsDetailedChatReportWithAnomalies() {
        PathfinderDebugSnapshot path = new PathfinderDebugSnapshot(
                new PathfinderResult<>(PathfinderStatus.NOT_FOUND, new MutableGraphPath<>()),
                "blocked",
                Instant.EPOCH);

        String report = DebugTextFormatter.detailedStatus(
                Optional.of(anomalousSnapshot()),
                Optional.of(path),
                Instant.EPOCH.plusSeconds(5));

        assertTrue(report.startsWith("traveler debug\n"));
        assertTrue(report.contains("path status=NOT_FOUND nodes=0 cost=0.00"));
        assertTrue(report.contains("nav phase=EXECUTE_ACTION action=JUMP mode=WAIT_FOR_CAMERA progress=2"));
        assertTrue(report.contains("pos=(1.00,64.00,2.00) target=(1.00,65.00,3.00) targetDistance=1.41"));
        assertTrue(report.contains("vector=(0.00,0.00) vectorLength=0.00"));
        assertTrue(report.contains("keys=Z speed=0.20 sprint=false completed=false"));
        assertTrue(report.contains("camera currentYaw=0.0 targetYaw=140.0 outputYaw=4.0 yawLag=140.0"));
        assertTrue(report.contains("anomalies=PATH_NOT_FOUND,STALE_NAVIGATION_DEBUG"));
        assertTrue(report.contains("ZERO_VECTOR_WHILE_NOT_COMPLETED"));
        assertTrue(report.contains("JUMP_REQUESTED_WITHOUT_SPACE"));
    }

    @Test
    void treatsPassiveClimbDownAsIntentionalMovement() {
        DebugReport report = DebugReport.from(
                Optional.of(passiveClimbDownSnapshot()),
                Optional.of(foundPathSnapshot()),
                Instant.EPOCH);

        assertFalse(report.anomalies().contains(DebugAnomaly.NO_MOVEMENT_KEYS_WHILE_ACTIVE));
        assertFalse(report.anomalies().contains(DebugAnomaly.ZERO_VECTOR_WHILE_NOT_COMPLETED));
    }

    @Test
    void formatsMissingDebugStateAsAnomalies() {
        String report = DebugTextFormatter.detailedStatus(Optional.empty(), Optional.empty(), Instant.EPOCH);

        assertTrue(report.contains("nav=none"));
        assertTrue(report.contains("path=none"));
        assertTrue(report.contains("anomalies=NO_NAVIGATION,NO_PATH"));
    }

    private static NavigationFrameInput frameInput() {
        return new NavigationFrameInput(
                new NavigationPoint(1.0, 64.0, 2.0),
                new CameraAngles(0.0, 0.0),
                0.016);
    }

    private static NavigationControlFrame frame() {
        return frame(NavigationSteeringDebug.none());
    }

    private static NavigationControlFrame frame(NavigationSteeringDebug steeringDebug) {
        MovementIntent intent = new MovementIntent(true, false, false, false, true, true);
        NavigationFramePlan plan = new NavigationFramePlan(
                NavigationPhase.EXECUTE_ACTION,
                new PathProgress(2),
                MovementTarget.follow(new NavigationPoint(1.0, 65.0, 3.0)),
                new MovementVectorIntent(new HorizontalVector(0.0, 1.0), PlannedMovementMode.DIRECT, true),
                new CameraAngles(12.0, 0.0),
                ActionIntent.jump(),
                new SpeedIntent(1.0, true),
                LocomotionExecutionState.start(),
                false,
                steeringDebug);
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

    private static NavigationDebugSnapshot anomalousSnapshot() {
        return new NavigationDebugSnapshot(
                Instant.EPOCH,
                new NavigationPoint(1.0, 64.0, 2.0),
                new NavigationPoint(1.0, 65.0, 3.0),
                new HorizontalVector(0.0, 0.0),
                NavigationPhase.EXECUTE_ACTION,
                ActionIntent.jump(),
                PlannedMovementMode.WAIT_FOR_CAMERA,
                new PathProgress(2),
                new CameraAngles(0.0, 0.0),
                new CameraAngles(140.0, 0.0),
                new CameraAngles(4.0, 0.0),
                new MovementIntent(true, false, false, false, false, false),
                new SpeedIntent(0.2, false),
                false);
    }

    private static NavigationDebugSnapshot passiveClimbDownSnapshot() {
        return new NavigationDebugSnapshot(
                Instant.EPOCH,
                new NavigationPoint(1.0, 70.0, 2.0),
                new NavigationPoint(1.0, 64.0, 2.0),
                new HorizontalVector(0.0, 0.0),
                NavigationPhase.EXECUTE_ACTION,
                ActionIntent.climbDown(),
                PlannedMovementMode.WAIT_FOR_CAMERA,
                new PathProgress(2),
                new CameraAngles(0.0, 0.0),
                new CameraAngles(0.0, 0.0),
                new CameraAngles(0.0, 0.0),
                MovementIntent.idle(),
                new SpeedIntent(0.2, false),
                false);
    }

    private static PathfinderDebugSnapshot foundPathSnapshot() {
        MutableGraphPath<BlockPosition> path = new MutableGraphPath<>();
        path.addNode(new BlockPosition(1, 70, 2));
        path.addNode(new BlockPosition(1, 64, 2));
        return new PathfinderDebugSnapshot(
                new PathfinderResult<>(PathfinderStatus.FOUND, path),
                "path",
                Instant.EPOCH);
    }
}

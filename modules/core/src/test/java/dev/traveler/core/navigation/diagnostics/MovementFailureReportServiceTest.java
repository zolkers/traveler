package dev.traveler.core.navigation.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationControllerState;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.NavigationSession;
import dev.traveler.core.navigation.camera.CameraAngles;
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
import dev.traveler.core.navigation.recovery.MovementFailure;
import dev.traveler.core.navigation.recovery.MovementFailureKind;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MovementFailureReportServiceTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void writesExpectedGotMoveAndSixteenCubedBlockScan() throws Exception {
        MovementFailureReportService reporter = new MovementFailureReportService(
                () -> position -> new BlockScanSample(
                        position,
                        "test:block_" + position.x() + "_" + position.y() + "_" + position.z(),
                        "PASSABLE",
                        "AVOID",
                        "AIR"),
                new FileMovementFailureReportSink(temporaryDirectory),
                new MovementFailureReportSettings(16));
        MovementFailureReportContext context = new MovementFailureReportContext(
                new NavigationSession(path(), "active", Instant.EPOCH),
                input(),
                frame(),
                new MovementFailure(MovementFailureKind.PATH_DIVERGENCE, "corridor drift"));

        reporter.report(context);

        List<Path> reports;
        try (java.util.stream.Stream<Path> files = Files.list(temporaryDirectory)) {
            reports = files.toList();
        }
        assertEquals(1, reports.size());
        String report = Files.readString(reports.getFirst());
        assertTrue(report.contains("failure=PATH_DIVERGENCE"));
        assertTrue(report.contains("expected.position=WorldPoint[x=1.0, y=65.0, z=1.0]"));
        assertTrue(report.contains("got.position=WorldPoint[x=1.25, y=64.0, z=1.25]"));
        assertTrue(report.contains("possibleFailedMove=action=JUMP"));
        assertTrue(report.contains("blockScan.size=4096"));
        assertTrue(report.contains("block=-7,56,-7 test:block_-7_56_-7"));
    }

    private static NavigationPath path() {
        return NavigationPath.of(
                List.of(new WorldPoint(0.0, 64.0, 0.0), new WorldPoint(1.0, 65.0, 1.0)),
                List.of(MovementAction.JUMP));
    }

    private static NavigationFrameInput input() {
        return new NavigationFrameInput(
                new WorldPoint(1.25, 64.0, 1.25),
                new CameraAngles(90.0, 15.0),
                0.1);
    }

    private static NavigationControlFrame frame() {
        MovementIntent intent = new MovementIntent(true, false, false, true, true, true);
        MovementTarget target = MovementTarget.follow(new WorldPoint(1.0, 65.0, 1.0));
        PathProgress progress = PathProgress.start();
        NavigationFramePlan plan = new NavigationFramePlan(
                NavigationPhase.EXECUTE_ACTION,
                progress,
                target,
                new MovementVectorIntent(new HorizontalVector(1.0, 1.0), PlannedMovementMode.DIRECT, true),
                new CameraAngles(90.0, 15.0),
                ActionIntent.none(),
                new SpeedIntent(1.0, true),
                LocomotionExecutionState.start(),
                false);
        return new NavigationControlFrame(
                new NavigationControllerState(progress, intent, LocomotionExecutionState.start()),
                intent,
                new CameraAngles(90.0, 15.0),
                target,
                plan,
                false);
    }
}

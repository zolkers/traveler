package dev.traveler.core.navigation.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.spatial.NavigationPoint;
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
    void clearsStuckTimerWhenPositionProgresses() {
        MovementProgressMonitor monitor =
                new MovementProgressMonitor(new MovementHealthSettings(0.05, 0.25, 0.2));

        assertTrue(monitor.update(input(0.0, 0.10), MOVING).isEmpty());
        assertTrue(monitor.update(input(0.1, 0.10), MOVING).isEmpty());
        assertTrue(monitor.update(input(0.1, 0.10), MOVING).isEmpty());

        assertTrue(monitor.update(input(0.1, 0.10), MOVING).isEmpty());
    }

    private static NavigationFrameInput input(double x, double deltaSeconds) {
        return new NavigationFrameInput(
                new NavigationPoint(x, 64.0, 0.0),
                new CameraAngles(0.0, 0.0),
                deltaSeconds);
    }
}

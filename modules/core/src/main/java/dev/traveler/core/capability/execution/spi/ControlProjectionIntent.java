package dev.traveler.core.capability.execution.spi;

import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import java.util.Objects;

public record ControlProjectionIntent(
        HorizontalVector desiredVector,
        PlannedMovementMode movementMode,
        boolean jumpRequested,
        boolean descendRequested,
        boolean sprintRequested,
        CameraAngles cameraTarget) {
    public ControlProjectionIntent {
        Objects.requireNonNull(desiredVector, "desiredVector");
        Objects.requireNonNull(movementMode, "movementMode");
        Objects.requireNonNull(cameraTarget, "cameraTarget");
    }
}

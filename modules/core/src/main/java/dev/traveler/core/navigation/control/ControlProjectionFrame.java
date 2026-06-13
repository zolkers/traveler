package dev.traveler.core.navigation.control;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.input.MovementIntent;
import java.util.Objects;

public record ControlProjectionFrame(MovementIntent intent, CameraAngles cameraAngles) {
    public ControlProjectionFrame {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(cameraAngles, "cameraAngles");
    }
}

package dev.traveler.core.navigation;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public record NavigationFrameInput(
        NavigationPoint position,
        CameraAngles cameraAngles,
        double deltaSeconds) {
    public NavigationFrameInput {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(cameraAngles, "cameraAngles");
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0) {
            throw new IllegalArgumentException("deltaSeconds must be non-negative.");
        }
    }
}

package dev.traveler.core.navigation;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public record NavigationFrameInput(
        NavigationPoint position,
        CameraAngles cameraAngles,
        double deltaSeconds,
        AgentMotionState motionState) {
    public NavigationFrameInput(NavigationPoint position, CameraAngles cameraAngles, double deltaSeconds) {
        this(position, cameraAngles, deltaSeconds, AgentMotionState.groundedStill());
    }

    public NavigationFrameInput {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(cameraAngles, "cameraAngles");
        Objects.requireNonNull(motionState, "motionState");
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0) {
            throw new IllegalArgumentException("deltaSeconds must be non-negative.");
        }
    }
}

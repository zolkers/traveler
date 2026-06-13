package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.locomotion.LocomotionExecutionState;
import java.util.Objects;

public record NavigationFramePlan(
        NavigationPhase phase,
        PathProgress routeProgress,
        MovementTarget movementTarget,
        MovementVectorIntent movementVector,
        CameraAngles cameraTarget,
        ActionIntent actionIntent,
        SpeedIntent speedIntent,
        ToleranceProfile toleranceProfile,
        LocomotionExecutionState locomotionState,
        boolean completed) {
    public NavigationFramePlan {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(routeProgress, "routeProgress");
        Objects.requireNonNull(movementTarget, "movementTarget");
        Objects.requireNonNull(movementVector, "movementVector");
        Objects.requireNonNull(cameraTarget, "cameraTarget");
        Objects.requireNonNull(actionIntent, "actionIntent");
        Objects.requireNonNull(speedIntent, "speedIntent");
        Objects.requireNonNull(toleranceProfile, "toleranceProfile");
        Objects.requireNonNull(locomotionState, "locomotionState");
    }
}

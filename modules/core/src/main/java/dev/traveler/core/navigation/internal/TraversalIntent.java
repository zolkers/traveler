package dev.traveler.core.navigation.internal;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.common.geometry.HorizontalVector;
import java.util.Objects;

public record TraversalIntent(
        HorizontalVector desiredVector,
        PlannedMovementMode movementMode,
        boolean jumpRequested,
        boolean descendRequested,
        boolean sprintRequested,
        CameraAngles cameraTarget) {
    public TraversalIntent {
        Objects.requireNonNull(desiredVector, "desiredVector");
        Objects.requireNonNull(movementMode, "movementMode");
        Objects.requireNonNull(cameraTarget, "cameraTarget");
    }

    public static TraversalIntent from(NavigationFramePlan plan) {
        NavigationFramePlan framePlan = Objects.requireNonNull(plan, "plan");
        return new TraversalIntent(
                framePlan.movementVector().desiredVector(),
                framePlan.movementVector().mode(),
                framePlan.actionIntent().jumpRequested(),
                framePlan.actionIntent().descendRequested(),
                framePlan.speedIntent().sprintRequested(),
                framePlan.cameraTarget());
    }
}

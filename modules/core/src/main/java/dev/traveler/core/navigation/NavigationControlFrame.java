package dev.traveler.core.navigation;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.follow.MovementTarget;
import dev.traveler.core.navigation.input.MovementIntent;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import java.util.Objects;

public record NavigationControlFrame(
        NavigationControllerState state,
        MovementIntent intent,
        CameraAngles cameraAngles,
        MovementTarget movementTarget,
        NavigationFramePlan plan,
        boolean completed) {
    public NavigationControlFrame {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(cameraAngles, "cameraAngles");
        Objects.requireNonNull(movementTarget, "movementTarget");
        Objects.requireNonNull(plan, "plan");
    }
}

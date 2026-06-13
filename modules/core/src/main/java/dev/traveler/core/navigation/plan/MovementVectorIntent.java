package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.spatial.HorizontalVector;
import java.util.Objects;

public record MovementVectorIntent(
        HorizontalVector desiredVector,
        PlannedMovementMode mode,
        boolean specialActionAllowed) {
    public MovementVectorIntent {
        Objects.requireNonNull(desiredVector, "desiredVector");
        Objects.requireNonNull(mode, "mode");
    }

    public static MovementVectorIntent idle() {
        return new MovementVectorIntent(new HorizontalVector(0.0, 0.0), PlannedMovementMode.WAIT_FOR_CAMERA, false);
    }
}

package dev.traveler.core.navigation.follow;

import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public record PathFollowFrame(
        MovementTarget movementTarget,
        PathProgress progress,
        double speedScale,
        LocomotionPlan locomotionPlan,
        boolean completed) {
    public PathFollowFrame(
            MovementTarget movementTarget,
            PathProgress progress,
            double speedScale,
            boolean completed) {
        this(movementTarget, progress, speedScale, LocomotionPlan.walk(), completed);
    }

    public PathFollowFrame {
        Objects.requireNonNull(movementTarget, "movementTarget");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(locomotionPlan, "locomotionPlan");
        if (!Double.isFinite(speedScale) || speedScale < 0.0 || speedScale > 1.0) {
            throw new IllegalArgumentException("speedScale must be between 0 and 1.");
        }
    }

    public NavigationPoint target() {
        return movementTarget.point();
    }
}

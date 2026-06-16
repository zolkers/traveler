package dev.traveler.core.navigation.locomotion;

import dev.traveler.core.common.geometry.HorizontalVector;
import java.util.Objects;

public record AgentMotionState(
        boolean onGround,
        boolean horizontalCollision,
        HorizontalVector horizontalVelocity,
        double verticalVelocity) {
    private static final double BLOCKED_SPEED = 0.03;

    public AgentMotionState {
        Objects.requireNonNull(horizontalVelocity, "horizontalVelocity");
        if (!Double.isFinite(verticalVelocity)) {
            throw new IllegalArgumentException("verticalVelocity must be finite.");
        }
    }

    public static AgentMotionState groundedStill() {
        return new AgentMotionState(true, false, new HorizontalVector(0.0, 0.0), 0.0);
    }

    public double horizontalSpeed() {
        return horizontalVelocity.length();
    }

    public boolean blockedOnGround() {
        return onGround && horizontalCollision && horizontalSpeed() <= BLOCKED_SPEED;
    }
}

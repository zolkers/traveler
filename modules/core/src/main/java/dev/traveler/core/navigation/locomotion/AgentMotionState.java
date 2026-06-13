package dev.traveler.core.navigation.locomotion;

public record AgentMotionState(
        boolean onGround,
        boolean horizontalCollision,
        double horizontalSpeed,
        double verticalVelocity) {
    private static final double BLOCKED_SPEED = 0.03;

    public AgentMotionState {
        if (!Double.isFinite(horizontalSpeed) || horizontalSpeed < 0.0) {
            throw new IllegalArgumentException("horizontalSpeed must be non-negative.");
        }
        if (!Double.isFinite(verticalVelocity)) {
            throw new IllegalArgumentException("verticalVelocity must be finite.");
        }
    }

    public static AgentMotionState groundedStill() {
        return new AgentMotionState(true, false, 0.0, 0.0);
    }

    public boolean blockedOnGround() {
        return onGround && horizontalCollision && horizontalSpeed <= BLOCKED_SPEED;
    }
}

package dev.traveler.core.world.behavior.decision;

import java.util.Objects;

public record MovementDecision(boolean allowed, MovementAction action) {
    private static final MovementDecision BLOCKED = new MovementDecision(false, MovementAction.BLOCKED);
    private static final MovementDecision WALK = new MovementDecision(true, MovementAction.WALK);
    private static final MovementDecision STEP_UP = new MovementDecision(true, MovementAction.STEP_UP);
    private static final MovementDecision DROP = new MovementDecision(true, MovementAction.DROP);
    private static final MovementDecision JUMP = new MovementDecision(true, MovementAction.JUMP);
    private static final MovementDecision SWIM = new MovementDecision(true, MovementAction.SWIM);
    private static final MovementDecision CLIMB = new MovementDecision(true, MovementAction.CLIMB);

    public MovementDecision {
        Objects.requireNonNull(action, "action");
        if (allowed == (action == MovementAction.BLOCKED)) {
            throw new IllegalArgumentException("Movement allowance and action disagree.");
        }
    }

    public static MovementDecision blocked() {
        return BLOCKED;
    }

    public static MovementDecision walk() {
        return WALK;
    }

    public static MovementDecision stepUp() {
        return STEP_UP;
    }

    public static MovementDecision drop() {
        return DROP;
    }

    public static MovementDecision jump() {
        return JUMP;
    }

    public static MovementDecision swim() {
        return SWIM;
    }

    public static MovementDecision climb() {
        return CLIMB;
    }
}

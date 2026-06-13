package dev.traveler.core.navigation.input;

public record MovementIntent(
        boolean forward,
        boolean back,
        boolean left,
        boolean right,
        boolean jump,
        boolean sprint) {
    public MovementIntent {
        if (forward && back) {
            throw new IllegalArgumentException("Forward and back cannot both be active.");
        }
        if (left && right) {
            throw new IllegalArgumentException("Left and right cannot both be active.");
        }
    }

    public static MovementIntent idle() {
        return new MovementIntent(false, false, false, false, false, false);
    }

    public MovementIntent withSprint(boolean enabled) {
        return new MovementIntent(forward, back, left, right, jump, enabled);
    }

    public boolean moving() {
        return forward || back || left || right || jump;
    }
}

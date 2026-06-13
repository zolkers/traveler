package dev.traveler.core.navigation.input;

enum TurnInputMode {
    DIRECT(true, false, true),
    TURN_STRAFE(false, false, true),
    BACKPEDAL(false, true, false),
    WAIT_FOR_CAMERA(false, false, false);

    private final boolean allowsForward;
    private final boolean allowsBack;
    private final boolean allowsStrafe;

    TurnInputMode(boolean allowsForward, boolean allowsBack, boolean allowsStrafe) {
        this.allowsForward = allowsForward;
        this.allowsBack = allowsBack;
        this.allowsStrafe = allowsStrafe;
    }

    boolean allowsForward() {
        return allowsForward;
    }

    boolean allowsBack() {
        return allowsBack;
    }

    boolean allowsStrafe() {
        return allowsStrafe;
    }

    static TurnInputMode select(
            double forwardAmount,
            double sideAmount,
            double desiredDistance,
            MovementInputSettings settings) {
        if (shouldBackpedal(forwardAmount, sideAmount, desiredDistance, settings)) {
            return BACKPEDAL;
        }
        if (forwardAmount > -settings.pressThreshold()) {
            return DIRECT;
        }
        if (sideAmount >= settings.turnStrafeThreshold()) {
            return TURN_STRAFE;
        }
        return WAIT_FOR_CAMERA;
    }

    private static boolean shouldBackpedal(
            double forwardAmount,
            double sideAmount,
            double desiredDistance,
            MovementInputSettings settings) {
        return forwardAmount <= -settings.pressThreshold()
                && sideAmount < settings.turnStrafeThreshold()
                && desiredDistance <= settings.backpedalMaximumDistance();
    }
}

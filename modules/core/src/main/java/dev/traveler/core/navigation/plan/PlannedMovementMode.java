package dev.traveler.core.navigation.plan;

public enum PlannedMovementMode {
    DIRECT(true, false, true),
    FORWARD_ARC(true, true, true),
    STRAFE_TURN(false, true, false),
    SIDESTEP_RECENTER(false, true, false),
    BACKPEDAL(false, false, false),
    WAIT_FOR_CAMERA(false, false, false);

    private final boolean forwardAllowed;
    private final boolean strafeAllowed;
    private final boolean approachPhase;

    PlannedMovementMode(boolean forwardAllowed, boolean strafeAllowed, boolean approachPhase) {
        this.forwardAllowed = forwardAllowed;
        this.strafeAllowed = strafeAllowed;
        this.approachPhase = approachPhase;
    }

    public boolean forwardAllowed() {
        return forwardAllowed;
    }

    public boolean strafeAllowed() {
        return strafeAllowed;
    }

    public boolean approachPhase() {
        return approachPhase;
    }
}

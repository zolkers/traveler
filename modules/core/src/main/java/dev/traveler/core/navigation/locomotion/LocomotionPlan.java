package dev.traveler.core.navigation.locomotion;

import java.util.Objects;

public record LocomotionPlan(LocomotionAction action) {
    private static final LocomotionPlan WALK = new LocomotionPlan(LocomotionAction.WALK);
    private static final LocomotionPlan STEP_UP = new LocomotionPlan(LocomotionAction.STEP_UP);
    private static final LocomotionPlan JUMP = new LocomotionPlan(LocomotionAction.JUMP);
    private static final LocomotionPlan DROP = new LocomotionPlan(LocomotionAction.DROP);
    private static final LocomotionPlan RECOVER = new LocomotionPlan(LocomotionAction.RECOVER);

    public LocomotionPlan {
        Objects.requireNonNull(action, "action");
    }

    public static LocomotionPlan walk() {
        return WALK;
    }

    public static LocomotionPlan stepUp() {
        return STEP_UP;
    }

    public static LocomotionPlan jump() {
        return JUMP;
    }

    public static LocomotionPlan drop() {
        return DROP;
    }

    public static LocomotionPlan recover() {
        return RECOVER;
    }

    public boolean jumpRequested() {
        return action == LocomotionAction.JUMP;
    }
}

package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.locomotion.LocomotionAction;
import java.util.Objects;

public record ActionIntent(
        LocomotionAction action,
        boolean jumpRequested,
        boolean recoveryRequested) {
    public ActionIntent {
        Objects.requireNonNull(action, "action");
    }

    public static ActionIntent none() {
        return new ActionIntent(LocomotionAction.WALK, false, false);
    }

    public static ActionIntent jump() {
        return new ActionIntent(LocomotionAction.JUMP, true, false);
    }

    public static ActionIntent stepUp() {
        return new ActionIntent(LocomotionAction.STEP_UP, false, false);
    }

    public static ActionIntent drop() {
        return new ActionIntent(LocomotionAction.DROP, false, false);
    }

    public static ActionIntent climb() {
        return new ActionIntent(LocomotionAction.CLIMB, false, false);
    }

    public static ActionIntent recover() {
        return new ActionIntent(LocomotionAction.RECOVER, false, true);
    }

    public static ActionIntent from(LocomotionAction action) {
        LocomotionAction movementAction = Objects.requireNonNull(action, "action");
        return switch (movementAction) {
            case JUMP -> jump();
            case STEP_UP -> stepUp();
            case DROP -> drop();
            case CLIMB -> climb();
            case RECOVER -> recover();
            case WALK -> none();
        };
    }
}

package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.locomotion.LocomotionAction;
import java.util.Objects;

public record ActionIntent(
        LocomotionAction action,
        ClimbDirection climbDirection,
        boolean recoveryRequested) {
    public ActionIntent {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(climbDirection, "climbDirection");
        if (action == LocomotionAction.CLIMB && climbDirection == ClimbDirection.NONE) {
            throw new IllegalArgumentException("Climb actions require a climb direction.");
        }
        if (action != LocomotionAction.CLIMB && climbDirection != ClimbDirection.NONE) {
            throw new IllegalArgumentException("Only climb actions can have a climb direction.");
        }
    }

    public static ActionIntent none() {
        return new ActionIntent(LocomotionAction.WALK, ClimbDirection.NONE, false);
    }

    public static ActionIntent jump() {
        return new ActionIntent(LocomotionAction.JUMP, ClimbDirection.NONE, false);
    }

    public static ActionIntent stepUp() {
        return new ActionIntent(LocomotionAction.STEP_UP, ClimbDirection.NONE, false);
    }

    public static ActionIntent drop() {
        return new ActionIntent(LocomotionAction.DROP, ClimbDirection.NONE, false);
    }

    public static ActionIntent swim() {
        return new ActionIntent(LocomotionAction.SWIM, ClimbDirection.NONE, false);
    }

    public static ActionIntent climb() {
        return climb(ClimbDirection.LEVEL);
    }

    public static ActionIntent climb(ClimbDirection direction) {
        ClimbDirection climbDirection = Objects.requireNonNull(direction, "direction");
        if (climbDirection == ClimbDirection.NONE) {
            throw new IllegalArgumentException("Climb direction cannot be NONE.");
        }
        return new ActionIntent(LocomotionAction.CLIMB, climbDirection, false);
    }

    public static ActionIntent climbUp() {
        return climb(ClimbDirection.UP);
    }

    public static ActionIntent climbDown() {
        return climb(ClimbDirection.DOWN);
    }

    public static ActionIntent recover() {
        return new ActionIntent(LocomotionAction.RECOVER, ClimbDirection.NONE, true);
    }

    public boolean jumpRequested() {
        return action == LocomotionAction.JUMP
                || action == LocomotionAction.SWIM
                || climbDirection == ClimbDirection.UP;
    }

    public boolean descendRequested() {
        return climbDirection == ClimbDirection.DOWN;
    }

    public static ActionIntent from(LocomotionAction action) {
        LocomotionAction movementAction = Objects.requireNonNull(action, "action");
        return switch (movementAction) {
            case JUMP -> jump();
            case STEP_UP -> stepUp();
            case DROP -> drop();
            case SWIM -> swim();
            case CLIMB -> climb();
            case RECOVER -> recover();
            case WALK -> none();
        };
    }
}

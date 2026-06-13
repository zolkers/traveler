package dev.traveler.core.navigation.input;

import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class MovementInputPlanner {
    private static final double JUMP_HEIGHT_THRESHOLD = 0.25;

    private final MovementInputSettings settings;

    public MovementInputPlanner(MovementInputSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public MovementIntent plan(
            NavigationPoint current,
            NavigationPoint target,
            double cameraYawDegrees,
            MovementIntent previousIntent) {
        NavigationPoint position = Objects.requireNonNull(current, "current");
        NavigationPoint destination = Objects.requireNonNull(target, "target");
        MovementIntent previous = Objects.requireNonNull(previousIntent, "previousIntent");
        HorizontalVector desired = position.horizontalVectorTo(destination);
        if (desired.isZero()) {
            return verticalIntent(position, destination);
        }
        return intentFor(desired, cameraYawDegrees, previous);
    }

    private MovementIntent intentFor(HorizontalVector desired, double yawDegrees, MovementIntent previous) {
        CameraMovementBasis basis = CameraMovementBasis.fromMinecraftYaw(yawDegrees);
        HorizontalVector scaled = desired.normalized().scaled(Math.min(1.0, desired.length()));
        double forwardAmount = scaled.dot(basis.forward());
        double rightAmount = scaled.dot(basis.right());
        boolean forward = pressed(forwardAmount, previous.forward());
        boolean back = pressed(-forwardAmount, previous.back());
        boolean left = pressed(-rightAmount, previous.left());
        boolean right = pressed(rightAmount, previous.right());
        return new MovementIntent(forward, back, left, right, false, forward && !back);
    }

    private boolean pressed(double amount, boolean wasPressed) {
        double threshold = wasPressed ? settings.releaseThreshold() : settings.pressThreshold();
        return amount >= threshold;
    }

    private static MovementIntent verticalIntent(NavigationPoint current, NavigationPoint target) {
        boolean jump = target.y() - current.y() > JUMP_HEIGHT_THRESHOLD;
        return new MovementIntent(false, false, false, false, jump, false);
    }
}

package dev.traveler.core.navigation.control;

import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAimController;
import dev.traveler.core.navigation.camera.CameraAimSettings;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.internal.TraversalIntent;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.settings.TravelerSettings;
import java.util.Objects;

public final class ControlProjector {
    private final ControlProjectionSettings settings;
    private final CameraAimController cameraAimController;

    public ControlProjector(
            ControlProjectionSettings settings,
            CameraAimController cameraAimController) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.cameraAimController = Objects.requireNonNull(cameraAimController, "cameraAimController");
    }

    public static ControlProjector standard() {
        return standard(TravelerSettings.standard());
    }

    public static ControlProjector standard(TravelerSettings travelerSettings) {
        TravelerSettings settings = Objects.requireNonNull(travelerSettings, "travelerSettings");
        return new ControlProjector(
                settings.controlProjectionSettings(),
                new CameraAimController(settings.cameraAimSettings()));
    }

    public static ControlProjector standard(CameraAimSettings cameraAimSettings) {
        return new ControlProjector(
                ControlProjectionSettings.standard(),
                new CameraAimController(cameraAimSettings));
    }

    public ControlProjectionFrame project(
            NavigationFramePlan plan,
            NavigationFrameInput input,
            MovementIntent previousIntent) {
        NavigationFramePlan framePlan = Objects.requireNonNull(plan, "plan");
        return project(TraversalIntent.from(framePlan), input, previousIntent);
    }

    public ControlProjectionFrame project(
            TraversalIntent intent,
            NavigationFrameInput input,
            MovementIntent previousIntent) {
        TraversalIntent traversalIntent = Objects.requireNonNull(intent, "intent");
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        MovementIntent previous = Objects.requireNonNull(previousIntent, "previousIntent");
        CameraAngles camera = cameraAimController.update(
                frameInput.cameraAngles(),
                traversalIntent.cameraTarget(),
                frameInput.deltaSeconds());
        return new ControlProjectionFrame(intentFor(traversalIntent, camera, previous), camera);
    }

    private MovementIntent intentFor(
            TraversalIntent intent,
            CameraAngles cameraAngles,
            MovementIntent previous) {
        HorizontalVector desired = scaledDesiredVector(intent.desiredVector());
        CameraMovementBasis basis = CameraMovementBasis.fromMinecraftYaw(cameraAngles.yawDegrees());
        double forwardAmount = desired.dot(basis.forward());
        double rightAmount = desired.dot(basis.right());
        boolean forward = forward(intent.movementMode(), forwardAmount, previous);
        boolean back = back(intent.movementMode(), forwardAmount, previous);
        boolean left = left(intent.movementMode(), rightAmount, previous);
        boolean right = right(intent.movementMode(), rightAmount, previous);
        boolean jump = intent.jumpRequested();
        boolean descend = intent.descendRequested();
        boolean sprint = intent.sprintRequested() && forward && !back && !descend;
        return new MovementIntent(forward, back, left, right, jump, sprint);
    }

    private static HorizontalVector scaledDesiredVector(HorizontalVector desiredVector) {
        if (desiredVector.isZero()) {
            return desiredVector;
        }
        return desiredVector.normalized().scaled(Math.min(1.0, desiredVector.length()));
    }

    private boolean forward(PlannedMovementMode mode, double amount, MovementIntent previous) {
        return mode.forwardAllowed() && pressed(amount, previous.forward());
    }

    private boolean back(PlannedMovementMode mode, double amount, MovementIntent previous) {
        return mode == PlannedMovementMode.BACKPEDAL && pressed(-amount, previous.back());
    }

    private boolean left(PlannedMovementMode mode, double amount, MovementIntent previous) {
        return mode.strafeAllowed() && pressed(-amount, previous.left());
    }

    private boolean right(PlannedMovementMode mode, double amount, MovementIntent previous) {
        return mode.strafeAllowed() && pressed(amount, previous.right());
    }

    private boolean pressed(double amount, boolean wasPressed) {
        double threshold = wasPressed ? settings.releaseThreshold() : settings.pressThreshold();
        return amount >= threshold;
    }

}

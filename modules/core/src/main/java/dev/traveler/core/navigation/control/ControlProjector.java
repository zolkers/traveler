package dev.traveler.core.navigation.control;

import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAimController;
import dev.traveler.core.navigation.camera.CameraAimSettings;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.input.MovementIntent;
import dev.traveler.core.navigation.plan.NavigationFramePlan;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.spatial.CameraMovementBasis;
import dev.traveler.core.navigation.spatial.HorizontalVector;
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
        return standard(CameraAimSettings.standard());
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
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        MovementIntent previous = Objects.requireNonNull(previousIntent, "previousIntent");
        CameraAngles camera = cameraAimController.update(
                frameInput.cameraAngles(),
                framePlan.cameraTarget(),
                frameInput.deltaSeconds());
        return new ControlProjectionFrame(intentFor(framePlan, frameInput.cameraAngles(), previous), camera);
    }

    private MovementIntent intentFor(
            NavigationFramePlan plan,
            CameraAngles cameraAngles,
            MovementIntent previous) {
        HorizontalVector desired = scaledDesiredVector(plan.movementVector().desiredVector());
        CameraMovementBasis basis = CameraMovementBasis.fromMinecraftYaw(cameraAngles.yawDegrees());
        double forwardAmount = desired.dot(basis.forward());
        double rightAmount = desired.dot(basis.right());
        boolean forward = forward(plan.movementVector().mode(), forwardAmount, previous);
        boolean back = back(plan.movementVector().mode(), forwardAmount, previous);
        boolean left = left(plan.movementVector().mode(), rightAmount, previous);
        boolean right = right(plan.movementVector().mode(), rightAmount, previous);
        boolean jump = plan.actionIntent().jumpRequested()
                && plan.movementVector().specialActionAllowed();
        boolean sprint = plan.speedIntent().sprintRequested() && forward && !back;
        return new MovementIntent(forward, back, left, right, jump, sprint);
    }

    private static HorizontalVector scaledDesiredVector(HorizontalVector desiredVector) {
        if (desiredVector.isZero()) {
            return desiredVector;
        }
        return desiredVector.normalized().scaled(Math.min(1.0, desiredVector.length()));
    }

    private boolean forward(PlannedMovementMode mode, double amount, MovementIntent previous) {
        return mode == PlannedMovementMode.DIRECT && pressed(amount, previous.forward());
    }

    private boolean back(PlannedMovementMode mode, double amount, MovementIntent previous) {
        return mode == PlannedMovementMode.BACKPEDAL && pressed(-amount, previous.back());
    }

    private boolean left(PlannedMovementMode mode, double amount, MovementIntent previous) {
        return allowsStrafe(mode) && pressed(-amount, previous.left());
    }

    private boolean right(PlannedMovementMode mode, double amount, MovementIntent previous) {
        return allowsStrafe(mode) && pressed(amount, previous.right());
    }

    private boolean pressed(double amount, boolean wasPressed) {
        double threshold = wasPressed ? settings.releaseThreshold() : settings.pressThreshold();
        return amount >= threshold;
    }

    private static boolean allowsStrafe(PlannedMovementMode mode) {
        return mode == PlannedMovementMode.DIRECT || mode == PlannedMovementMode.TURN_STRAFE;
    }
}

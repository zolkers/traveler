package dev.traveler.core.debug.snapshots;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.NavigationFrameInput;
import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.plan.ActionIntent;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.navigation.plan.PlannedMovementMode;
import dev.traveler.core.navigation.plan.SpeedIntent;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.time.Instant;
import java.util.Objects;

public record NavigationDebugSnapshot(
        Instant updatedAt,
        NavigationPoint agentPosition,
        NavigationPoint movementTarget,
        HorizontalVector movementVector,
        NavigationPhase phase,
        ActionIntent actionIntent,
        PlannedMovementMode movementMode,
        PathProgress routeProgress,
        CameraAngles currentCamera,
        CameraAngles cameraTarget,
        CameraAngles outputCamera,
        MovementIntent intent,
        SpeedIntent speedIntent,
        boolean completed,
        boolean specialActionAllowed,
        double lateralError,
        boolean clearanceWarning) {
    public NavigationDebugSnapshot(
            Instant updatedAt,
            NavigationPoint agentPosition,
            NavigationPoint movementTarget,
            HorizontalVector movementVector,
            NavigationPhase phase,
            ActionIntent actionIntent,
            PlannedMovementMode movementMode,
            PathProgress routeProgress,
            CameraAngles currentCamera,
            CameraAngles cameraTarget,
            CameraAngles outputCamera,
            MovementIntent intent,
            SpeedIntent speedIntent,
            boolean completed) {
        this(
                updatedAt,
                agentPosition,
                movementTarget,
                movementVector,
                phase,
                actionIntent,
                movementMode,
                routeProgress,
                currentCamera,
                cameraTarget,
                outputCamera,
                intent,
                speedIntent,
                completed,
                false,
                0.0,
                false);
    }

    public NavigationDebugSnapshot {
        Objects.requireNonNull(updatedAt, "updatedAt");
        Objects.requireNonNull(agentPosition, "agentPosition");
        Objects.requireNonNull(movementTarget, "movementTarget");
        Objects.requireNonNull(movementVector, "movementVector");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(actionIntent, "actionIntent");
        Objects.requireNonNull(movementMode, "movementMode");
        Objects.requireNonNull(routeProgress, "routeProgress");
        Objects.requireNonNull(currentCamera, "currentCamera");
        Objects.requireNonNull(cameraTarget, "cameraTarget");
        Objects.requireNonNull(outputCamera, "outputCamera");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(speedIntent, "speedIntent");
        if (!Double.isFinite(lateralError) || lateralError < 0.0) {
            throw new IllegalArgumentException("lateralError must be non-negative.");
        }
    }

    public static NavigationDebugSnapshot from(
            NavigationFrameInput input,
            NavigationControlFrame frame,
            Instant updatedAt) {
        NavigationFrameInput frameInput = Objects.requireNonNull(input, "input");
        NavigationControlFrame controlFrame = Objects.requireNonNull(frame, "frame");
        return new NavigationDebugSnapshot(
                Objects.requireNonNull(updatedAt, "updatedAt"),
                frameInput.position(),
                controlFrame.movementTarget().point(),
                controlFrame.plan().movementVector().desiredVector(),
                controlFrame.plan().phase(),
                controlFrame.plan().actionIntent(),
                controlFrame.plan().movementVector().mode(),
                controlFrame.plan().routeProgress(),
                frameInput.cameraAngles(),
                controlFrame.plan().cameraTarget(),
                controlFrame.cameraAngles(),
                controlFrame.intent(),
                controlFrame.plan().speedIntent(),
                controlFrame.completed(),
                controlFrame.plan().movementVector().specialActionAllowed(),
                controlFrame.plan().steeringDebug().lateralError(),
                controlFrame.plan().steeringDebug().clearanceWarning());
    }
}

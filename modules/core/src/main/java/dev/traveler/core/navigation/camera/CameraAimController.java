package dev.traveler.core.navigation.camera;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class CameraAimController {
    private static final double MAX_FRAME_SECONDS = 0.1;

    private final CameraAimSettings settings;

    public CameraAimController(CameraAimSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public CameraAngles update(CameraAngles current, CameraAngles target, double deltaSeconds) {
        CameraAngles currentAngles = Objects.requireNonNull(current, "current");
        CameraAngles targetAngles = Objects.requireNonNull(target, "target");
        double frameSeconds = Math.clamp(deltaSeconds, 0.0, MAX_FRAME_SECONDS);
        double yaw = currentAngles.yawDegrees() + angularStep(
                CameraAngles.shortestYawDelta(currentAngles.yawDegrees(), targetAngles.yawDegrees()),
                settings.maxYawDegreesPerSecond(),
                settings.maxYawStepDegrees(),
                frameSeconds);
        double pitch = currentAngles.pitchDegrees() + angularStep(
                targetAngles.pitchDegrees() - currentAngles.pitchDegrees(),
                settings.maxPitchDegreesPerSecond(),
                settings.maxPitchDegreesPerSecond() * frameSeconds,
                frameSeconds);
        return new CameraAngles(yaw, pitch);
    }

    public static CameraAngles targetAngles(NavigationPoint eye, NavigationPoint target) {
        NavigationPoint origin = Objects.requireNonNull(eye, "eye");
        NavigationPoint destination = Objects.requireNonNull(target, "target");
        double deltaX = destination.x() - origin.x();
        double deltaY = destination.y() - origin.y();
        double deltaZ = destination.z() - origin.z();
        double yaw = Math.toDegrees(Math.atan2(-deltaX, deltaZ));
        double pitch = -Math.toDegrees(Math.atan2(deltaY, Math.hypot(deltaX, deltaZ)));
        return new CameraAngles(yaw, pitch);
    }

    private double angularStep(double delta, double maxDegreesPerSecond, double maxStepDegrees, double frameSeconds) {
        if (Math.abs(delta) <= settings.deadzoneDegrees()) {
            return delta;
        }
        double dampedStep = delta * (1.0 - Math.exp(-settings.response() * frameSeconds));
        double velocityStep = maxDegreesPerSecond * frameSeconds;
        double safeStep = Math.min(maxStepDegrees, velocityStep);
        return Math.clamp(dampedStep, -safeStep, safeStep);
    }
}

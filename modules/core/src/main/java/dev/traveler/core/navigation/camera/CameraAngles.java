package dev.traveler.core.navigation.camera;

public record CameraAngles(double yawDegrees, double pitchDegrees) {
    public CameraAngles {
        requireFinite(yawDegrees, "yawDegrees");
        requireFinite(pitchDegrees, "pitchDegrees");
        yawDegrees = cleanZero(normalizeYaw(yawDegrees));
        pitchDegrees = cleanZero(Math.clamp(pitchDegrees, -90.0, 90.0));
    }

    public static double shortestYawDelta(double fromYawDegrees, double toYawDegrees) {
        return normalizeYaw(toYawDegrees - fromYawDegrees);
    }

    public static double normalizeYaw(double yawDegrees) {
        double normalized = yawDegrees;
        while (normalized <= -180.0) {
            normalized += 360.0;
        }
        while (normalized > 180.0) {
            normalized -= 360.0;
        }
        return normalized;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }

    private static double cleanZero(double value) {
        if (value == 0.0) {
            return 0.0;
        }
        return value;
    }
}

package dev.traveler.core.navigation.control;

import dev.traveler.core.common.geometry.HorizontalVector;

public record CameraMovementBasis(HorizontalVector forward, HorizontalVector right) {
    public static CameraMovementBasis fromMinecraftYaw(double yawDegrees) {
        double yawRadians = Math.toRadians(yawDegrees);
        HorizontalVector forward = new HorizontalVector(-Math.sin(yawRadians), Math.cos(yawRadians));
        HorizontalVector right = new HorizontalVector(-Math.cos(yawRadians), -Math.sin(yawRadians));
        return new CameraMovementBasis(forward, right);
    }
}

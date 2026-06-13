package dev.traveler.core.navigation.input;

import dev.traveler.core.navigation.spatial.HorizontalVector;

record CameraMovementBasis(HorizontalVector forward, HorizontalVector right) {
    static CameraMovementBasis fromMinecraftYaw(double yawDegrees) {
        double yawRadians = Math.toRadians(yawDegrees);
        HorizontalVector forward = new HorizontalVector(-Math.sin(yawRadians), Math.cos(yawRadians));
        HorizontalVector right = new HorizontalVector(-Math.cos(yawRadians), -Math.sin(yawRadians));
        return new CameraMovementBasis(forward, right);
    }
}

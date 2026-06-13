package dev.traveler.core.route;

import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.movement.MovementProfiles;
import java.util.Objects;

public record RouteSearchSettings(
        int horizontalMargin,
        int verticalMargin,
        MovementProfile movementProfile) {
    private static final int STANDARD_HORIZONTAL_MARGIN = 24;
    private static final int STANDARD_VERTICAL_MARGIN = 8;
    private static final MovementCapabilities CLIENT_CAPABILITIES =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    public RouteSearchSettings {
        requirePositive(horizontalMargin, "horizontalMargin");
        requirePositive(verticalMargin, "verticalMargin");
        Objects.requireNonNull(movementProfile, "movementProfile");
    }

    public static RouteSearchSettings standardClient() {
        return new RouteSearchSettings(
                STANDARD_HORIZONTAL_MARGIN,
                STANDARD_VERTICAL_MARGIN,
                MovementProfiles.defaultPlayerWith(CLIENT_CAPABILITIES));
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}

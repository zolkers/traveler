package dev.traveler.core.route;

import dev.traveler.core.settings.TravelerSettings;
import dev.traveler.core.world.movement.EntityDimensions;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.movement.TraversalRules;
import java.util.Objects;

public record RouteSearchSettings(
        int horizontalMargin,
        int verticalMargin,
        MovementProfile movementProfile) {
    public RouteSearchSettings {
        requirePositive(horizontalMargin, "horizontalMargin");
        requirePositive(verticalMargin, "verticalMargin");
        Objects.requireNonNull(movementProfile, "movementProfile");
    }

    public static RouteSearchSettings standardClient() {
        return TravelerSettings.standard().routeSearchSettings();
    }

    public RouteSearchSettings withMovementProfile(MovementProfile profile) {
        return new RouteSearchSettings(horizontalMargin, verticalMargin, profile);
    }

    public RouteSearchSettings withEntityDimensions(EntityDimensions dimensions) {
        return withMovementProfile(movementProfile.withDimensions(dimensions));
    }

    public RouteSearchSettings withMovementCapabilities(MovementCapabilities capabilities) {
        return withMovementProfile(movementProfile.withCapabilities(capabilities));
    }

    public RouteSearchSettings withTraversalRules(TraversalRules rules) {
        return withMovementProfile(movementProfile.withRules(rules));
    }

    public RouteSearchSettings withMaxSafeFallDistance(double distance) {
        return withMovementCapabilities(movementProfile.capabilities().withMaxSafeFallDistance(distance));
    }

    public RouteSearchSettings withSwimmingEnabled(boolean enabled) {
        return withMovementCapabilities(movementProfile.capabilities().withCanSwim(enabled));
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}

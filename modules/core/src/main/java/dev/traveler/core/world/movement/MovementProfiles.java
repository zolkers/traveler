package dev.traveler.core.world.movement;

import dev.traveler.core.settings.TravelerSettings;
import java.util.Objects;

public final class MovementProfiles {
    private MovementProfiles() {}

    public static MovementProfile defaultPlayer() {
        return new MovementProfile(defaultPlayerDimensions(), defaultPlayerCapabilities(), defaultPlayerRules());
    }

    public static MovementProfile defaultPlayerWith(MovementCapabilities capabilities) {
        return new MovementProfile(
                defaultPlayerDimensions(),
                Objects.requireNonNull(capabilities, "capabilities"),
                defaultPlayerRules());
    }

    public static EntityDimensions defaultPlayerDimensions() {
        return TravelerSettings.standard().entityDimensions();
    }

    public static MovementCapabilities defaultPlayerCapabilities() {
        return TravelerSettings.standard().movementCapabilities();
    }

    public static TraversalRules defaultPlayerRules() {
        return TravelerSettings.standard().traversalRules();
    }
}

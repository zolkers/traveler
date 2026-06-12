package dev.traveler.core.layer;

import dev.traveler.core.world.MovementProfile;

@FunctionalInterface
public interface MovementLayer {
    MovementProfile movementProfile();
}

package dev.traveler.core.layer;

import dev.traveler.core.world.movement.MovementProfile;

@FunctionalInterface
public interface MovementLayer {
    MovementProfile movementProfile();
}

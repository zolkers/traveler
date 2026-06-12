package dev.traveler.mc.v1_21_11.common.adapter.movement;

import dev.traveler.core.layer.MovementLayer;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.movement.MovementProfiles;

public final class MinecraftMovementProfileAdapter implements MovementLayer {
    public MinecraftMovementProfileAdapter() {}

    @Override
    public MovementProfile movementProfile() {
        return defaultPlayerProfile();
    }

    public static MovementProfile defaultPlayerProfile() {
        return MovementProfiles.defaultPlayer();
    }
}

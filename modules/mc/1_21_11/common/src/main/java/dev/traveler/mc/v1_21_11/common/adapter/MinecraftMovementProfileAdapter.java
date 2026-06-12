package dev.traveler.mc.v1_21_11.common.adapter;

import dev.traveler.core.layer.MovementLayer;
import dev.traveler.core.world.BlockPassability;
import dev.traveler.core.world.EntityDimensions;
import dev.traveler.core.world.FluidHandling;
import dev.traveler.core.world.MovementCapabilities;
import dev.traveler.core.world.MovementProfile;
import dev.traveler.core.world.TraversalCost;
import dev.traveler.core.world.TraversalRules;
import java.util.Set;

public final class MinecraftMovementProfileAdapter implements MovementLayer {
    public MinecraftMovementProfileAdapter() {}

    @Override
    public MovementProfile movementProfile() {
        return defaultPlayerProfile();
    }

    public static MovementProfile defaultPlayerProfile() {
        return new MovementProfile(
                new EntityDimensions(0.6, 1.8),
                new MovementCapabilities(true, true, false, true, 0.6, 1.25, 3.0),
                new TraversalRules(
                        false,
                        true,
                        FluidHandling.ALLOW,
                        new TraversalCost(1.0),
                        Set.of(BlockPassability.WALKABLE, BlockPassability.PASSABLE)));
    }
}

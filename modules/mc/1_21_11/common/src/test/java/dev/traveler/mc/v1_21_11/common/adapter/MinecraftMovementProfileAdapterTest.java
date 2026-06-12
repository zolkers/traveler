package dev.traveler.mc.v1_21_11.common.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.world.BlockPassability;
import dev.traveler.core.world.FluidHandling;
import dev.traveler.core.world.MovementProfile;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MinecraftMovementProfileAdapterTest {
    @Test
    void createsDefaultPlayerLikeMovementProfile() {
        MovementProfile profile = MinecraftMovementProfileAdapter.defaultPlayerProfile();

        assertEquals(0.6, profile.dimensions().width());
        assertEquals(1.8, profile.dimensions().height());
        assertEquals(true, profile.capabilities().canWalk());
        assertEquals(true, profile.capabilities().canSwim());
        assertEquals(false, profile.capabilities().canFly());
        assertEquals(true, profile.capabilities().canCrouch());
        assertEquals(0.6, profile.capabilities().maxStepUp());
        assertEquals(1.25, profile.capabilities().maxJumpHeight());
        assertEquals(3.0, profile.capabilities().maxSafeFallDistance());
        assertEquals(FluidHandling.ALLOW, profile.rules().fluidHandling());
        assertEquals(
                Set.of(BlockPassability.WALKABLE, BlockPassability.PASSABLE),
                profile.rules().allowedPassability());
    }
}

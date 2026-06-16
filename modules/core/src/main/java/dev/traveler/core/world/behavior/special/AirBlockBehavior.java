package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.api.CollisionSemantics;
import dev.traveler.core.world.behavior.api.FluidSemantics;
import dev.traveler.core.world.behavior.api.SupportSemantics;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import java.util.Set;

public final class AirBlockBehavior implements BlockBehavior {
    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.AIR;
    }

    @Override
    public BlockSemantics describe(SurfaceMovementContext context) {
        return BlockSemantics.of(
                CollisionSemantics.PASSABLE,
                SupportSemantics.NONE,
                FluidSemantics.NONE,
                Set.of());
    }
}

package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.api.CollisionSemantics;
import dev.traveler.core.world.behavior.api.FluidSemantics;
import dev.traveler.core.world.behavior.api.SupportSemantics;
import dev.traveler.core.world.behavior.api.TraversalAffordance;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.Objects;
import java.util.Set;

public final class FluidBlockBehavior implements BlockBehavior {
    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.FLUID;
    }

    @Override
    public boolean supportsStanding(MovementCapabilities capabilities) {
        return false;
    }

    @Override
    public BlockSemantics describe(SurfaceMovementContext context) {
        Objects.requireNonNull(context, "context");
        return BlockSemantics.of(
                CollisionSemantics.PASSABLE,
                SupportSemantics.NONE,
                FluidSemantics.SWIMMABLE,
                Set.of(TraversalAffordance.SWIM));
    }
}

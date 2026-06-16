package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.api.CollisionSemantics;
import dev.traveler.core.world.behavior.api.FluidSemantics;
import dev.traveler.core.world.behavior.api.SupportSemantics;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import java.util.Objects;
import java.util.Set;

public abstract class TallObstacleBlockBehavior implements BlockBehavior {
    @Override
    public final BlockSemantics describe(SurfaceMovementContext context) {
        Objects.requireNonNull(context, "context");
        return BlockSemantics.of(
                CollisionSemantics.SOLID,
                SupportSemantics.NONE,
                FluidSemantics.NONE,
                Set.of());
    }
}

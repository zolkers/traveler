package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.Objects;

public final class WaterloggedBlockBehavior implements BlockBehavior {
    private final BlockBehavior delegate;

    public WaterloggedBlockBehavior(BlockBehavior delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.WATERLOGGED;
    }

    public BlockBehavior delegate() {
        return delegate;
    }

    @Override
    public boolean supportsStanding(MovementCapabilities capabilities) {
        return delegate.supportsStanding(capabilities);
    }

    @Override
    public MovementDecision evaluateMovement(SurfaceMovementContext context) {
        return delegate.evaluateMovement(context);
    }
}

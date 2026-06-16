package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.api.BehaviorTag;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import java.util.Objects;
import java.util.Set;

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
    public BlockSemantics describe(SurfaceMovementContext context) {
        BlockSemantics semantics = delegate.describe(context);
        return new BlockSemantics(
                semantics.collision(),
                semantics.support(),
                semantics.fluid(),
                semantics.affordances(),
                mergeTags(semantics.tags()));
    }

    private Set<BehaviorTag> mergeTags(Set<BehaviorTag> tags) {
        java.util.EnumSet<BehaviorTag> merged = java.util.EnumSet.of(BehaviorTag.WATERLOGGED);
        merged.addAll(tags);
        return merged;
    }
}

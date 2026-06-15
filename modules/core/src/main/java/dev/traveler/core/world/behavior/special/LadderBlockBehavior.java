package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import java.util.Objects;
import java.util.Set;

public final class LadderBlockBehavior extends ClimbableBlockBehavior {
    private final HorizontalFacing facing;

    public LadderBlockBehavior(HorizontalFacing facing) {
        this.facing = Objects.requireNonNull(facing, "facing");
    }

    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.LADDER;
    }

    public HorizontalFacing facing() {
        return facing;
    }

    @Override
    public Set<HorizontalFacing> climbableFaces() {
        return Set.of(facing);
    }
}

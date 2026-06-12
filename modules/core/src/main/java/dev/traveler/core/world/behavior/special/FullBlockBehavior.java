package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehaviorKey;

public final class FullBlockBehavior extends WalkableSurfaceBlockBehavior {
    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.FULL_BLOCK;
    }
}

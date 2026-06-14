package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehaviorKey;

public final class WallBlockBehavior extends TallObstacleBlockBehavior {
    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.WALL;
    }
}

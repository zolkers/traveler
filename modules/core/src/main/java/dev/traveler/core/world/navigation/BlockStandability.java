package dev.traveler.core.world.navigation;

import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.WorldLayer;

final class BlockStandability {
    private BlockStandability() {}

    static boolean isStandable(WorldLayer worldLayer, BlockPosition position) {
        return isBodyPassable(worldLayer, position) && supportsEntity(worldLayer, position.below());
    }

    private static boolean isBodyPassable(WorldLayer worldLayer, BlockPosition position) {
        return isPassable(worldLayer, position) && isPassable(worldLayer, position.above());
    }

    private static boolean supportsEntity(WorldLayer worldLayer, BlockPosition position) {
        BlockPassability passability = classification(worldLayer, position).passability();
        return passability == BlockPassability.SOLID || passability == BlockPassability.WALKABLE;
    }

    private static boolean isPassable(WorldLayer worldLayer, BlockPosition position) {
        BlockPassability passability = classification(worldLayer, position).passability();
        return passability == BlockPassability.PASSABLE || passability == BlockPassability.WALKABLE;
    }

    private static BlockClassification classification(WorldLayer worldLayer, BlockPosition position) {
        return worldLayer.classify(position);
    }
}

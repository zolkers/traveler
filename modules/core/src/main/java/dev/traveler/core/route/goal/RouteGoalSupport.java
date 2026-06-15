package dev.traveler.core.route.goal;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;

final class RouteGoalSupport {
    private RouteGoalSupport() {
    }

    static BlockPosition passableFeetGoal(WorldLayer worldLayer, BlockPosition target) {
        if (worldLayer == null) {
            return target;
        }
        if (worldLayer.classify(target).passability() == BlockPassability.SOLID) {
            return target.above();
        }
        return target;
    }
}

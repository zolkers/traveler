package dev.traveler.core.world.navigation;

import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.WorldLayer;
import java.util.HashSet;
import java.util.Set;

record BlockedWorldLayer(Set<BlockPosition> blockedFeet) implements WorldLayer {
    BlockedWorldLayer {
        blockedFeet = new HashSet<>(blockedFeet);
    }

    @Override
    public BlockClassification classify(BlockPosition position) {
        if (blockedFeet.contains(position) || position.y() == 63) {
            return solid();
        }
        return new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID);
    }

    private static BlockClassification solid() {
        return new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID);
    }
}

package dev.traveler.core.layer;

import dev.traveler.core.world.block.BlockPosition;

@FunctionalInterface
public interface WorldLayer {
    BlockClassification classify(BlockPosition position);
}

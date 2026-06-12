package dev.traveler.core.layer;

import dev.traveler.core.world.BlockPosition;

@FunctionalInterface
public interface WorldLayer {
    BlockClassification classify(BlockPosition position);
}

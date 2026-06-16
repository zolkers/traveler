package dev.traveler.core.navigation.diagnostics;

import dev.traveler.core.world.block.BlockPosition;

@FunctionalInterface
public interface BlockScanSource {
    BlockScanSample sample(BlockPosition position);
}

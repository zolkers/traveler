package dev.traveler.core.command;

import dev.traveler.core.world.block.BlockPosition;

public record TravelerCommandBlockPosition(int x, int y, int z) {
    public BlockPosition toCorePosition() {
        return new BlockPosition(x, y, z);
    }
}

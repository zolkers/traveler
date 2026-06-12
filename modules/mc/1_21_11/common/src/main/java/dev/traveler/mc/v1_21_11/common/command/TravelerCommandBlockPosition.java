package dev.traveler.mc.v1_21_11.common.command;

import dev.traveler.core.world.block.BlockPosition;

public record TravelerCommandBlockPosition(int x, int y, int z) {
    public BlockPosition toCorePosition() {
        return new BlockPosition(x, y, z);
    }
}

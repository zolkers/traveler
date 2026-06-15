package dev.traveler.core.command;

import dev.traveler.core.world.block.BlockPosition;

public interface TravelerCommandSource {
    BlockPosition blockPosition();

    void reply(String message);
}

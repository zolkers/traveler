package dev.traveler.core.world.navigation;

import dev.traveler.core.world.block.BlockPosition;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockLineOfWalkTest {
    @Test
    void acceptsClearDiagonalWalkLines() {
        BlockLineOfWalk lineOfWalk = new BlockLineOfWalk(new BlockedWorldLayer(Set.of()));

        boolean clear = lineOfWalk.hasLineOfWalk(new BlockPosition(0, 64, 0), new BlockPosition(3, 64, 3));

        assertTrue(clear);
    }

    @Test
    void rejectsWalkLinesThroughSolidBlocks() {
        BlockPosition blocked = new BlockPosition(1, 64, 1);
        BlockLineOfWalk lineOfWalk = new BlockLineOfWalk(new BlockedWorldLayer(Set.of(blocked)));

        boolean clear = lineOfWalk.hasLineOfWalk(new BlockPosition(0, 64, 0), new BlockPosition(3, 64, 3));

        assertFalse(clear);
    }

    @Test
    void rejectsWalkLinesThatShaveBlockedCorners() {
        BlockPosition blockedCorner = new BlockPosition(1, 64, 0);
        BlockLineOfWalk lineOfWalk = new BlockLineOfWalk(new BlockedWorldLayer(Set.of(blockedCorner)));

        boolean clear = lineOfWalk.hasLineOfWalk(new BlockPosition(0, 64, 0), new BlockPosition(2, 64, 2));

        assertFalse(clear);
    }

}

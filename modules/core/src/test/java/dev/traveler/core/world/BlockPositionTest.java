package dev.traveler.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class BlockPositionTest {
    @Test
    void offsetCreatesStableTranslatedPosition() {
        BlockPosition origin = new BlockPosition(10, 64, -4);

        BlockPosition offset = origin.offset(1, -2, 3);

        assertEquals(new BlockPosition(11, 62, -1), offset);
        assertEquals(new BlockPosition(10, 64, -4), origin);
    }

    @Test
    void directionalHelpersMirrorOneBlockOffsets() {
        BlockPosition position = new BlockPosition(2, 5, -3);

        assertEquals(position.offset(1, 0, 0), position.east());
        assertEquals(position.offset(-1, 0, 0), position.west());
        assertEquals(position.offset(0, 1, 0), position.above());
        assertEquals(position.offset(0, -1, 0), position.below());
        assertEquals(position.offset(0, 0, 1), position.south());
        assertEquals(position.offset(0, 0, -1), position.north());
    }

    @Test
    void neighborsUseStableSixDirectionOrder() {
        BlockPosition position = new BlockPosition(0, 70, 0);

        assertEquals(
                List.of(
                        position.east(),
                        position.west(),
                        position.above(),
                        position.below(),
                        position.south(),
                        position.north()),
                position.neighbors());
    }
}

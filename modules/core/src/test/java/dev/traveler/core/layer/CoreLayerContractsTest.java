package dev.traveler.core.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.FluidHandling;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoreLayerContractsTest {
    @Test
    void worldLayerClassifiesBlockPositionsWithoutMinecraftTypes() {
        BlockClassification stone = new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID);
        WorldLayer layer = new MapWorldLayer(Map.of(new BlockPosition(1, 2, 3), stone));

        assertEquals(stone, layer.classify(new BlockPosition(1, 2, 3)));
    }

    @Test
    void blockClassificationRejectsNullValues() {
        assertThrows(NullPointerException.class, () -> new BlockClassification(null, FluidHandling.ALLOW));
        assertThrows(NullPointerException.class, () -> new BlockClassification(BlockPassability.WALKABLE, null));
    }

    private record MapWorldLayer(Map<BlockPosition, BlockClassification> blocks) implements WorldLayer {
        @Override
        public BlockClassification classify(BlockPosition position) {
            return blocks.getOrDefault(
                    position, new BlockClassification(BlockPassability.WALKABLE, FluidHandling.ALLOW));
        }
    }
}

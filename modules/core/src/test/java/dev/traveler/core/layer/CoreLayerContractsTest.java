package dev.traveler.core.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.world.BlockPassability;
import dev.traveler.core.world.BlockPosition;
import dev.traveler.core.world.EntityDimensions;
import dev.traveler.core.world.FluidHandling;
import dev.traveler.core.world.MovementCapabilities;
import dev.traveler.core.world.MovementProfile;
import dev.traveler.core.world.TraversalCost;
import dev.traveler.core.world.TraversalRules;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoreLayerContractsTest {
    @Test
    void blockClassificationExposesTraversalMeaning() {
        BlockClassification classification =
                new BlockClassification(BlockPassability.WALKABLE, FluidHandling.ALLOW);

        assertTrue(classification.isWalkable());
    }

    @Test
    void worldLayerClassifiesBlockPositionsWithoutMinecraftTypes() {
        BlockClassification stone = new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID);
        WorldLayer layer = new MapWorldLayer(Map.of(new BlockPosition(1, 2, 3), stone));

        assertEquals(stone, layer.classify(new BlockPosition(1, 2, 3)));
    }

    @Test
    void classifierRejectsNullContextAndClassificationValues() {
        BlockClassifier<String> classifier =
                context -> new BlockClassification(BlockPassability.WALKABLE, FluidHandling.ALLOW);

        assertThrows(NullPointerException.class, () -> classifier.classify(null));
        assertThrows(NullPointerException.class, () -> new BlockClassification(null, FluidHandling.ALLOW));
        assertThrows(NullPointerException.class, () -> new BlockClassification(BlockPassability.WALKABLE, null));
    }

    @Test
    void movementLayerReturnsCoreMovementProfile() {
        MovementProfile profile = new MovementProfile(
                new EntityDimensions(0.6, 1.8),
                new MovementCapabilities(true, false, false, true, 0.6, 1.25, 3.0),
                new TraversalRules(
                        true, true, FluidHandling.ALLOW, new TraversalCost(1.0), Set.of(BlockPassability.WALKABLE)));
        MovementLayer layer = () -> profile;

        assertEquals(profile, layer.movementProfile());
    }

    private record MapWorldLayer(Map<BlockPosition, BlockClassification> blocks) implements WorldLayer {
        @Override
        public BlockClassification classify(BlockPosition position) {
            return blocks.getOrDefault(
                    position, new BlockClassification(BlockPassability.WALKABLE, FluidHandling.ALLOW));
        }
    }
}

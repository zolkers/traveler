package dev.traveler.core.world.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.movement.FluidHandling;
import org.junit.jupiter.api.Test;

class BlockBehaviorClassificationPolicyTest {
    private static final BlockBehaviorRegistry BEHAVIORS = BlockBehaviorRegistry.defaults();
    private static final BlockBehaviorClassificationPolicy POLICY = new BlockBehaviorClassificationPolicy();

    @Test
    void keepsNonPartialBehaviorsOnTheirBaseClassification() {
        assertEquals(BlockPassability.SOLID, classify(solid(), BEHAVIORS.behavior(BlockBehaviorKey.FULL_BLOCK)));
        assertEquals(BlockPassability.PASSABLE, classify(passable(), BEHAVIORS.behavior(BlockBehaviorKey.AIR)));
        assertEquals(BlockPassability.PASSABLE, classify(passable(), BEHAVIORS.behavior(BlockBehaviorKey.FLUID)));
    }

    @Test
    void marksPartialWalkableBehaviorsAsWalkableSurfaces() {
        assertEquals(BlockPassability.WALKABLE, classify(solid(), BEHAVIORS.behavior(BlockBehaviorKey.SLAB)));
        assertEquals(BlockPassability.WALKABLE, classify(solid(), BEHAVIORS.stair(HorizontalFacing.WEST)));
    }

    @Test
    void unwrapsWaterloggedPartialBehaviors() {
        BlockBehavior slab = BEHAVIORS.behavior(BlockBehaviorKey.SLAB);
        BlockBehavior stair = BEHAVIORS.stair(HorizontalFacing.NORTH);
        BlockBehavior full = BEHAVIORS.behavior(BlockBehaviorKey.FULL_BLOCK);

        assertEquals(BlockPassability.WALKABLE, classify(solidWater(), BEHAVIORS.waterlogged(slab)));
        assertEquals(BlockPassability.WALKABLE, classify(solidWater(), BEHAVIORS.waterlogged(stair)));
        assertEquals(BlockPassability.SOLID, classify(solidWater(), BEHAVIORS.waterlogged(full)));
    }

    private static BlockPassability classify(BlockClassification base, BlockBehavior behavior) {
        return POLICY.classify(base, behavior).passability();
    }

    private static BlockClassification solid() {
        return new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID);
    }

    private static BlockClassification solidWater() {
        return new BlockClassification(BlockPassability.SOLID, FluidHandling.ALLOW);
    }

    private static BlockClassification passable() {
        return new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID);
    }
}

package dev.traveler.core.world.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.behavior.special.AirBlockBehavior;
import dev.traveler.core.world.behavior.special.FluidBlockBehavior;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.behavior.special.SlabBlockBehavior;
import dev.traveler.core.world.behavior.special.StairBlockBehavior;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import dev.traveler.core.world.movement.MovementCapabilities;
import org.junit.jupiter.api.Test;

class BlockBehaviorRegistryTest {
    @Test
    void startsWithDedicatedDefaultBehaviorStrategies() {
        BlockBehaviorRegistry registry = BlockBehaviorRegistry.defaults();

        assertInstanceOf(AirBlockBehavior.class, registry.behavior(BlockBehaviorKey.AIR));
        assertInstanceOf(FullBlockBehavior.class, registry.behavior(BlockBehaviorKey.FULL_BLOCK));
        assertInstanceOf(SlabBlockBehavior.class, registry.behavior(BlockBehaviorKey.SLAB));
        assertInstanceOf(StairBlockBehavior.class, registry.behavior(BlockBehaviorKey.STAIR));
        assertInstanceOf(FluidBlockBehavior.class, registry.behavior(BlockBehaviorKey.FLUID));
        assertInstanceOf(WaterloggedBlockBehavior.class, registry.behavior(BlockBehaviorKey.WATERLOGGED));
    }

    @Test
    void createsStatefulStairBehaviorForTheRequestedFacing() {
        BlockBehaviorRegistry registry = BlockBehaviorRegistry.defaults();

        StairBlockBehavior stair = assertInstanceOf(
                StairBlockBehavior.class, registry.stair(HorizontalFacing.WEST));

        assertEquals(HorizontalFacing.WEST, stair.facing());
    }

    @Test
    void waterloggedBehaviorKeepsTheDrySurfaceBehaviorAsDelegate() {
        BlockBehaviorRegistry registry = BlockBehaviorRegistry.defaults();
        BlockBehavior slab = registry.behavior(BlockBehaviorKey.SLAB);

        WaterloggedBlockBehavior waterlogged = assertInstanceOf(
                WaterloggedBlockBehavior.class, registry.waterlogged(slab));

        assertSame(slab, waterlogged.delegate());
    }

    @Test
    void allowsReplacingSpecificBehaviorStrategy() {
        BlockBehaviorRegistry registry = BlockBehaviorRegistry.defaults();
        BlockBehavior customStair = new TestBehavior(BlockBehaviorKey.STAIR);

        registry.register(customStair);

        assertSame(customStair, registry.behavior(BlockBehaviorKey.STAIR));
    }

    private record TestBehavior(BlockBehaviorKey key) implements BlockBehavior {
        @Override
        public boolean supportsStanding(MovementCapabilities capabilities) {
            return true;
        }

        @Override
        public MovementDecision evaluateMovement(SurfaceMovementContext context) {
            return MovementDecision.walk();
        }
    }
}

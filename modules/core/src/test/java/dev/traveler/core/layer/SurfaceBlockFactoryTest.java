package dev.traveler.core.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.FluidBlockBehavior;
import dev.traveler.core.world.behavior.special.LadderBlockBehavior;
import dev.traveler.core.world.behavior.special.SlabBlockBehavior;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import org.junit.jupiter.api.Test;

class SurfaceBlockFactoryTest {
    private final BlockBehaviorRegistry behaviors = BlockBehaviorRegistry.defaults();
    private final SurfaceBlockFactory factory = new SurfaceBlockFactory(behaviors);

    @Test
    void turnsClimbableSamplesIntoPassableActionVolumesWithoutNavigationCollision() {
        SurfaceBlock block = factory.create(new SurfaceBlockSample(
                false,
                false,
                BlockShape.fullCube(),
                BlockBehaviorSpec.ladder(HorizontalFacing.EAST)));

        LadderBlockBehavior behavior = assertInstanceOf(LadderBlockBehavior.class, block.behavior());
        assertEquals(HorizontalFacing.EAST, behavior.facing());
        assertEquals(BlockPassability.PASSABLE, block.classification().passability());
        assertEquals(FluidHandling.AVOID, block.classification().fluidHandling());
        assertTrue(block.shape().isEmpty());
    }

    @Test
    void wrapsCollidableFluidSamplesAsWaterloggedDryBehavior() {
        SurfaceBlock block = factory.create(new SurfaceBlockSample(
                false,
                true,
                BlockShape.bottomSlab(),
                BlockBehaviorSpec.slab()));

        WaterloggedBlockBehavior behavior = assertInstanceOf(WaterloggedBlockBehavior.class, block.behavior());
        assertInstanceOf(SlabBlockBehavior.class, behavior.delegate());
        assertEquals(BlockPassability.WALKABLE, block.classification().passability());
        assertEquals(FluidHandling.ALLOW, block.classification().fluidHandling());
        assertEquals(0.5, block.shape().floorHeightForCell(0, 0).orElseThrow());
    }

    @Test
    void turnsEmptyFluidSamplesIntoPureFluidBehavior() {
        SurfaceBlock block = factory.create(new SurfaceBlockSample(
                false,
                true,
                BlockShape.empty(),
                BlockBehaviorSpec.automatic()));

        assertInstanceOf(FluidBlockBehavior.class, block.behavior());
        assertEquals(BlockBehaviorKey.FLUID, block.behavior().key());
        assertEquals(BlockPassability.PASSABLE, block.classification().passability());
        assertEquals(FluidHandling.ALLOW, block.classification().fluidHandling());
        assertTrue(block.shape().isEmpty());
    }

    @Test
    void resolvesAutomaticSamplesFromRawCollisionShape() {
        SurfaceBlock air = factory.create(new SurfaceBlockSample(
                true,
                false,
                BlockShape.empty(),
                BlockBehaviorSpec.automatic()));
        SurfaceBlock solid = factory.create(new SurfaceBlockSample(
                false,
                false,
                BlockShape.fullCube(),
                BlockBehaviorSpec.automatic()));

        assertSame(behaviors.behavior(BlockBehaviorKey.AIR), air.behavior());
        assertSame(behaviors.behavior(BlockBehaviorKey.FULL_BLOCK), solid.behavior());
        assertEquals(BlockPassability.PASSABLE, air.classification().passability());
        assertEquals(BlockPassability.SOLID, solid.classification().passability());
    }
}

package dev.traveler.mc.v1_21_11.common.adapter.block;

import dev.traveler.mc.v1_21_11.common.adapter.testing.MinecraftTestBootstrap;
import dev.traveler.mc.v1_21_11.common.adapter.testing.SingleStateBlockGetter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.BlockClassifier;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.movement.FluidHandling;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinecraftBlockClassifierTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void classifiesAirAsPassableAndSolidBlocksAsSolid() {
        MinecraftBlockClassifier classifier = new MinecraftBlockClassifier();

        assertEquals(BlockPassability.PASSABLE, classifier.classify(Blocks.AIR.defaultBlockState()));
        assertEquals(BlockPassability.SOLID, classifier.classify(Blocks.STONE.defaultBlockState()));
    }

    @Test
    void implementsCoreBlockClassifierContract() {
        MinecraftBlockClassifier classifier = new MinecraftBlockClassifier();
        BlockState air = Blocks.AIR.defaultBlockState();
        MinecraftBlockContext context = new MinecraftBlockContext(air, new SingleStateBlockGetter(air), BlockPos.ZERO);

        BlockClassification classification = classifier.classify(context);

        assertInstanceOf(BlockClassifier.class, classifier);
        assertEquals(BlockPassability.PASSABLE, classification.passability());
        assertEquals(FluidHandling.AVOID, classification.fluidHandling());
    }

    @Test
    void treatsNonEmptyFluidsAsPassableAndSupportsFluidHandlingChecks() {
        MinecraftBlockClassifier classifier = new MinecraftBlockClassifier();

        assertEquals(BlockPassability.PASSABLE, classifier.classify(Fluids.WATER.defaultFluidState()));
        assertTrue(classifier.hasFluid(Fluids.WATER.defaultFluidState()));
        assertTrue(classifier.allowsFluid(Fluids.WATER.defaultFluidState(), FluidHandling.ALLOW));
        assertTrue(classifier.allowsFluid(Fluids.WATER.defaultFluidState(), FluidHandling.REQUIRE));
        assertFalse(classifier.allowsFluid(Fluids.WATER.defaultFluidState(), FluidHandling.AVOID));
        assertFalse(classifier.allowsFluid(Fluids.EMPTY.defaultFluidState(), FluidHandling.REQUIRE));
    }

    @Test
    void classifiesWaterloggedCollidableBlocksAsSolidWithBlockGetter() {
        MinecraftBlockClassifier classifier = new MinecraftBlockClassifier();
        BlockState waterloggedSlab =
                Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        BlockGetter blockGetter = new SingleStateBlockGetter(waterloggedSlab);

        assertTrue(classifier.hasFluid(waterloggedSlab.getFluidState()));
        assertFalse(waterloggedSlab.getCollisionShape(blockGetter, BlockPos.ZERO).isEmpty());
        assertEquals(BlockPassability.SOLID, classifier.classify(waterloggedSlab, blockGetter, BlockPos.ZERO));
    }
}

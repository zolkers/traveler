package dev.traveler.mc.v1_21_11.common.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.world.BlockPassability;
import dev.traveler.core.world.FluidHandling;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinecraftBlockClassifierTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void classifiesAirAsPassableAndSolidBlocksAsSolid() {
        MinecraftBlockClassifier classifier = new MinecraftBlockClassifier();

        assertEquals(BlockPassability.PASSABLE, classifier.classify(Blocks.AIR.defaultBlockState()));
        assertEquals(BlockPassability.SOLID, classifier.classify(Blocks.STONE.defaultBlockState()));
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

    private static final class SingleStateBlockGetter implements BlockGetter {
        private final BlockState state;

        private SingleStateBlockGetter(BlockState state) {
            this.state = state;
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos position) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos position) {
            return state;
        }

        @Override
        public FluidState getFluidState(BlockPos position) {
            return state.getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    }
}

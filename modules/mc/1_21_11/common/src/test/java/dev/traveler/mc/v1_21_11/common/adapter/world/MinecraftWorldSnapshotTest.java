package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.mc.v1_21_11.common.adapter.testing.AbstractTestBlockGetter;
import dev.traveler.mc.v1_21_11.common.adapter.testing.MinecraftTestBootstrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.traveler.core.layer.NavigationBudgetProvider;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.layer.WorldNavigationBudget;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinecraftWorldSnapshotTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void convertsPositionsBetweenCoreAndMinecraftTypes() {
        BlockPosition core = new BlockPosition(7, -12, 34);

        BlockPos minecraft = MinecraftWorldSnapshot.toMinecraft(core);
        BlockPosition converted = MinecraftWorldSnapshot.toCore(minecraft);

        assertEquals(7, minecraft.getX());
        assertEquals(-12, minecraft.getY());
        assertEquals(34, minecraft.getZ());
        assertEquals(core, converted);
    }

    @Test
    void readsBlockAndFluidStatesThroughWrappedBlockGetter() {
        StubBlockGetter blockGetter = new StubBlockGetter();
        MinecraftWorldSnapshot snapshot = new MinecraftWorldSnapshot(blockGetter);
        BlockPosition position = new BlockPosition(1, 2, 3);

        assertSame(Blocks.STONE.defaultBlockState(), snapshot.blockState(position));
        assertSame(Fluids.WATER.defaultFluidState(), snapshot.fluidState(position));
        assertEquals(new BlockPos(1, 2, 3), blockGetter.lastBlockPosition);
        assertEquals(new BlockPos(1, 2, 3), blockGetter.lastFluidPosition);
    }

    @Test
    void implementsCoreWorldLayerContract() {
        MinecraftWorldSnapshot snapshot = new MinecraftWorldSnapshot(new StubBlockGetter());

        assertInstanceOf(WorldLayer.class, snapshot);
        assertEquals(
                BlockPassability.SOLID,
                snapshot.classify(new BlockPosition(0, 0, 0)).passability());
    }

    @Test
    void exposesCoreNavigationBudgetWhenProvided() {
        MinecraftWorldSnapshot snapshot =
                new MinecraftWorldSnapshot(new StubBlockGetter(), new WorldNavigationBudget(128));

        NavigationBudgetProvider provider = assertInstanceOf(NavigationBudgetProvider.class, snapshot);
        assertEquals(128, provider.navigationBudget().visibleHorizontalRadiusBlocks());
    }

    private static final class StubBlockGetter extends AbstractTestBlockGetter {
        private BlockPos lastBlockPosition;
        private BlockPos lastFluidPosition;

        @Override
        public BlockState getBlockState(BlockPos position) {
            lastBlockPosition = position;
            return Blocks.STONE.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos position) {
            lastFluidPosition = position;
            return Fluids.WATER.defaultFluidState();
        }
    }
}

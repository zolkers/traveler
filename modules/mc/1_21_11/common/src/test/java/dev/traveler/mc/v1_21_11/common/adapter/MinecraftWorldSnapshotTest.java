package dev.traveler.mc.v1_21_11.common.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.traveler.core.world.BlockPosition;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinecraftWorldSnapshotTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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

    private static final class StubBlockGetter implements BlockGetter {
        private BlockPos lastBlockPosition;
        private BlockPos lastFluidPosition;

        @Override
        public BlockEntity getBlockEntity(BlockPos position) {
            return null;
        }

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

package dev.traveler.mc.v1_21_11.common.adapter.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class SingleStateBlockGetter extends AbstractTestBlockGetter {
    private final BlockState state;

    public SingleStateBlockGetter(BlockState state) {
        this.state = state;
    }

    @Override
    public BlockState getBlockState(BlockPos position) {
        return state;
    }

    @Override
    public FluidState getFluidState(BlockPos position) {
        return state.getFluidState();
    }
}

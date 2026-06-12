package dev.traveler.mc.v1_21_11.common.adapter;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public record MinecraftBlockContext(BlockState state, BlockGetter blockGetter, BlockPos position) {
    public MinecraftBlockContext {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(blockGetter, "blockGetter");
        Objects.requireNonNull(position, "position");
    }
}

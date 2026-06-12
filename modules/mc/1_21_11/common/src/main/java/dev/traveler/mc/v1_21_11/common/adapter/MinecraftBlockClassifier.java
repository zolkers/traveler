package dev.traveler.mc.v1_21_11.common.adapter;

import dev.traveler.core.world.BlockPassability;
import dev.traveler.core.world.FluidHandling;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class MinecraftBlockClassifier {
    public BlockPassability classify(BlockState state) {
        Objects.requireNonNull(state, "state");
        if (state.isAir()) {
            return BlockPassability.PASSABLE;
        }
        if (hasFluid(state.getFluidState())) {
            return BlockPassability.PASSABLE;
        }
        if (state.canOcclude()) {
            return BlockPassability.SOLID;
        }
        return BlockPassability.WALKABLE;
    }

    public BlockPassability classify(BlockState state, BlockGetter blockGetter, BlockPos position) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(blockGetter, "blockGetter");
        Objects.requireNonNull(position, "position");
        if (state.isAir()) {
            return BlockPassability.PASSABLE;
        }
        if (hasFluid(state.getFluidState())) {
            return BlockPassability.PASSABLE;
        }
        if (!state.getCollisionShape(blockGetter, position).isEmpty()) {
            return BlockPassability.SOLID;
        }
        return BlockPassability.WALKABLE;
    }

    public BlockPassability classify(FluidState state) {
        Objects.requireNonNull(state, "state");
        return BlockPassability.PASSABLE;
    }

    public boolean hasFluid(FluidState state) {
        Objects.requireNonNull(state, "state");
        return !state.isEmpty();
    }

    public boolean allowsFluid(FluidState state, FluidHandling handling) {
        Objects.requireNonNull(handling, "handling");
        return switch (handling) {
            case ALLOW -> true;
            case AVOID -> !hasFluid(state);
            case REQUIRE -> hasFluid(state);
        };
    }
}

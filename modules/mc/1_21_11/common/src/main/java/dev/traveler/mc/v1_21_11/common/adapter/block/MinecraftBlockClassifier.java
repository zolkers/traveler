package dev.traveler.mc.v1_21_11.common.adapter.block;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.BlockClassifier;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.movement.FluidHandling;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class MinecraftBlockClassifier implements BlockClassifier<MinecraftBlockContext> {
    @Override
    public BlockClassification classifyContext(MinecraftBlockContext context) {
        Objects.requireNonNull(context, "context");
        return new BlockClassification(
                classify(context.state(), context.blockGetter(), context.position()),
                fluidHandlingOf(context.state().getFluidState()));
    }

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
        if (!state.getCollisionShape(blockGetter, position).isEmpty()) {
            return BlockPassability.SOLID;
        }
        if (hasFluid(state.getFluidState())) {
            return BlockPassability.PASSABLE;
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

    public FluidHandling fluidHandlingOf(FluidState state) {
        return hasFluid(state) ? FluidHandling.ALLOW : FluidHandling.AVOID;
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

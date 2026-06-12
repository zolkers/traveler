package dev.traveler.mc.v1_21_11.common.adapter;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.BlockPosition;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class MinecraftWorldSnapshot implements WorldLayer {
    private final BlockGetter blockGetter;
    private final MinecraftBlockClassifier classifier;

    public MinecraftWorldSnapshot(BlockGetter blockGetter) {
        this(blockGetter, new MinecraftBlockClassifier());
    }

    public MinecraftWorldSnapshot(BlockGetter blockGetter, MinecraftBlockClassifier classifier) {
        this.blockGetter = Objects.requireNonNull(blockGetter, "blockGetter");
        this.classifier = Objects.requireNonNull(classifier, "classifier");
    }

    public BlockGetter blockGetter() {
        return blockGetter;
    }

    public BlockState blockState(BlockPosition position) {
        return blockState(toMinecraft(position));
    }

    public BlockState blockState(BlockPos position) {
        return blockGetter.getBlockState(position);
    }

    public FluidState fluidState(BlockPosition position) {
        return fluidState(toMinecraft(position));
    }

    public FluidState fluidState(BlockPos position) {
        return blockGetter.getFluidState(position);
    }

    @Override
    public BlockClassification classify(BlockPosition position) {
        BlockPos blockPosition = toMinecraft(position);
        return classifier.classify(new MinecraftBlockContext(blockState(blockPosition), blockGetter, blockPosition));
    }

    public static BlockPos toMinecraft(BlockPosition position) {
        Objects.requireNonNull(position, "position");
        return new BlockPos(position.x(), position.y(), position.z());
    }

    public static BlockPosition toCore(BlockPos position) {
        Objects.requireNonNull(position, "position");
        return new BlockPosition(position.getX(), position.getY(), position.getZ());
    }
}

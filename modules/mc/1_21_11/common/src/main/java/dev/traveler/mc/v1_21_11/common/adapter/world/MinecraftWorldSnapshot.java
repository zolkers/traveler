package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockClassifier;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class MinecraftWorldSnapshot implements SurfaceWorldLayer {
    private final BlockGetter blockGetter;
    private final MinecraftSurfaceBlockAdapter surfaceBlockAdapter;

    public MinecraftWorldSnapshot(BlockGetter blockGetter) {
        this(blockGetter, new MinecraftBlockClassifier());
    }

    public MinecraftWorldSnapshot(BlockGetter blockGetter, MinecraftBlockClassifier classifier) {
        this.blockGetter = Objects.requireNonNull(blockGetter, "blockGetter");
        surfaceBlockAdapter = new MinecraftSurfaceBlockAdapter(
                Objects.requireNonNull(classifier, "classifier"), BlockBehaviorRegistry.defaults());
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
        return surfaceBlock(position).classification();
    }

    @Override
    public SurfaceBlock surfaceBlock(BlockPosition position) {
        BlockPos blockPosition = toMinecraft(position);
        MinecraftBlockContext context =
                new MinecraftBlockContext(blockState(blockPosition), blockGetter, blockPosition);
        return surfaceBlockAdapter.surfaceBlock(context);
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

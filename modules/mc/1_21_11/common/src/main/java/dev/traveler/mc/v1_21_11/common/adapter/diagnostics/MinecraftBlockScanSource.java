package dev.traveler.mc.v1_21_11.common.adapter.diagnostics;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.navigation.diagnostics.BlockScanSample;
import dev.traveler.core.navigation.diagnostics.BlockScanSource;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import dev.traveler.mc.v1_21_11.common.adapter.world.MinecraftSurfaceBlockAdapter;
import dev.traveler.mc.v1_21_11.common.adapter.world.MinecraftWorldSnapshot;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class MinecraftBlockScanSource implements BlockScanSource {
    private final BlockGetter blockGetter;
    private final MinecraftSurfaceBlockAdapter surfaceBlockAdapter = new MinecraftSurfaceBlockAdapter();

    public MinecraftBlockScanSource(BlockGetter blockGetter) {
        this.blockGetter = Objects.requireNonNull(blockGetter, "blockGetter");
    }

    @Override
    public BlockScanSample sample(BlockPosition position) {
        BlockPosition corePosition = Objects.requireNonNull(position, "position");
        BlockPos minecraftPosition = MinecraftWorldSnapshot.toMinecraft(corePosition);
        BlockState state = blockGetter.getBlockState(minecraftPosition);
        SurfaceBlock surfaceBlock = surfaceBlockAdapter.surfaceBlock(
                new MinecraftBlockContext(state, blockGetter, minecraftPosition));
        return new BlockScanSample(
                corePosition,
                state.toString(),
                surfaceBlock.classification().passability().name(),
                surfaceBlock.classification().fluidHandling().name(),
                surfaceBlock.behavior().key().name());
    }
}

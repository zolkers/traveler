package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.layer.BlockBehaviorSpec;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.Optional;
import net.minecraft.world.level.block.SlabBlock;

final class SlabBlockSpecResolver implements MinecraftBlockBehaviorSpecResolver {
    @Override
    public Optional<BlockBehaviorSpec> resolve(MinecraftBlockContext context, BlockShape shape) {
        if (!(context.state().getBlock() instanceof SlabBlock)) {
            return Optional.empty();
        }
        return Optional.of(BlockBehaviorSpec.slab());
    }
}

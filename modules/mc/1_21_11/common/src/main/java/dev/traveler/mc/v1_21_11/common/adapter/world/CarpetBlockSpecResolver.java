package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.layer.BlockBehaviorSpec;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.Optional;
import net.minecraft.world.level.block.CarpetBlock;

final class CarpetBlockSpecResolver implements MinecraftBlockBehaviorSpecResolver {
    @Override
    public Optional<BlockBehaviorSpec> resolve(MinecraftBlockContext context, BlockShape shape) {
        if (!(context.state().getBlock() instanceof CarpetBlock)) {
            return Optional.empty();
        }
        return Optional.of(BlockBehaviorSpec.carpet());
    }
}

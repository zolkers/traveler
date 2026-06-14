package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LadderBlock;

final class LadderBlockBehaviorResolver implements MinecraftBlockBehaviorResolver {
    @Override
    public Optional<BlockBehavior> resolve(
            MinecraftBlockContext context,
            BlockShape shape,
            BlockBehaviorRegistry behaviorRegistry) {
        if (!(context.state().getBlock() instanceof LadderBlock)) {
            return Optional.empty();
        }
        Direction direction = context.state().getValue(LadderBlock.FACING);
        return Optional.of(behaviorRegistry.ladder(MinecraftHorizontalFacing.from(direction)));
    }
}

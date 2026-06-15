package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.core.layer.BlockBehaviorSpec;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.mc.v1_21_11.common.adapter.block.MinecraftBlockContext;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.level.block.VineBlock;

final class VineBlockSpecResolver implements MinecraftBlockBehaviorSpecResolver {
    @Override
    public Optional<BlockBehaviorSpec> resolve(MinecraftBlockContext context, BlockShape shape) {
        if (!(context.state().getBlock() instanceof VineBlock)) {
            return Optional.empty();
        }
        return Optional.of(BlockBehaviorSpec.vine(attachedFaces(context), context.state().getValue(VineBlock.UP)));
    }

    private static Set<HorizontalFacing> attachedFaces(MinecraftBlockContext context) {
        EnumSet<HorizontalFacing> faces = EnumSet.noneOf(HorizontalFacing.class);
        addFace(faces, HorizontalFacing.NORTH, context.state().getValue(VineBlock.NORTH));
        addFace(faces, HorizontalFacing.SOUTH, context.state().getValue(VineBlock.SOUTH));
        addFace(faces, HorizontalFacing.WEST, context.state().getValue(VineBlock.WEST));
        addFace(faces, HorizontalFacing.EAST, context.state().getValue(VineBlock.EAST));
        return Set.copyOf(faces);
    }

    private static void addFace(EnumSet<HorizontalFacing> faces, HorizontalFacing face, boolean attached) {
        if (attached) {
            faces.add(face);
        }
    }
}

package dev.traveler.mc.v1_21_11.common.adapter.world;

import dev.traveler.mc.v1_21_11.common.adapter.testing.MinecraftTestBootstrap;
import dev.traveler.mc.v1_21_11.common.adapter.testing.SingleStateBlockGetter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.StairBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MinecraftSurfaceBlockAdapterTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void snapshotExposesSurfaceWorldLayerContract() {
        MinecraftWorldSnapshot snapshot =
                new MinecraftWorldSnapshot(new SingleStateBlockGetter(Blocks.STONE.defaultBlockState()));

        assertInstanceOf(SurfaceWorldLayer.class, snapshot);
        SurfaceBlock surfaceBlock = snapshot.surfaceBlock(new BlockPosition(0, 0, 0));
        assertEquals(BlockPassability.SOLID, surfaceBlock.classification().passability());
    }

    @Test
    void bottomSlabsExposeHalfHeightSupportSurface() {
        SurfaceBlock block = surfaceBlock(
                Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));

        assertEquals(0.5, block.shape().floorHeightForCell(0, 0).orElseThrow());
        assertEquals(BlockPassability.WALKABLE, block.classification().passability());
    }

    @Test
    void topAndDoubleSlabsExposeTopSupportSurface() {
        SurfaceBlock top = surfaceBlock(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        SurfaceBlock doubleSlab = surfaceBlock(
                Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));

        assertEquals(1.0, top.shape().floorHeightForCell(1, 1).orElseThrow());
        assertEquals(1.0, doubleSlab.shape().floorHeightForCell(1, 1).orElseThrow());
    }

    @Test
    void straightBottomStairsExposeLowAndHighSubcellSurfaces() {
        SurfaceBlock stair = surfaceBlock(Blocks.OAK_STAIRS
                .defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH)
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT));

        Set<Double> floors = Stream.of(
                        stair.shape().floorHeightForCell(0, 0).orElseThrow(),
                        stair.shape().floorHeightForCell(1, 0).orElseThrow(),
                        stair.shape().floorHeightForCell(0, 1).orElseThrow(),
                        stair.shape().floorHeightForCell(1, 1).orElseThrow())
                .collect(Collectors.toSet());

        assertEquals(Set.of(0.5, 1.0), floors);
    }

    @Test
    void stairBlockStateKeepsFacingInsideCoreBehavior() {
        SurfaceBlock stair = surfaceBlock(Blocks.OAK_STAIRS
                .defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT));

        StairBlockBehavior behavior = assertInstanceOf(StairBlockBehavior.class, stair.behavior());

        assertEquals(HorizontalFacing.WEST, behavior.facing());
    }

    @ParameterizedTest
    @MethodSource("surfaceClassifications")
    void blockStatesExposeConsistentSurfaceClassification(
            BlockState state, BlockBehaviorKey behaviorKey, BlockPassability passability) {
        SurfaceBlock block = surfaceBlock(state);

        assertEquals(behaviorKey, block.behavior().key());
        assertEquals(passability, block.classification().passability());
    }

    @Test
    void pureFluidBlocksExposeSwimmingBehaviorAndAirDoesNotSupportWalking() {
        SurfaceBlock water = surfaceBlock(Blocks.WATER.defaultBlockState());
        SurfaceBlock air = surfaceBlock(Blocks.AIR.defaultBlockState());

        assertTrue(water.shape().isEmpty());
        assertEquals(BlockBehaviorKey.FLUID, water.behavior().key());
        assertEquals(BlockBehaviorKey.AIR, air.behavior().key());
    }

    private static SurfaceBlock surfaceBlock(BlockState state) {
        MinecraftWorldSnapshot snapshot = new MinecraftWorldSnapshot(new SingleStateBlockGetter(state));
        return snapshot.surfaceBlock(new BlockPosition(0, 0, 0));
    }

    private static Stream<Arguments> surfaceClassifications() {
        return Stream.of(
                Arguments.of(Blocks.STONE.defaultBlockState(), BlockBehaviorKey.FULL_BLOCK, BlockPassability.SOLID),
                Arguments.of(slab(SlabType.BOTTOM), BlockBehaviorKey.SLAB, BlockPassability.WALKABLE),
                Arguments.of(slab(SlabType.TOP), BlockBehaviorKey.SLAB, BlockPassability.WALKABLE),
                Arguments.of(slab(SlabType.DOUBLE), BlockBehaviorKey.SLAB, BlockPassability.WALKABLE),
                Arguments.of(stair(), BlockBehaviorKey.STAIR, BlockPassability.WALKABLE),
                Arguments.of(waterloggedSlab(), BlockBehaviorKey.WATERLOGGED, BlockPassability.WALKABLE),
                Arguments.of(waterloggedStair(), BlockBehaviorKey.WATERLOGGED, BlockPassability.WALKABLE),
                Arguments.of(Blocks.WATER.defaultBlockState(), BlockBehaviorKey.FLUID, BlockPassability.PASSABLE),
                Arguments.of(Blocks.AIR.defaultBlockState(), BlockBehaviorKey.AIR, BlockPassability.PASSABLE));
    }

    private static BlockState slab(SlabType type) {
        return Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    private static BlockState waterloggedSlab() {
        return slab(SlabType.BOTTOM).setValue(BlockStateProperties.WATERLOGGED, true);
    }

    private static BlockState stair() {
        return Blocks.OAK_STAIRS
                .defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH)
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
    }

    private static BlockState waterloggedStair() {
        return stair().setValue(BlockStateProperties.WATERLOGGED, true);
    }
}

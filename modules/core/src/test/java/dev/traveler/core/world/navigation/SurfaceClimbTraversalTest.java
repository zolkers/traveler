package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.LadderBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurfaceClimbTraversalTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    @Test
    void acceptsLadderClimbFromFacingSide() {
        Map<BlockPosition, SurfaceBlock> blocks = Map.of(
                new BlockPosition(1, 64, 0), ladder(HorizontalFacing.EAST),
                new BlockPosition(1, 65, 0), ladder(HorizontalFacing.EAST));

        boolean climb = SurfaceClimbTraversal.canClimbWithLookup(
                position -> blocks.getOrDefault(position, SurfaceBlock.empty()),
                eastOfLadder(63, 64.0),
                eastOfLadder(65, 66.0),
                PLAYER);

        assertTrue(climb);
    }

    @Test
    void rejectsLadderClimbFromOppositeFacingSide() {
        Map<BlockPosition, SurfaceBlock> blocks = Map.of(
                new BlockPosition(1, 64, 0), ladder(HorizontalFacing.EAST),
                new BlockPosition(1, 65, 0), ladder(HorizontalFacing.EAST));

        boolean climb = SurfaceClimbTraversal.canClimbWithLookup(
                position -> blocks.getOrDefault(position, SurfaceBlock.empty()),
                westOfLadder(63, 64.0),
                westOfLadder(65, 66.0),
                PLAYER);

        assertFalse(climb);
    }

    @Test
    void rejectsLadderClimbFromSideFaceThatIsNotFacing() {
        Map<BlockPosition, SurfaceBlock> blocks = Map.of(
                new BlockPosition(1, 64, 0), ladder(HorizontalFacing.EAST),
                new BlockPosition(1, 65, 0), ladder(HorizontalFacing.EAST));

        boolean climb = SurfaceClimbTraversal.canClimbWithLookup(
                position -> blocks.getOrDefault(position, SurfaceBlock.empty()),
                northOfLadder(63, 64.0),
                northOfLadder(65, 66.0),
                PLAYER);

        assertFalse(climb);
    }

    private static SurfaceNode westOfLadder(int supportY, double floorY) {
        return new SurfaceNode(new BlockPosition(0, supportY, 0), 1, 1, floorY);
    }

    private static SurfaceNode eastOfLadder(int supportY, double floorY) {
        return new SurfaceNode(new BlockPosition(2, supportY, 0), 0, 1, floorY);
    }

    private static SurfaceNode northOfLadder(int supportY, double floorY) {
        return new SurfaceNode(new BlockPosition(1, supportY, -1), 0, 1, floorY);
    }

    private static SurfaceBlock ladder(HorizontalFacing facing) {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID),
                BlockShape.empty(),
                new LadderBlockBehavior(facing));
    }
}

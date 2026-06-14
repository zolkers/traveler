package dev.traveler.core.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import org.junit.jupiter.api.Test;

class RouteGoalTest {
    @Test
    void fallsBackFromRequestedAirBlockToStandingSurfaceBelowIt() {
        BlockPosition support = new BlockPosition(4, 63, 2);
        BlockPosition requestedFeet = support.above();
        RouteGoal goal = RouteGoal.blockTarget(requestedFeet);
        SurfaceWorldLayer world = position ->
                position.equals(support) ? SurfaceBlock.solid(BlockShape.fullCube()) : SurfaceBlock.empty();

        assertEquals(
                support,
                goal.surfaceGoals(new SurfaceNodeResolver(world)).getFirst().blockPosition());
    }

    @Test
    void resolvesSolidBlockGoalToFeetSpaceAboveForBlockSearch() {
        BlockPosition target = new BlockPosition(1, 64, 1);
        RouteGoal goal = RouteGoal.blockTarget(target);
        WorldLayer world = position -> new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID);

        assertEquals(target.above(), goal.blockGoal(world));
    }

    @Test
    void keepsNonSolidBlockGoalAtRequestedPositionForBlockSearch() {
        BlockPosition target = new BlockPosition(1, 64, 1);
        RouteGoal goal = RouteGoal.blockTarget(target);
        WorldLayer world = position -> new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID);

        assertEquals(target, goal.blockGoal(world));
    }
}

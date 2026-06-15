package dev.traveler.core.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.LadderBlockBehavior;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RouteGoalTest {
    private static final String STANDARD_GOAL_IMPLEMENTATION_PACKAGE = "dev.traveler.core.route.goal";

    @Test
    void standardFactoriesReturnDedicatedGoalImplementations() {
        assertStandardGoalImplementation(RouteGoal.blockTarget(new BlockPosition(1, 64, 1)));
        assertStandardGoalImplementation(RouteGoal.xz(8, -4));
        assertStandardGoalImplementation(RouteGoal.yLevel(90));
        assertStandardGoalImplementation(RouteGoal.unspecified());
    }

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
    void resolvesClimbableTargetToAdjacentLandingSurface() {
        BlockPosition ladder = new BlockPosition(4, 64, 2);
        BlockPosition landing = new BlockPosition(4, 63, 1);
        BlockPosition wrongSideLanding = new BlockPosition(4, 63, 3);
        RouteGoal goal = RouteGoal.blockTarget(ladder);
        SurfaceWorldLayer world = position -> Map.of(
                        landing,
                        SurfaceBlock.solid(BlockShape.fullCube()),
                        wrongSideLanding,
                        SurfaceBlock.solid(BlockShape.fullCube()),
                        ladder,
                        new SurfaceBlock(
                                new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID),
                                BlockShape.empty(),
                                new LadderBlockBehavior(HorizontalFacing.NORTH)))
                .getOrDefault(position, SurfaceBlock.empty());

        assertEquals(
                landing,
                goal.surfaceGoals(new SurfaceNodeResolver(world)).getFirst().blockPosition());
    }

    @Test
    void resolvesClimbableTargetWithoutLandingToClimbSurface() {
        BlockPosition ladder = new BlockPosition(4, 64, 2);
        RouteGoal goal = RouteGoal.blockTarget(ladder);
        SurfaceWorldLayer world = position -> Map.of(
                        ladder,
                        new SurfaceBlock(
                                new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID),
                                BlockShape.empty(),
                                new LadderBlockBehavior(HorizontalFacing.NORTH)))
                .getOrDefault(position, SurfaceBlock.empty());

        assertEquals(
                ladder,
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

    @Test
    void xzGoalKeepsCurrentHeightAndAcceptsAnyMatchingHeight() {
        RouteGoal goal = RouteGoal.xz(8, -4);
        BlockPosition start = new BlockPosition(2, 64, 3);

        assertEquals(new BlockPosition(8, 64, -4), goal.blockGoal(null, start));
        assertTrue(goal.isSatisfiedBy(new BlockPosition(8, -20, -4)));
    }

    @Test
    void yGoalKeepsCurrentColumnAndAcceptsAnyMatchingHeight() {
        RouteGoal goal = RouteGoal.yLevel(90);
        BlockPosition start = new BlockPosition(2, 64, 3);

        assertEquals(new BlockPosition(2, 90, 3), goal.blockGoal(null, start));
        assertTrue(goal.isSatisfiedBy(new BlockPosition(-30, 90, 18)));
    }

    private static void assertStandardGoalImplementation(RouteGoal goal) {
        assertEquals(STANDARD_GOAL_IMPLEMENTATION_PACKAGE, goal.getClass().getPackageName());
    }
}

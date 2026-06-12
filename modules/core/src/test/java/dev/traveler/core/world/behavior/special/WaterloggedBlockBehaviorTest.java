package dev.traveler.core.world.behavior.special;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.context.MovementDirection;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import org.junit.jupiter.api.Test;

class WaterloggedBlockBehaviorTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, true, false, false, 0.6, 1.25, 3.0);

    @Test
    void keepsWaterloggedIdentityWhileDelegatingSurfaceMovement() {
        SlabBlockBehavior slab = new SlabBlockBehavior();
        WaterloggedBlockBehavior behavior = new WaterloggedBlockBehavior(slab);

        MovementDecision decision = behavior.evaluateMovement(context(
                node(63.0), node(63.5), MovementDirection.east()));

        assertEquals(BlockBehaviorKey.WATERLOGGED, behavior.key());
        assertSame(slab, behavior.delegate());
        assertTrue(decision.allowed());
        assertEquals(MovementAction.STEP_UP, decision.action());
    }

    private static SurfaceMovementContext context(
            SurfaceNode from, SurfaceNode to, MovementDirection direction) {
        return new SurfaceMovementContext(from, to, PLAYER, direction);
    }

    private static SurfaceNode node(double floorY) {
        return new SurfaceNode(new BlockPosition(0, 63, 0), 0, 0, floorY);
    }
}

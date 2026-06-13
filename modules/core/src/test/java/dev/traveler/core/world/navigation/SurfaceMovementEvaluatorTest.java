package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import org.junit.jupiter.api.Test;

class SurfaceMovementEvaluatorTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    @Test
    void classifiesSafeDownwardMovementAsDrop() {
        SurfaceMovementEvaluator evaluator = new SurfaceMovementEvaluator(PLAYER);
        MovementDecision decision =
                evaluator.decision(node(0, 65.0), node(1, 64.0), SurfaceBlock.solid(BlockShape.fullCube()));

        assertEquals(MovementAction.DROP, decision.action());
    }

    @Test
    void keepsFlatMovementAsWalk() {
        SurfaceMovementEvaluator evaluator = new SurfaceMovementEvaluator(PLAYER);
        MovementDecision decision =
                evaluator.decision(node(0, 64.0), node(1, 64.0), SurfaceBlock.solid(BlockShape.fullCube()));

        assertEquals(MovementAction.WALK, decision.action());
    }

    private static SurfaceNode node(int x, double floorY) {
        return new SurfaceNode(new BlockPosition(x, (int) Math.floor(floorY) - 1, 0), 0, 0, floorY);
    }
}

package dev.traveler.core.world.behavior;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.ClimbableBlockBehavior;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.behavior.special.LadderBlockBehavior;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import org.junit.jupiter.api.Test;

class BlockBehaviorsTest {
    @Test
    void unwrapsWaterloggedBehaviorToItsDryDelegate() {
        FullBlockBehavior fullBlock = new FullBlockBehavior();
        WaterloggedBlockBehavior waterlogged = new WaterloggedBlockBehavior(fullBlock);

        assertSame(fullBlock, BlockBehaviors.dry(waterlogged));
    }

    @Test
    void resolvesClimbableBehaviorThroughWaterloggedWrapper() {
        LadderBlockBehavior ladder = new LadderBlockBehavior(HorizontalFacing.WEST);
        WaterloggedBlockBehavior waterlogged = new WaterloggedBlockBehavior(ladder);

        ClimbableBlockBehavior climbable =
                assertInstanceOf(ClimbableBlockBehavior.class, BlockBehaviors.climbable(waterlogged).orElseThrow());

        assertSame(ladder, climbable);
    }

    @Test
    void returnsEmptyWhenDryBehaviorIsNotClimbable() {
        assertTrue(BlockBehaviors.climbable(new FullBlockBehavior()).isEmpty());
    }
}

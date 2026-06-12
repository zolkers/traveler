package dev.traveler.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MovementValueObjectsTest {
    @Test
    void rejectsInvalidEntityDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new EntityDimensions(0.0, 1.8));
        assertThrows(IllegalArgumentException.class, () -> new EntityDimensions(0.6, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new EntityDimensions(Double.NaN, 1.8));
        assertThrows(IllegalArgumentException.class, () -> new EntityDimensions(0.6, Double.POSITIVE_INFINITY));
    }

    @Test
    void rejectsInvalidTraversalCosts() {
        assertThrows(IllegalArgumentException.class, () -> new TraversalCost(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new TraversalCost(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new TraversalCost(Double.POSITIVE_INFINITY));
    }

    @Test
    void rejectsInvalidMovementCapabilities() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(false, false, false, false, 0.6, 1.25, 3.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(true, false, false, false, -0.1, 1.25, 3.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(true, false, false, false, 0.6, -0.1, 3.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(true, false, false, false, 0.6, 1.25, -0.1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(true, false, false, false, Double.NaN, 1.25, 3.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(true, false, false, false, 0.6, Double.POSITIVE_INFINITY, 3.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(true, false, false, false, 0.6, 1.25, Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(true, false, false, false, 0.6, 1.25, Double.POSITIVE_INFINITY));
    }

    @Test
    void movementCapabilitiesExposeStepJumpFallAndMovementModes() {
        MovementCapabilities capabilities = new MovementCapabilities(true, true, false, true, 0.6, 1.25, 3.0);

        assertEquals(0.6, capabilities.maxStepUp());
        assertEquals(1.25, capabilities.maxJumpHeight());
        assertEquals(3.0, capabilities.maxSafeFallDistance());
        assertEquals(true, capabilities.canWalk());
        assertEquals(true, capabilities.canSwim());
        assertEquals(false, capabilities.canFly());
        assertEquals(true, capabilities.canCrouch());
    }

    @Test
    void traversalRulesExposeFluidHandlingAndAllowedPassability() {
        Set<BlockPassability> passability = new HashSet<>();
        passability.add(BlockPassability.WALKABLE);
        passability.add(BlockPassability.PASSABLE);

        TraversalRules rules =
                new TraversalRules(false, true, FluidHandling.AVOID, new TraversalCost(1.0), passability);

        passability.clear();

        assertEquals(false, rules.allowDiagonal());
        assertEquals(true, rules.allowVertical());
        assertEquals(FluidHandling.AVOID, rules.fluidHandling());
        assertEquals(new TraversalCost(1.0), rules.defaultCost());
        assertEquals(Set.of(BlockPassability.WALKABLE, BlockPassability.PASSABLE), rules.allowedPassability());
    }

    @Test
    void movementProfileKeepsValidatedComponentsTogether() {
        EntityDimensions dimensions = new EntityDimensions(0.6, 1.8);
        MovementCapabilities capabilities = new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);
        TraversalRules rules = new TraversalRules(
                false,
                true,
                FluidHandling.ALLOW,
                new TraversalCost(1.0),
                Set.of(BlockPassability.WALKABLE, BlockPassability.PASSABLE));

        MovementProfile profile = new MovementProfile(dimensions, capabilities, rules);

        assertSame(dimensions, profile.dimensions());
        assertSame(capabilities, profile.capabilities());
        assertSame(rules, profile.rules());
    }

    @Test
    void movementProfileAndTraversalRulesRejectMissingComponents() {
        EntityDimensions dimensions = new EntityDimensions(0.6, 1.8);
        MovementCapabilities capabilities = new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);
        TraversalRules rules = new TraversalRules(
                false,
                true,
                FluidHandling.ALLOW,
                new TraversalCost(1.0),
                Set.of(BlockPassability.WALKABLE));
        Set<BlockPassability> passabilityWithNull = new HashSet<>();
        passabilityWithNull.add(null);

        assertThrows(
                NullPointerException.class,
                () -> new TraversalRules(
                        false, true, null, new TraversalCost(1.0), Set.of(BlockPassability.WALKABLE)));
        assertThrows(
                NullPointerException.class,
                () -> new TraversalRules(
                        false, true, FluidHandling.ALLOW, null, Set.of(BlockPassability.WALKABLE)));
        assertThrows(
                NullPointerException.class,
                () -> new TraversalRules(false, true, FluidHandling.ALLOW, new TraversalCost(1.0), null));
        assertThrows(
                NullPointerException.class,
                () -> new TraversalRules(
                        false, true, FluidHandling.ALLOW, new TraversalCost(1.0), passabilityWithNull));
        assertThrows(NullPointerException.class, () -> new MovementProfile(null, capabilities, rules));
        assertThrows(NullPointerException.class, () -> new MovementProfile(dimensions, null, rules));
        assertThrows(NullPointerException.class, () -> new MovementProfile(dimensions, capabilities, null));
        assertEquals(new TraversalCost(1.0), rules.defaultCost());
    }

    @Test
    void traversalRulesRejectEmptyPassability() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TraversalRules(false, true, FluidHandling.ALLOW, new TraversalCost(1.0), Set.of()));
    }
}

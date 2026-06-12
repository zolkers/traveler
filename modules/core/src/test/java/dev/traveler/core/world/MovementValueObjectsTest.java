package dev.traveler.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertThrows(IllegalArgumentException.class, () -> new MovementCapabilities(false, false, false, 0.6, 1.25));
        assertThrows(IllegalArgumentException.class, () -> new MovementCapabilities(true, false, false, -0.1, 1.25));
        assertThrows(IllegalArgumentException.class, () -> new MovementCapabilities(true, false, false, 0.6, -0.1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(true, false, false, Double.NaN, 1.25));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementCapabilities(true, false, false, 0.6, Double.POSITIVE_INFINITY));
    }

    @Test
    void movementProfileKeepsValidatedComponentsTogether() {
        EntityDimensions dimensions = new EntityDimensions(0.6, 1.8);
        MovementCapabilities capabilities = new MovementCapabilities(true, false, false, 0.6, 1.25);
        TraversalRules rules = new TraversalRules(false, true, new TraversalCost(1.0));

        MovementProfile profile = new MovementProfile(dimensions, capabilities, rules);

        assertSame(dimensions, profile.dimensions());
        assertSame(capabilities, profile.capabilities());
        assertSame(rules, profile.rules());
    }

    @Test
    void movementProfileAndTraversalRulesRejectMissingComponents() {
        EntityDimensions dimensions = new EntityDimensions(0.6, 1.8);
        MovementCapabilities capabilities = new MovementCapabilities(true, false, false, 0.6, 1.25);
        TraversalRules rules = new TraversalRules(false, true, new TraversalCost(1.0));

        assertThrows(NullPointerException.class, () -> new TraversalRules(false, true, null));
        assertThrows(NullPointerException.class, () -> new MovementProfile(null, capabilities, rules));
        assertThrows(NullPointerException.class, () -> new MovementProfile(dimensions, null, rules));
        assertThrows(NullPointerException.class, () -> new MovementProfile(dimensions, capabilities, null));
        assertEquals(new TraversalCost(1.0), rules.defaultCost());
    }
}

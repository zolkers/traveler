package dev.traveler.core.world.movement;

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
    void movementCapabilitiesCanReplaceSwimmingMode() {
        MovementCapabilities capabilities = new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

        MovementCapabilities swimming = capabilities.withCanSwim(true);

        assertEquals(true, swimming.canSwim());
        assertEquals(capabilities.maxSafeFallDistance(), swimming.maxSafeFallDistance());
    }

    @Test
    void traversalRulesExposeRouteExpansionOptionsAndCost() {
        TraversalRules rules = new TraversalRules(false, true, new TraversalCost(1.0));

        assertEquals(false, rules.allowDiagonal());
        assertEquals(true, rules.allowVertical());
        assertEquals(new TraversalCost(1.0), rules.defaultCost());
    }

    @Test
    void movementProfileKeepsValidatedComponentsTogether() {
        EntityDimensions dimensions = MovementProfiles.defaultPlayerDimensions();
        MovementCapabilities capabilities = MovementProfiles.defaultPlayerCapabilities();
        TraversalRules rules = MovementProfiles.defaultPlayerRules();

        MovementProfile profile = new MovementProfile(dimensions, capabilities, rules);

        assertSame(dimensions, profile.dimensions());
        assertSame(capabilities, profile.capabilities());
        assertSame(rules, profile.rules());
    }

    @Test
    void movementProfileCanReplaceIndividualSettingGroups() {
        MovementProfile profile = MovementProfiles.defaultPlayer();
        EntityDimensions dimensions = new EntityDimensions(0.9, 2.0);
        MovementCapabilities capabilities = profile.capabilities().withMaxSafeFallDistance(10.0);
        TraversalRules rules = new TraversalRules(false, true, new TraversalCost(2.0));

        assertSame(dimensions, profile.withDimensions(dimensions).dimensions());
        assertSame(capabilities, profile.withCapabilities(capabilities).capabilities());
        assertSame(rules, profile.withRules(rules).rules());
    }

    @Test
    void defaultPlayerWithKeepsCustomCapabilitiesOnly() {
        MovementCapabilities capabilities = new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

        MovementProfile profile = MovementProfiles.defaultPlayerWith(capabilities);

        assertEquals(MovementProfiles.defaultPlayerDimensions(), profile.dimensions());
        assertSame(capabilities, profile.capabilities());
        assertEquals(MovementProfiles.defaultPlayerRules(), profile.rules());
    }

    @Test
    void movementProfileAndTraversalRulesRejectMissingComponents() {
        EntityDimensions dimensions = MovementProfiles.defaultPlayerDimensions();
        MovementCapabilities capabilities = MovementProfiles.defaultPlayerCapabilities();
        TraversalRules rules = MovementProfiles.defaultPlayerRules();

        assertThrows(NullPointerException.class, () -> new TraversalRules(false, true, null));
        assertThrows(NullPointerException.class, () -> new MovementProfile(null, capabilities, rules));
        assertThrows(NullPointerException.class, () -> new MovementProfile(dimensions, null, rules));
        assertThrows(NullPointerException.class, () -> new MovementProfile(dimensions, capabilities, null));
        assertThrows(NullPointerException.class, () -> MovementProfiles.defaultPlayerWith(null));
        assertEquals(new TraversalCost(1.0), rules.defaultCost());
    }
}

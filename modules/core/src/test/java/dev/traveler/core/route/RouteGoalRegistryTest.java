package dev.traveler.core.route;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.route.goal.RouteGoalRegistry;
import org.junit.jupiter.api.Test;

class RouteGoalRegistryTest {
    @Test
    void standardRegistryExposesBuiltInGoalTypes() {
        RouteGoalRegistry registry = RouteGoalRegistry.standard();

        assertTrue(registry.find("xyz").isPresent());
        assertTrue(registry.find("xz").isPresent());
        assertTrue(registry.find("y").isPresent());
    }
}

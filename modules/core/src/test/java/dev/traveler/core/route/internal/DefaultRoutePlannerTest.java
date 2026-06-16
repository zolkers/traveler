package dev.traveler.core.route.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.traveler.core.route.RouteSearchSettings;
import org.junit.jupiter.api.Test;

class DefaultRoutePlannerTest {
    @Test
    void defaultPlannerLivesBehindRouteSearchFacade() {
        assertNotNull(new DefaultRoutePlanner(RouteSearchSettings.standardClient()));
    }
}

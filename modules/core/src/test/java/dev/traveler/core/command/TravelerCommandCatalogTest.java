package dev.traveler.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TravelerCommandCatalogTest {
    @Test
    void composesRoutesFromFeaturesAndFindsThemByPath() {
        TravelerCommandRoute route = new TravelerCommandRoute(
                "traveler test",
                "Runs a test command",
                context -> TravelerCommandResult.success("ok"));

        TravelerCommandCatalog catalog = TravelerCommandCatalog.fromFeatures(registry -> registry.add(route));

        assertEquals(List.of(route), catalog.routes());
        assertEquals(route, catalog.route("traveler test").orElseThrow());
    }

    @Test
    void routeListIsImmutable() {
        TravelerCommandRoute route = new TravelerCommandRoute(
                "traveler test",
                "Runs a test command",
                context -> TravelerCommandResult.success("ok"));
        TravelerCommandCatalog catalog = TravelerCommandCatalog.fromFeatures(registry -> registry.add(route));

        assertThrows(UnsupportedOperationException.class, () -> catalog.routes().add(route));
    }

    @Test
    void missingRouteReturnsEmptyOptional() {
        TravelerCommandCatalog catalog = TravelerCommandCatalog.fromFeatures();

        assertTrue(catalog.route("traveler missing").isEmpty());
    }
}

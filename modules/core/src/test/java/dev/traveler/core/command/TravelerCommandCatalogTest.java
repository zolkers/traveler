package dev.traveler.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TravelerCommandCatalogTest {
    @Test
    void composesRoutesFromFeaturesAndFindsThemByPath() {
        TravelerCommandRoute route = testRoute();

        TravelerCommandCatalog catalog = TravelerCommandCatalog.fromFeatures(registry -> registry.add(route));

        assertEquals(List.of(route), catalog.routes());
        assertEquals(route, catalog.route("traveler test").orElseThrow());
    }

    @Test
    void routeListIsImmutable() {
        TravelerCommandRoute route = testRoute();
        TravelerCommandCatalog catalog = TravelerCommandCatalog.fromFeatures(registry -> registry.add(route));

        assertThrows(UnsupportedOperationException.class, () -> catalog.routes().add(route));
    }

    @Test
    void missingRouteReturnsEmptyOptional() {
        TravelerCommandCatalog catalog = TravelerCommandCatalog.fromFeatures();

        assertTrue(catalog.route("traveler missing").isEmpty());
    }

    @Test
    void buildsRoutesFromAnnotatedCommandObject() {
        AnnotatedTravelerCommandFeature feature = AnnotatedTravelerCommandFeature.from(new TestAnnotatedCommands());

        TravelerCommandCatalog catalog = TravelerCommandCatalog.fromFeatures(feature);

        TravelerCommandRoute route = catalog.route("traveler test ping").orElseThrow();
        assertEquals("Replies with pong", route.description());
        assertEquals(
                TravelerCommandResult.success("pong"),
                route.handler().execute(new TravelerCommandContext(
                        java.util.Map.of(),
                        message -> {
                        })));
    }

    @Test
    void rejectsAnnotatedMethodsWithUnsupportedSignature() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> AnnotatedTravelerCommandFeature.from(new InvalidAnnotatedCommands()));

        assertTrue(error.getMessage().contains("TravelerCommandContext"));
    }

    @Test
    void rejectsBlankAnnotatedRootRouteParts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AnnotatedTravelerCommandFeature.from(new BlankRootCommands()));
        assertThrows(
                IllegalArgumentException.class,
                () -> AnnotatedTravelerCommandFeature.from(new BlankSubrouteCommands()));
    }

    private static TravelerCommandRoute testRoute() {
        return new TravelerCommandRoute(
                "traveler test",
                "Runs a test command",
                context -> TravelerCommandResult.success("ok"));
    }

    @TravelerCommand(root = "traveler test")
    private static final class TestAnnotatedCommands {
        @TravelerSubcommand(route = "ping", description = "Replies with pong")
        private TravelerCommandResult ping(TravelerCommandContext context) {
            return TravelerCommandResult.success("pong");
        }
    }

    @TravelerCommand(root = "traveler broken")
    private static final class InvalidAnnotatedCommands {
        @TravelerSubcommand(route = "missing-context", description = "Invalid command")
        private TravelerCommandResult missingContext() {
            return TravelerCommandResult.success("broken");
        }
    }

    @TravelerCommand(root = " ")
    private static final class BlankRootCommands {
        @TravelerSubcommand(route = "ping", description = "Invalid root")
        private TravelerCommandResult ping(TravelerCommandContext context) {
            return TravelerCommandResult.success("broken");
        }
    }

    @TravelerCommand(root = "traveler broken")
    private static final class BlankSubrouteCommands {
        @TravelerSubcommand(route = " ", description = "Invalid subroute")
        private TravelerCommandResult ping(TravelerCommandContext context) {
            return TravelerCommandResult.success("broken");
        }
    }
}

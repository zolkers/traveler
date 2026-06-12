package dev.traveler.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TravelerCommandCatalog {
    private final List<TravelerCommandRoute> routes;

    private TravelerCommandCatalog(List<TravelerCommandRoute> routes) {
        this.routes = List.copyOf(routes);
    }

    public static TravelerCommandCatalog fromFeatures(TravelerCommandFeature... features) {
        Builder builder = new Builder();
        for (TravelerCommandFeature feature : features) {
            Objects.requireNonNull(feature, "feature").register(builder);
        }
        return builder.build();
    }

    public List<TravelerCommandRoute> routes() {
        return routes;
    }

    public Optional<TravelerCommandRoute> route(String path) {
        String normalizedPath = Objects.requireNonNull(path, "path").trim();
        return routes.stream()
                .filter(route -> route.path().equals(normalizedPath))
                .findFirst();
    }

    public static final class Builder {
        private final List<TravelerCommandRoute> routes = new ArrayList<>();

        public void add(TravelerCommandRoute route) {
            TravelerCommandRoute commandRoute = Objects.requireNonNull(route, "route");
            if (containsPath(commandRoute.path())) {
                throw new IllegalArgumentException("Duplicate command route: " + commandRoute.path());
            }
            routes.add(commandRoute);
        }

        private boolean containsPath(String path) {
            return routes.stream().anyMatch(route -> route.path().equals(path));
        }

        private TravelerCommandCatalog build() {
            return new TravelerCommandCatalog(routes);
        }
    }
}

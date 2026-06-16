package dev.traveler.core.navigation.debug;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record DebugFrame(Set<DebugLayer> layers) {
    public DebugFrame {
        layers = Set.copyOf(Objects.requireNonNull(layers, "layers"));
    }

    public static DebugFrame of(Collection<DebugLayer> layers) {
        return new DebugFrame(new LinkedHashSet<>(Objects.requireNonNull(layers, "layers")));
    }
}

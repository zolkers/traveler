package dev.traveler.core.route.start;

import dev.traveler.core.route.internal.SurfaceTraversalFeatures;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record SurfaceRouteStartResolver(List<SurfaceRouteStartProvider> providers) {
    public SurfaceRouteStartResolver {
        providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    public static SurfaceRouteStartResolver standard() {
        return SurfaceTraversalFeatures.startResolver(SurfaceTraversalFeatures.standard());
    }

    public List<SurfaceNode> startNodes(SurfaceRouteStartContext context) {
        SurfaceRouteStartContext safeContext = Objects.requireNonNull(context, "context");
        LinkedHashSet<SurfaceNode> starts = new LinkedHashSet<>();
        for (SurfaceRouteStartProvider provider : providers) {
            starts.addAll(provider.startNodes(safeContext));
        }
        return List.copyOf(new ArrayList<>(starts));
    }
}

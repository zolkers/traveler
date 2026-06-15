package dev.traveler.core.world.navigation.features.drop;

import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import dev.traveler.core.world.navigation.SurfaceTraversalFeature;
import java.util.List;

public final class DropSurfaceTraversalFeature implements SurfaceTraversalFeature {
    @Override
    public List<SurfaceConnectionProvider> connectionProviders() {
        return List.of(SurfaceConnectionProviders.drop());
    }
}

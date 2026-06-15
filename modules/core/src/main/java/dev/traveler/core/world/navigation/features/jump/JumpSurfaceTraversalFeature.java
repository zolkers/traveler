package dev.traveler.core.world.navigation.features.jump;

import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import dev.traveler.core.world.navigation.SurfaceTraversalFeature;
import java.util.List;

public final class JumpSurfaceTraversalFeature implements SurfaceTraversalFeature {
    @Override
    public List<SurfaceConnectionProvider> connectionProviders() {
        return List.of(SurfaceConnectionProviders.jump());
    }
}
